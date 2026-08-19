package com.sks.server.service;

import com.sks.server.model.LoggerLevelConfig;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 日志级别配置管理（MySQL）
 * 仅负责配置的持久化，不与 Maximo 交互；推送日志级别由前端直连 Maximo 脚本完成。
 */
@Component
public class LoggerLevelService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    private static final Set<String> VALID_LEVELS = new HashSet<>(
            Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));

    /**
     * 查询全部配置（按排序、名称）
     */
    public List<Map<String, Object>> listConfigs() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, logger_name, log_level, ignored, description, sort_order, created_at, updated_at " +
                "FROM logger_level_config ORDER BY sort_order ASC, logger_name ASC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("查询日志级别配置失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 查询未忽略的配置（用于"更新到 Maximo"）
     */
    public List<Map<String, Object>> listActiveConfigs() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, logger_name, log_level, ignored, description, sort_order, created_at, updated_at " +
                "FROM logger_level_config WHERE ignored = 0 ORDER BY sort_order ASC, logger_name ASC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("查询未忽略日志级别配置失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 批量保存配置：全量覆盖（删除不在提交列表中的、更新已存在的、插入新的）。
     * 以 logger_name 作为唯一键。
     */
    public List<Map<String, Object>> saveConfigs(List<LoggerLevelConfig> loggers) {
        if (loggers == null) {
            loggers = Collections.emptyList();
        }

        // 校验并规整
        List<LoggerLevelConfig> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (LoggerLevelConfig c : loggers) {
            String name = c.getLoggerName() == null ? "" : c.getLoggerName().trim();
            if (name.isEmpty()) {
                continue; // 跳过空名称行
            }
            String level = c.getLevel() == null ? "INFO" : c.getLevel().trim().toUpperCase();
            if (!VALID_LEVELS.contains(level)) {
                throw new RuntimeException("不支持的日志级别: " + c.getLevel() + " (logger: " + name + ")");
            }
            if (!seen.add(name)) {
                throw new RuntimeException("日志器名称重复: " + name);
            }
            LoggerLevelConfig n = new LoggerLevelConfig();
            n.setLoggerName(name);
            n.setLevel(level);
            n.setIgnored(Boolean.TRUE.equals(c.getIgnored()));
            n.setDescription(c.getDescription() == null ? "" : c.getDescription().trim());
            n.setSortOrder(c.getSortOrder() == null ? 0 : c.getSortOrder());
            valid.add(n);
        }

        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 删除不在提交列表中的行
                if (valid.isEmpty()) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("DELETE FROM logger_level_config");
                    }
                } else {
                    String placeholders = String.join(",", Collections.nCopies(valid.size(), "?"));
                    String delSql = "DELETE FROM logger_level_config WHERE logger_name NOT IN (" + placeholders + ")";
                    try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                        int idx = 1;
                        for (LoggerLevelConfig c : valid) {
                            ps.setString(idx++, c.getLoggerName());
                        }
                        ps.executeUpdate();
                    }
                }

                // upsert（依赖 uk_logger_name 唯一键）
                String upsertSql = "INSERT INTO logger_level_config (logger_name, log_level, ignored, description, sort_order) " +
                        "VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE log_level = VALUES(log_level), ignored = VALUES(ignored), " +
                        "description = VALUES(description), sort_order = VALUES(sort_order)";
                try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                    int order = 0;
                    for (LoggerLevelConfig c : valid) {
                        ps.setString(1, c.getLoggerName());
                        ps.setString(2, c.getLevel());
                        ps.setInt(3, Boolean.TRUE.equals(c.getIgnored()) ? 1 : 0);
                        ps.setString(4, c.getDescription());
                        // 若前端未提供有效排序，则按提交顺序赋值
                        ps.setInt(5, c.getSortOrder() != null && c.getSortOrder() != 0 ? c.getSortOrder() : order++);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("保存日志级别配置失败: " + e.getMessage(), e);
        }

        return listConfigs();
    }

    /**
     * 删除单条配置
     */
    public boolean deleteConfig(Long id) {
        String sql = "DELETE FROM logger_level_config WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除日志级别配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 增量导入配置到默认表（logger_level_config）：
     * 已存在的 logger_name 跳过，仅插入不存在的。返回 {added, skipped, total}
     */
    public Map<String, Object> importConfigs(List<LoggerLevelConfig> loggers) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (loggers == null) {
            loggers = Collections.emptyList();
        }

        // 规整 + 去重入参
        LinkedHashMap<String, LoggerLevelConfig> toCheck = new LinkedHashMap<>();
        for (LoggerLevelConfig c : loggers) {
            String name = c.getLoggerName() == null ? "" : c.getLoggerName().trim();
            if (name.isEmpty()) {
                continue;
            }
            String level = c.getLevel() == null ? "INFO" : c.getLevel().trim().toUpperCase();
            if (!VALID_LEVELS.contains(level)) {
                throw new RuntimeException("不支持的日志级别: " + c.getLevel() + " (logger: " + name + ")");
            }
            if (!toCheck.containsKey(name)) {
                LoggerLevelConfig n = new LoggerLevelConfig();
                n.setLoggerName(name);
                n.setLevel(level);
                n.setIgnored(Boolean.TRUE.equals(c.getIgnored()));
                n.setDescription(c.getDescription() == null ? "" : c.getDescription().trim());
                toCheck.put(name, n);
            }
        }

        int inputCount = toCheck.size();
        if (inputCount == 0) {
            result.put("added", 0);
            result.put("skipped", 0);
            result.put("total", listConfigs().size());
            return result;
        }

        try (Connection conn = mysqlDataSource.getConnection()) {
            // 查已存在的 logger_name
            Set<String> existing = new HashSet<>();
            String querySql = "SELECT logger_name FROM logger_level_config WHERE logger_name IN (" +
                    String.join(",", Collections.nCopies(inputCount, "?")) + ")";
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                int idx = 1;
                for (String name : toCheck.keySet()) {
                    ps.setString(idx++, name);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        existing.add(rs.getString("logger_name"));
                    }
                }
            }

            // 差集 = 入参中不存在的
            List<LoggerLevelConfig> toInsert = new ArrayList<>();
            for (Map.Entry<String, LoggerLevelConfig> e : toCheck.entrySet()) {
                if (!existing.contains(e.getKey())) {
                    toInsert.add(e.getValue());
                }
            }

            if (!toInsert.isEmpty()) {
                conn.setAutoCommit(false);
                try {
                    String insertSql = "INSERT INTO logger_level_config (logger_name, log_level, ignored, description, sort_order) " +
                            "VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        int order = 0;
                        for (LoggerLevelConfig c : toInsert) {
                            ps.setString(1, c.getLoggerName());
                            ps.setString(2, c.getLevel());
                            ps.setInt(3, Boolean.TRUE.equals(c.getIgnored()) ? 1 : 0);
                            ps.setString(4, c.getDescription());
                            ps.setInt(5, order++);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                    conn.commit();
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }

            result.put("added", toInsert.size());
            result.put("skipped", inputCount - toInsert.size());
            result.put("total", listConfigs().size());
        } catch (Exception e) {
            throw new RuntimeException("导入日志级别配置失败: " + e.getMessage(), e);
        }
        return result;
    }

    private Map<String, Object> mapRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("loggerName", rs.getString("logger_name"));
        row.put("level", rs.getString("log_level"));
        row.put("ignored", rs.getInt("ignored") == 1);
        row.put("description", rs.getString("description"));
        row.put("sortOrder", rs.getInt("sort_order"));
        row.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null);
        row.put("updatedAt", rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : null);
        return row;
    }
}
