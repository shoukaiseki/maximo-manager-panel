package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/**
 * MAXSEQUENCE 序列查询与诊断。
 * 诊断逻辑：记录最大值 = MAX(TBNAME 表的 NAME 列)，序列当前值 = SYSCAT.SEQUENCES.NEXTCACHEFIRSTVALUE - INCREMENT，
 * 记录最大值大于序列当前值（或序列/表查询失败）判定为 ABNORMAL。
 */
@Component
public class MaxSequenceService {

    @Inject
    private DataSource dataSource;

    private static final int BATCH_SIZE = 16;
    private static final int MAX_THREADS = 8;
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Z0-9_$#]+$");

    public Map<String, Object> list(String tbname, String name, String sequencename,
                                    int pageNum, int pageSize, String where) {
        StringBuilder whereSql = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendCondition(whereSql, params, "TBNAME", tbname);
        appendCondition(whereSql, params, "NAME", name);
        appendCondition(whereSql, params, "SEQUENCENAME", sequencename);
        appendCustomWhere(whereSql, where);

        int total = count(whereSql.toString(), params);
        List<Map<String, Object>> rows = queryPage(whereSql.toString(), params, pageNum, pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("rows", rows);
        return result;
    }

    /**
     * 诊断：ids 非空时只诊断指定行；否则按当前过滤条件诊断全部匹配行。
     */
    public List<Map<String, Object>> diagnose(String ids, String tbname, String name, String sequencename,
                                              String where) {
        StringBuilder whereSql = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        List<Long> idList = parseIds(ids);
        if (!idList.isEmpty()) {
            whereSql.append(" AND MAXSEQUENCEID IN (");
            for (int i = 0; i < idList.size(); i++) {
                whereSql.append(i == 0 ? "?" : ",?");
                params.add(idList.get(i));
            }
            whereSql.append(")");
        } else {
            appendCondition(whereSql, params, "TBNAME", tbname);
            appendCondition(whereSql, params, "NAME", name);
            appendCondition(whereSql, params, "SEQUENCENAME", sequencename);
            appendCustomWhere(whereSql, where);
        }

        List<Map<String, Object>> rows = queryAll(whereSql.toString(), params);
        if (rows.isEmpty()) {
            return rows;
        }

        Map<String, SequenceInfo> sequences = loadSequences();
        Map<Long, MaxResult> maxValues = loadMaxValues(rows);

        for (Map<String, Object> row : rows) {
            Long id = (Long) row.get("MAXSEQUENCEID");
            String seqName = str(row.get("SEQUENCENAME")).trim().toUpperCase(Locale.ROOT);
            SequenceInfo seq = sequences.get(seqName);
            MaxResult max = maxValues.get(id);
            List<String> errors = new ArrayList<>();

            Long recordMax = null;
            if (max == null) {
                errors.add("未执行记录最大值查询");
            } else if (!max.ok) {
                errors.add(max.message);
            } else {
                recordMax = max.value;
            }

            Long currentValue = null;
            if (seq == null) {
                errors.add("序列 " + seqName + " 在 SYSCAT.SEQUENCES 中不存在");
            } else {
                currentValue = seq.currentValue();
                if (currentValue == null) {
                    errors.add("无法获取序列 " + seqName + " 的当前值");
                }
            }

            row.put("V_MAXRECORD", recordMax);
            row.put("V_CURRENTVALUE", currentValue);
            if (!errors.isEmpty()) {
                row.put("V_STATUS", "ERROR");
                row.put("V_ERROR", String.join("; ", errors));
            } else if (recordMax != null && recordMax > currentValue) {
                row.put("V_STATUS", "ABNORMAL");
                row.put("V_ERROR", "序列当前值(" + currentValue + ")小于记录最大值(" + recordMax + ")");
            } else {
                row.put("V_STATUS", "NORMAL");
                row.put("V_ERROR", "");
            }
        }
        return rows;
    }

    /**
     * 修复序列：将指定记录的数据库序列重启为 记录最大值 + 1。
     * 无记录最大值或序列信息缺失时跳过并返回失败原因。
     */
    public List<Map<String, Object>> repair(String ids) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return results;
        }

        StringBuilder whereSql = new StringBuilder(" WHERE MAXSEQUENCEID IN (");
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            whereSql.append(i == 0 ? "?" : ",?");
            params.add(idList.get(i));
        }
        whereSql.append(")");

        List<Map<String, Object>> rows = queryAll(whereSql.toString(), params);
        if (rows.isEmpty()) {
            return results;
        }

        Map<String, SequenceInfo> sequences = loadSequences();
        for (Map<String, Object> row : rows) {
            Long id = (Long) row.get("MAXSEQUENCEID");
            String table = str(row.get("TBNAME")).trim().toUpperCase(Locale.ROOT);
            String column = str(row.get("NAME")).trim().toUpperCase(Locale.ROOT);
            String seqName = str(row.get("SEQUENCENAME")).trim().toUpperCase(Locale.ROOT);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("MAXSEQUENCEID", id);

            if (!IDENTIFIER.matcher(table).matches() || !IDENTIFIER.matcher(column).matches()) {
                result.put("SUCCESS", false);
                result.put("MESSAGE", "表名或列名包含非法字符");
                results.add(result);
                continue;
            }

            MaxResult max = querySingleMax(table, column);
            if (!max.ok) {
                result.put("SUCCESS", false);
                result.put("MESSAGE", max.message);
                results.add(result);
                continue;
            }
            if (max.value == null) {
                result.put("SUCCESS", true);
                result.put("MESSAGE", "记录为空，无需修复");
                results.add(result);
                continue;
            }

            SequenceInfo seq = sequences.get(seqName);
            if (seq == null) {
                result.put("SUCCESS", false);
                result.put("MESSAGE", "序列 " + seqName + " 在 SYSCAT.SEQUENCES 中不存在");
                results.add(result);
                continue;
            }
            if (!IDENTIFIER.matcher(seq.schema).matches() || !IDENTIFIER.matcher(seqName).matches()) {
                result.put("SUCCESS", false);
                result.put("MESSAGE", "序列名或模式名包含非法字符");
                results.add(result);
                continue;
            }

            long restartValue = max.value + 1;
            String alterSql = "ALTER SEQUENCE " + seq.schema + "." + seqName + " RESTART WITH " + restartValue;
            try (Connection conn = dataSource.getConnection();
                 java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate(alterSql);
                // DB2 执行 RESTART 后不会立即刷新 SYSCAT.SEQUENCES.NEXTCACHEFIRSTVALUE，
                // 需取一次 NEXT VALUE 触发刷新，否则再次诊断会把序列当前值读成旧值
                try (ResultSet rs = st.executeQuery("VALUES NEXT VALUE FOR " + seq.schema + "." + seqName)) {
                    if (rs.next()) {
                        rs.getObject(1);
                    }
                }
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                result.put("SUCCESS", true);
                result.put("MESSAGE", "序列已重启，下一个可用值为 "
                        + (restartValue + (seq.increment == null ? 1 : seq.increment)) + "（已预取 " + restartValue + " 刷新序列状态）");
            } catch (SQLException e) {
                result.put("SUCCESS", false);
                result.put("MESSAGE", truncate("重启序列失败: " + e.getMessage()));
            }
            results.add(result);
        }
        return results;
    }

    private int count(String whereSql, List<Object> params) {
        String sql = "SELECT COUNT(*) AS TOTAL FROM MAXSEQUENCE" + whereSql;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("TOTAL") : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXSEQUENCE 总数失败: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> queryPage(String whereSql, List<Object> params, int pageNum, int pageSize) {
        int size = Math.max(1, Math.min(pageSize, 500));
        int offset = (Math.max(pageNum, 1) - 1) * size;
        String sql = "SELECT * FROM MAXSEQUENCE" + whereSql
                + " ORDER BY MAXSEQUENCEID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> allParams = new ArrayList<>(params);
        allParams.add(offset);
        allParams.add(size);
        return query(sql, allParams);
    }

    private List<Map<String, Object>> queryAll(String whereSql, List<Object> params) {
        String sql = "SELECT * FROM MAXSEQUENCE" + whereSql + " ORDER BY MAXSEQUENCEID";
        return query(sql, params);
    }

    private List<Map<String, Object>> query(String sql, List<Object> params) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(readRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXSEQUENCE 失败: " + e.getMessage(), e);
        }
        return rows;
    }

    private Map<String, Object> readRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("MAXSEQUENCEID", toLong(rs.getObject("MAXSEQUENCEID")));
        row.put("TBNAME", rs.getString("TBNAME"));
        row.put("NAME", rs.getString("NAME"));
        row.put("SEQUENCENAME", rs.getString("SEQUENCENAME"));
        row.put("MAXRESERVED", toLong(rs.getObject("MAXRESERVED")));
        row.put("MAXVALUE", toLong(rs.getObject("MAXVALUE")));
        row.put("RANGE", toLong(rs.getObject("RANGE")));
        row.put("ROWSTAMP", toLong(rs.getObject("ROWSTAMP")));
        return row;
    }

    /**
     * 序列信息（按名字索引，同名的优先取 MAXIMO 模式）：当前值 = 下一缓存起始值 - 步长。
     */
    private Map<String, SequenceInfo> loadSequences() {
        Map<String, SequenceInfo> result = new HashMap<>();
        String sql = "SELECT SEQNAME, SEQSCHEMA, NEXTCACHEFIRSTVALUE, INCREMENT FROM SYSCAT.SEQUENCES";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = str(rs.getString("SEQNAME")).toUpperCase(Locale.ROOT);
                String schema = str(rs.getString("SEQSCHEMA")).trim().toUpperCase(Locale.ROOT);
                SequenceInfo info = new SequenceInfo(
                        toLong(rs.getObject("NEXTCACHEFIRSTVALUE")),
                        toLong(rs.getObject("INCREMENT")));
                SequenceInfo existing = result.get(name);
                if (existing == null || (!"MAXIMO".equals(existing.schema) && "MAXIMO".equals(schema))) {
                    info.schema = schema;
                    result.put(name, info);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 SYSCAT.SEQUENCES 失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 批量查询记录最大值：多条 MAX 查询拼成 UNION ALL 并发执行，批次失败时回退逐条查询。
     */
    private Map<Long, MaxResult> loadMaxValues(List<Map<String, Object>> rows) {
        Map<Long, MaxResult> results = new ConcurrentHashMap<>();
        List<Map<String, Object>> valid = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long id = (Long) row.get("MAXSEQUENCEID");
            String table = str(row.get("TBNAME")).trim().toUpperCase(Locale.ROOT);
            String column = str(row.get("NAME")).trim().toUpperCase(Locale.ROOT);
            if (id == null || !IDENTIFIER.matcher(table).matches() || !IDENTIFIER.matcher(column).matches()) {
                results.put(id == null ? -1L : id, new MaxResult(false, null, "表名或列名包含非法字符"));
            } else {
                valid.add(row);
            }
        }
        if (valid.isEmpty()) {
            return results;
        }

        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < valid.size(); i += BATCH_SIZE) {
            batches.add(valid.subList(i, Math.min(i + BATCH_SIZE, valid.size())));
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(MAX_THREADS, batches.size()));
        List<Future<?>> futures = new ArrayList<>();
        for (List<Map<String, Object>> batch : batches) {
            futures.add(pool.submit(() -> {
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(buildUnionMaxSql(batch));
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int idx = rs.getInt(1);
                        Long id = (Long) batch.get(idx).get("MAXSEQUENCEID");
                        results.put(id, new MaxResult(true, toLong(rs.getObject(2)), null));
                    }
                } catch (SQLException batchError) {
                    for (Map<String, Object> row : batch) {
                        Long id = (Long) row.get("MAXSEQUENCEID");
                        results.put(id, querySingleMax(
                                str(row.get("TBNAME")).trim().toUpperCase(Locale.ROOT),
                                str(row.get("NAME")).trim().toUpperCase(Locale.ROOT)));
                    }
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
                // 单批次异常已在上方回退处理，这里不再中断整体诊断
            }
        }
        pool.shutdown();
        return results;
    }

    private String buildUnionMaxSql(List<Map<String, Object>> batch) {
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT ").append(i).append(" AS RN, CAST(MAX(")
                    .append(str(batch.get(i).get("NAME")).trim().toUpperCase(Locale.ROOT))
                    .append(") AS VARCHAR(40)) AS MV FROM ")
                    .append(str(batch.get(i).get("TBNAME")).trim().toUpperCase(Locale.ROOT));
        }
        return sql.toString();
    }

    private MaxResult querySingleMax(String table, String column) {
        String sql = "SELECT MAX(" + column + ") AS MV FROM " + table;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return new MaxResult(true, rs.next() ? toLong(rs.getObject("MV")) : null, null);
        } catch (SQLException e) {
            return new MaxResult(false, null, truncate("查询 " + table + "." + column + " 最大值失败: " + e.getMessage()));
        }
    }

    private void appendCondition(StringBuilder whereSql, List<Object> params, String column, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String v = value.trim();
        whereSql.append(" AND ");
        if (v.startsWith("=")) {
            whereSql.append("UPPER(").append(column).append(") = ?");
            params.add(v.substring(1).trim().toUpperCase(Locale.ROOT));
        } else {
            String pattern = (v.contains("%") || v.contains("_")) ? v : "%" + v + "%";
            whereSql.append("UPPER(").append(column).append(") LIKE ?");
            params.add(pattern.toUpperCase(Locale.ROOT));
        }
    }

    private void appendCustomWhere(StringBuilder whereSql, String where) {
        if (where != null && !where.trim().isEmpty()) {
            whereSql.append(" AND (").append(where.trim()).append(")");
        }
    }

    private List<Long> parseIds(String ids) {
        List<Long> list = new ArrayList<>();
        if (ids == null || ids.trim().isEmpty()) {
            return list;
        }
        for (String part : ids.split(",")) {
            String v = part.trim();
            if (!v.isEmpty()) {
                try {
                    list.add(Long.parseLong(v));
                } catch (NumberFormatException ignored) {
                    // 忽略非法 id
                }
            }
        }
        return list;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class SequenceInfo {
        private String schema;
        private final Long nextCacheFirstValue;
        private final Long increment;

        private SequenceInfo(Long nextCacheFirstValue, Long increment) {
            this.nextCacheFirstValue = nextCacheFirstValue;
            this.increment = increment;
        }

        private Long currentValue() {
            if (nextCacheFirstValue == null || increment == null) {
                return null;
            }
            return nextCacheFirstValue - increment;
        }
    }

    private static class MaxResult {
        private final boolean ok;
        private final Long value;
        private final String message;

        private MaxResult(boolean ok, Long value, String message) {
            this.ok = ok;
            this.value = value;
            this.message = message;
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }
}
