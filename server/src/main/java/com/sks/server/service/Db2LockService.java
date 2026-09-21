package com.sks.server.service;

import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * DB2 锁表查询服务
 */
@Component
public class Db2LockService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询锁定的表（支持 TABNAME 精确/模糊过滤）
     */
    public Map<String, Object> queryList(String tabName, int pageNum, int pageSize) {
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        whereSql.append(" WHERE RTRIM(lh.TABSCHEMA) = 'MAXIMO'");
        if (tabName != null && !tabName.trim().isEmpty()) {
            String v = tabName.trim();
            if (v.startsWith("=")) {
                whereSql.append(" AND RTRIM(lh.TABNAME) = ?");
                params.add(v.substring(1));
            } else {
                whereSql.append(" AND UPPER(lh.TABNAME) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }

        String whereStr = whereSql.toString();
        String joins = " FROM SYSIBMADM.LOCKS_HELD AS lh " +
                "LEFT JOIN TABLE(MON_GET_UNIT_OF_WORK(NULL, -2)) AS uow " +
                "ON lh.AGENT_ID = uow.APPLICATION_HANDLE " +
                "LEFT JOIN SYSIBMADM.APPLICATIONS AS app " +
                "ON lh.AGENT_ID = app.AGENT_ID";

        // 总数
        String countSql = "SELECT COUNT(*) AS total" + joins + whereStr;
        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询锁表总数失败: " + e.getMessage(), e);
        }

        // 分页数据
        String dataSql = "SELECT lh.AGENT_ID, uow.UOW_START_TIME, lh.APPL_NAME, lh.AUTHID, " +
                "lh.TABSCHEMA, lh.TABNAME, lh.LOCK_OBJECT_TYPE, lh.LOCK_MODE, lh.LOCK_STATUS, " +
                "lh.TBSP_NAME, lh.LOCK_NAME, uow.WORKLOAD_OCCURRENCE_STATE AS UOW_STATE, " +
                "uow.TOTAL_APP_COMMITS, uow.TOTAL_APP_ROLLBACKS, app.APPL_STATUS, " +
                "app.CLIENT_NNAME AS CLIENT_IP, app.CLIENT_PRDID, lh.SNAPSHOT_TIMESTAMP " +
                joins + whereStr +
                " ORDER BY uow.UOW_START_TIME " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.setInt(params.size() + 1, offset);
            ps.setInt(params.size() + 2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询锁表列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("AGENT_ID", rs.getLong("AGENT_ID"));
        row.put("UOW_START_TIME", rs.getTimestamp("UOW_START_TIME"));
        row.put("APPL_NAME", rs.getString("APPL_NAME"));
        row.put("AUTHID", rs.getString("AUTHID"));
        row.put("TABSCHEMA", rs.getString("TABSCHEMA"));
        row.put("TABNAME", rs.getString("TABNAME"));
        row.put("LOCK_OBJECT_TYPE", rs.getString("LOCK_OBJECT_TYPE"));
        row.put("LOCK_MODE", rs.getString("LOCK_MODE"));
        row.put("LOCK_STATUS", rs.getString("LOCK_STATUS"));
        row.put("TBSP_NAME", rs.getString("TBSP_NAME"));
        row.put("LOCK_NAME", rs.getString("LOCK_NAME"));
        row.put("UOW_STATE", rs.getString("UOW_STATE"));
        row.put("TOTAL_APP_COMMITS", rs.getLong("TOTAL_APP_COMMITS"));
        row.put("TOTAL_APP_ROLLBACKS", rs.getLong("TOTAL_APP_ROLLBACKS"));
        row.put("APPL_STATUS", rs.getString("APPL_STATUS"));
        row.put("CLIENT_IP", rs.getString("CLIENT_IP"));
        row.put("CLIENT_PRDID", rs.getString("CLIENT_PRDID"));
        row.put("SNAPSHOT_TIMESTAMP", rs.getTimestamp("SNAPSHOT_TIMESTAMP"));
        return row;
    }

    // ========================================================================
    //                           锁诊断（固定诊断SQL集 + 汇总分析）
    // ========================================================================

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int VALUE_MAX_LEN = 80;

    /**
     * 对指定进程执行固定诊断 SQL 集，并输出「汇总分析」文本报告。
     *
     * <p>关于「进程正在执行的 SQL」：Maximo 通过 JCC PreparedStatement 访问 DB2，
     * 监控视图中的 STMT_TEXT 只能取到 com.ibm.db2.jcc.am.xx@hash 之类的对象引用，
     * 无法通过 SQL 还原真实语句文本。如需精确定位，只能在数据库服务器上用 OS 层工具：
     * <pre>
     *   db2pd -d &lt;DB&gt; -app | grep &lt;AGENT_ID&gt;        # 取得 L-AnchID / L-StmtUID
     *   db2pd -d &lt;DB&gt; -dyn | awk '$2==&lt;AnchID&gt; &amp;&amp; $3==&lt;StmtUID&gt;'
     * </pre>
     * 本诊断因此聚焦「锁 / 事务 / 阻塞链」维度，用于判断问题来源与影响面。
     *
     * @param agentId 目标进程号
     * @param tabName 前端已知的表名（可空，作为兜底；优先使用锁信息中解析出的表名）
     */
    public Map<String, Object> diagnose(long agentId, String tabName) {
        String sqlSet = Solon.cfg().get("db2.diagnose.sqlSet", "v115");
        int queryTimeout = Solon.cfg().getInt("db2.diagnose.queryTimeout", 15);
        int longRunningMinutes = Solon.cfg().getInt("db2.diagnose.longRunningMinutes", 30);

        String tableSchema = Solon.cfg().get("db2.schema", "maximo").toUpperCase();
        String tableName = tabName == null ? "" : tabName.trim().toUpperCase();

        List<DiagnoseStep> steps = new ArrayList<>();
        String dbVersion = "";
        String fatalError = null;
        long t0 = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection()) {
            // 诊断只读，显式关闭自动提交无意义；保持自动提交避免占用事务
            conn.setAutoCommit(true);
            dbVersion = safeDbVersion(conn);

            // --- 1. 会话与工作单元概况 ---
            DiagnoseStep s1 = runQuery(conn, queryTimeout, "会话与工作单元概况",
                    "SELECT app.AGENT_ID, app.APPL_NAME, app.APPL_STATUS, app.CLIENT_NNAME, " +
                            "app.CLIENT_PRDID, app.CLIENT_PID, uow.UOW_START_TIME, " +
                            "uow.WORKLOAD_OCCURRENCE_STATE, uow.TOTAL_APP_COMMITS, uow.TOTAL_APP_ROLLBACKS " +
                            "FROM SYSIBMADM.APPLICATIONS app " +
                            "LEFT JOIN TABLE(MON_GET_UNIT_OF_WORK(NULL, -2)) uow " +
                            "ON app.AGENT_ID = uow.APPLICATION_HANDLE " +
                            "WHERE app.AGENT_ID = ?",
                    agentId);
            steps.add(s1);

            // --- 2. 该进程当前持有的锁 ---
            DiagnoseStep s2 = runQuery(conn, queryTimeout, "该进程当前持有的锁",
                    "SELECT AGENT_ID, TABSCHEMA, TABNAME, LOCK_OBJECT_TYPE, LOCK_MODE, " +
                            "LOCK_STATUS, TBSP_NAME, LOCK_NAME, APPL_NAME, AUTHID, SNAPSHOT_TIMESTAMP " +
                            "FROM SYSIBMADM.LOCKS_HELD " +
                            "WHERE AGENT_ID = ? " +
                            "ORDER BY TABNAME, LOCK_MODE",
                    agentId);
            steps.add(s2);

            // 优先使用锁信息里解析出的表名（比前端传入更准确）
            Map<String, Object> firstLock = firstRow(s2);
            if (firstLock != null) {
                String ts = str(firstLock.get("TABSCHEMA"));
                String tn = str(firstLock.get("TABNAME"));
                if (!ts.isEmpty()) tableSchema = ts;
                if (!tn.isEmpty()) tableName = tn;
            }

            // --- 3. 该进程正在等待的锁（自身被阻塞） ---
            DiagnoseStep s3 = runQuery(conn, queryTimeout, "该进程正在等待的锁（自身被阻塞）",
                    "SELECT AGENT_ID, AGENT_ID_HOLDING_LK, APPL_ID_HOLDING_LK, TABSCHEMA, TABNAME, " +
                            "LOCK_MODE, LOCK_MODE_REQUESTED, LOCK_WAIT_START_TIME " +
                            "FROM SYSIBMADM.LOCKWAITS WHERE AGENT_ID = ?",
                    agentId);
            steps.add(s3);

            // --- 4. 正在等待该进程的锁（本进程阻塞了别人） ---
            DiagnoseStep s4 = runQuery(conn, queryTimeout, "正在等待该进程的锁（本进程阻塞了别人）",
                    "SELECT AGENT_ID, AGENT_ID_HOLDING_LK, APPL_ID_HOLDING_LK, TABSCHEMA, TABNAME, " +
                            "LOCK_MODE, LOCK_MODE_REQUESTED, LOCK_WAIT_START_TIME " +
                            "FROM SYSIBMADM.LOCKWAITS WHERE AGENT_ID_HOLDING_LK = ?",
                    agentId);
            steps.add(s4);

            // --- 5. 同一张表上的其他锁持有者 ---
            if (tableName.isEmpty()) {
                DiagnoseStep s5 = new DiagnoseStep();
                s5.name = "同一张表上的其他锁持有者";
                s5.sql = "-- 未能从锁信息中解析出表名（该进程当前可能未持有任何锁），跳过本步";
                s5.error = null;
                s5.elapsedMs = 0;
                steps.add(s5);
            } else {
                steps.add(runQuery(conn, queryTimeout, "同一张表上的其他锁持有者",
                        "SELECT AGENT_ID, LOCK_MODE, LOCK_OBJECT_TYPE, LOCK_STATUS, TABSCHEMA, TABNAME, " +
                                "APPL_NAME, AUTHID " +
                                "FROM SYSIBMADM.LOCKS_HELD " +
                                "WHERE RTRIM(TABSCHEMA) = ? AND RTRIM(TABNAME) = ? AND AGENT_ID <> ? " +
                                "ORDER BY LOCK_MODE, AGENT_ID",
                        tableSchema, tableName, agentId));
            }

            // --- 6. 工作单元活动指标 ---
            steps.add(runQuery(conn, queryTimeout, "工作单元活动指标",
                    "SELECT APPLICATION_HANDLE, UOW_ID, ACTIVITY_ID, ACTIVITY_STATE, ACTIVITY_TYPE, " +
                            "LOCK_WAITS, LOCK_WAIT_TIME, LOCK_TIMEOUTS, ROWS_READ, ROWS_MODIFIED, " +
                            "TOTAL_ACT_TIME, ENTRY_TIME " +
                            "FROM TABLE(MON_GET_ACTIVITY(NULL, -2)) WHERE APPLICATION_HANDLE = ?",
                    agentId));
        } catch (SQLException e) {
            fatalError = "获取数据库连接失败: SQLCODE=" + e.getErrorCode()
                    + ", SQLSTATE=" + e.getSQLState() + ", " + e.getMessage();
        }

        long elapsed = System.currentTimeMillis() - t0;
        String report = buildReport(agentId, sqlSet, dbVersion, tableSchema, tableName,
                longRunningMinutes, steps, fatalError, elapsed);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentId", agentId);
        result.put("dbVersion", dbVersion);
        result.put("sqlSet", sqlSet);
        result.put("elapsedMs", elapsed);
        result.put("reportText", report);

        List<Map<String, Object>> stepInfos = new ArrayList<>();
        for (DiagnoseStep s : steps) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", s.name);
            info.put("sql", s.sql);
            info.put("rowCount", s.rows.size());
            info.put("error", s.error);
            info.put("elapsedMs", s.elapsedMs);
            stepInfos.add(info);
        }
        result.put("steps", stepInfos);
        return result;
    }

    // ---------------------------- 内部工具 ----------------------------

    /** 单条诊断 SQL 的执行结果 */
    private static class DiagnoseStep {
        String name;
        String sql;
        final List<String> columns = new ArrayList<>();
        final List<Map<String, Object>> rows = new ArrayList<>();
        String error;
        long elapsedMs;
    }

    /**
     * 执行一条诊断 SQL 并把结果集按「列标签 -> 值」通用地读出来。
     * 这样即使不同 DB2 版本视图列有增减，也不会因为多/少一列而整体报错。
     */
    private DiagnoseStep runQuery(Connection conn, int timeoutSeconds, String name, String sql, Object... params) {
        DiagnoseStep step = new DiagnoseStep();
        step.name = name;
        step.sql = sql;
        long t0 = System.currentTimeMillis();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (timeoutSeconds > 0) {
                ps.setQueryTimeout(timeoutSeconds);
            }
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                for (int i = 1; i <= n; i++) {
                    step.columns.add(md.getColumnLabel(i));
                }
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) {
                        row.put(step.columns.get(i - 1), normalize(rs.getObject(i)));
                    }
                    step.rows.add(row);
                }
            }
        } catch (SQLException e) {
            step.error = "SQLCODE=" + e.getErrorCode() + ", SQLSTATE=" + e.getSQLState() + ", " + e.getMessage();
        }
        step.elapsedMs = System.currentTimeMillis() - t0;
        return step;
    }

    /** JCC 对象引用（com.ibm.db2.jcc.am.xx@hash）不可读，转成可读标记 */
    private static Object normalize(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number || v instanceof String || v instanceof Boolean
                || v instanceof Timestamp || v instanceof java.sql.Date || v instanceof java.sql.Time) {
            return v;
        }
        String s = String.valueOf(v);
        if (s.startsWith("com.ibm.db2.jcc.")) {
            return "[JCC对象引用 " + s + "]";
        }
        return s;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static long lng(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(str(v));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private static Map<String, Object> firstRow(DiagnoseStep step) {
        return (step != null && !step.rows.isEmpty()) ? step.rows.get(0) : null;
    }

    private static String safeDbVersion(Connection conn) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            return md.getDatabaseProductName() + " " + md.getDatabaseProductVersion();
        } catch (SQLException e) {
            return "未知";
        }
    }

    /** 单元格显示值（Timestamp 格式化，其余直接 toString） */
    private static String cell(Object v) {
        if (v == null) {
            return "-";
        }
        if (v instanceof Timestamp) {
            return ((Timestamp) v).toLocalDateTime().format(DT_FMT);
        }
        String s = String.valueOf(v).replace('\r', ' ').replace('\n', ' ');
        if (s.length() > VALUE_MAX_LEN) {
            s = s.substring(0, VALUE_MAX_LEN) + "...";
        }
        return s.isEmpty() ? "-" : s;
    }

    private static String nowStr() {
        return LocalDateTime.now().format(DT_FMT);
    }

    private static String padRight(String s, int width) {
        StringBuilder sb = new StringBuilder(s == null ? "" : s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String repeat(char c, int n) {
        char[] buf = new char[Math.max(n, 0)];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    private static String line(char ch, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    /** 把一条诊断 SQL 的结果渲染成文本（0 行/1 行/多行三种排版） */
    private static String renderStepResult(DiagnoseStep step) {
        StringBuilder sb = new StringBuilder();
        if (step.error != null) {
            sb.append("  【执行失败】").append(step.error).append('\n');
            return sb.toString();
        }
        if (step.rows.isEmpty()) {
            sb.append("  结果：无数据（0 行）\n");
            return sb.toString();
        }
        if (step.rows.size() == 1) {
            sb.append("  结果（1 行）：\n");
            for (Map.Entry<String, Object> e : step.rows.get(0).entrySet()) {
                sb.append("      ").append(padRight(e.getKey(), 26))
                        .append(" = ").append(cell(e.getValue())).append('\n');
            }
            return sb.toString();
        }
        // 多行 -> 等宽表格
        int n = step.columns.size();
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            w[i] = step.columns.get(i).length();
        }
        for (Map<String, Object> row : step.rows) {
            for (int i = 0; i < n; i++) {
                w[i] = Math.max(w[i], cell(row.get(step.columns.get(i))).length());
            }
        }
        sb.append("  结果（").append(step.rows.size()).append(" 行）：\n");
        sb.append("      ");
        for (int i = 0; i < n; i++) {
            sb.append(padRight(step.columns.get(i), w[i]));
            if (i < n - 1) {
                sb.append("  ");
            }
        }
        sb.append('\n').append("      ");
        for (int i = 0; i < n; i++) {
            sb.append(repeat('-', w[i]));
            if (i < n - 1) {
                sb.append("  ");
            }
        }
        sb.append('\n');
        for (Map<String, Object> row : step.rows) {
            sb.append("      ");
            for (int i = 0; i < n; i++) {
                sb.append(padRight(cell(row.get(step.columns.get(i))), w[i]));
                if (i < n - 1) {
                    sb.append("  ");
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** 组装最终诊断报告文本 */
    private String buildReport(long agentId, String sqlSet, String dbVersion,
                               String tableSchema, String tableName, int longRunningMinutes,
                               List<DiagnoseStep> steps, String fatalError, long elapsedMs) {
        StringBuilder sb = new StringBuilder();
        String bar = line('=', 78);

        sb.append(bar).append('\n');
        sb.append("                        DB2 锁诊断报告\n");
        sb.append(bar).append('\n');
        sb.append("  目标进程 AGENT_ID : ").append(agentId).append('\n');
        sb.append("  诊断时间          : ").append(nowStr()).append('\n');
        sb.append("  数据库版本        : ").append(dbVersion).append('\n');
        sb.append("  诊断SQL集         : ").append(sqlSet).append('\n');
        sb.append("  目标表            : ").append(tableSchema).append('.').append(tableName).append('\n');
        sb.append("  总耗时            : ").append(elapsedMs).append(" ms\n");
        sb.append(bar).append('\n');

        if (fatalError != null) {
            sb.append('\n').append("  【致命错误】").append(fatalError).append('\n');
            sb.append(bar).append('\n');
            return sb.toString();
        }

        // ---------- 各步骤明细 ----------
        for (int i = 0; i < steps.size(); i++) {
            DiagnoseStep step = steps.get(i);
            sb.append('\n');
            sb.append(line('-', 78)).append('\n');
            sb.append('[').append(i + 1).append("] ").append(step.name)
                    .append("    (").append(step.elapsedMs).append(" ms)\n");
            sb.append(line('-', 78)).append('\n');
            sb.append("  执行的 SQL：\n");
            for (String s : step.sql.split("\n")) {
                sb.append("      ").append(s).append('\n');
            }
            sb.append('\n');
            sb.append(renderStepResult(step));
        }

        // ---------- 汇总分析 ----------
        sb.append('\n');
        sb.append(bar).append('\n');
        sb.append("                        汇总分析\n");
        sb.append(bar).append('\n');
        sb.append(buildAnalysis(agentId, steps, longRunningMinutes, tableSchema, tableName));
        sb.append('\n');
        sb.append(bar).append('\n');
        sb.append("  提示：SQL 无法取到进程正在执行的真实语句（JCC PreparedStatement 会导致\n");
        sb.append("        STMT_TEXT 只返回 com.ibm.db2.jcc.am.xx@hash 对象引用）。\n");
        sb.append("        如需精确定位 SQL，请在数据库服务器上执行：\n");
        sb.append("          db2pd -d <DB> -app | grep ").append(agentId).append("\n");
        sb.append("            -> 记下输出中的 L-AnchID 与 L-StmtUID\n");
        sb.append("          db2pd -d <DB> -dyn | awk '$2==<L-AnchID> && $3==<L-StmtUID>'\n");
        sb.append("            -> 输出末列即为该游标对应的真实 SQL 文本\n");
        sb.append(bar).append('\n');
        return sb.toString();
    }

    /** 规则化汇总分析 */
    private String buildAnalysis(long agentId, List<DiagnoseStep> steps, int longRunningMinutes,
                                 String tableSchema, String tableName) {
        DiagnoseStep session = stepAt(steps, 0);
        DiagnoseStep locks = stepAt(steps, 1);
        DiagnoseStep waiting = stepAt(steps, 2);
        DiagnoseStep waitedBy = stepAt(steps, 3);
        DiagnoseStep sameTable = stepAt(steps, 4);
        DiagnoseStep activity = stepAt(steps, 5);

        Map<String, Object> s1 = firstRow(session);
        StringBuilder sb = new StringBuilder();

        // ----- 事务概况 -----
        String uowState = s1 == null ? "" : str(s1.get("WORKLOAD_OCCURRENCE_STATE"));
        String applStatus = s1 == null ? "" : str(s1.get("APPL_STATUS"));
        String clientIp = s1 == null ? "" : str(s1.get("CLIENT_NNAME"));
        String applName = s1 == null ? "" : str(s1.get("APPL_NAME"));
        long commits = s1 == null ? 0L : lng(s1.get("TOTAL_APP_COMMITS"));
        long rollbacks = s1 == null ? 0L : lng(s1.get("TOTAL_APP_ROLLBACKS"));
        Object uowStartObj = s1 == null ? null : s1.get("UOW_START_TIME");
        long ageMinutes = computeAgeMinutes(uowStartObj);

        sb.append('\n').append("  【一、事务概况】\n");
        if (s1 == null) {
            sb.append("     未查询到该进程的会话信息，进程可能已结束或已断开。\n");
        } else {
            sb.append("     应用名 / 认证          : ").append(applName).append('\n');
            sb.append("     客户端IP               : ").append(clientIp.isEmpty() ? "-" : clientIp).append('\n');
            sb.append("     应用状态 / 工作单元状态 : ").append(applStatus).append(" / ").append(uowState).append('\n');
            sb.append("     事务开始时间           : ").append(cell(uowStartObj)).append('\n');
            sb.append("     事务已持续             : ")
                    .append(ageMinutes < 0 ? "未知" : ageMinutes + " 分钟"
                            + (ageMinutes >= 60 ? "（约 " + (ageMinutes / 60) + " 小时"
                            + (ageMinutes >= 1440 ? " / " + (ageMinutes / 1440) + " 天" : "") + "）" : ""))
                    .append('\n');
            sb.append("     提交次数 / 回滚次数    : ").append(commits).append(" / ").append(rollbacks).append('\n');
        }

        // ----- 锁情况 -----
        int lockCount = locks == null ? 0 : locks.rows.size();
        int waitSelfCount = waiting == null ? 0 : waiting.rows.size();
        int waitByCount = waitedBy == null ? 0 : waitedBy.rows.size();
        int sameTableCount = sameTable == null ? 0 : sameTable.rows.size();

        sb.append('\n').append("  【二、锁情况】\n");
        if (locks != null && locks.error != null) {
            sb.append("     持有锁查询失败：").append(locks.error).append('\n');
        } else if (lockCount == 0) {
            sb.append("     该进程当前未持有任何锁（可能已提交/回滚，或已释放锁）。\n");
        } else {
            sb.append("     该进程持有 ").append(lockCount).append(" 个锁：\n");
            for (Map<String, Object> row : locks.rows) {
                sb.append("        - ").append(str(row.get("TABSCHEMA"))).append('.')
                        .append(str(row.get("TABNAME")))
                        .append("  [").append(str(row.get("LOCK_OBJECT_TYPE")))
                        .append(" / ").append(str(row.get("LOCK_MODE")))
                        .append(" / ").append(str(row.get("LOCK_STATUS"))).append("]\n");
            }
        }
        sb.append("     该进程正在等待其他锁     : ").append(waitSelfCount).append(" 个\n");
        sb.append("     正在等待该进程释放锁     : ").append(waitByCount).append(" 个\n");
        sb.append("     同一张表上的其他锁持有者 : ").append(sameTableCount).append(" 个\n");

        // ----- 活动指标 -----
        sb.append('\n').append("  【三、活动指标】\n");
        if (activity == null || activity.error != null && activity.rows.isEmpty()) {
            sb.append("     无活动记录（该会话当前没有正在执行的活动，通常意味着游标/事务处于挂起等待状态）。\n");
        } else if (activity.rows.isEmpty()) {
            sb.append("     无活动记录（该会话当前没有正在执行的活动，通常意味着游标/事务处于挂起等待状态）。\n");
        } else {
            for (Map<String, Object> row : activity.rows) {
                sb.append("        - ACTIVITY_ID=").append(str(row.get("ACTIVITY_ID")))
                        .append(", 状态=").append(str(row.get("ACTIVITY_STATE")))
                        .append(", 类型=").append(str(row.get("ACTIVITY_TYPE")))
                        .append(", 锁等待次数=").append(str(row.get("LOCK_WAITS")))
                        .append(", 锁等待耗时=").append(str(row.get("LOCK_WAIT_TIME"))).append(" ms")
                        .append(", 锁超时次数=").append(str(row.get("LOCK_TIMEOUTS")))
                        .append(", 读行数=").append(str(row.get("ROWS_READ")))
                        .append(", 改行数=").append(str(row.get("ROWS_MODIFIED")))
                        .append(", 耗时=").append(str(row.get("TOTAL_ACT_TIME"))).append(" ms\n");
            }
        }

        // ----- 风险判定 -----
        List<String> reasons = new ArrayList<>();
        boolean isLongRunning = ageMinutes >= 0 && ageMinutes >= longRunningMinutes;
        boolean neverCommitted = s1 != null && commits == 0 && rollbacks == 0;
        boolean isIdleOrWait = "UOWWAIT".equalsIgnoreCase(uowState) || "UOWWAIT".equalsIgnoreCase(applStatus);
        boolean blockingOthers = waitByCount > 0;
        boolean blockedSelf = waitSelfCount > 0;
        boolean holdsLock = lockCount > 0;

        if (isLongRunning) {
            reasons.add("事务已持续 " + ageMinutes + " 分钟，超过阈值 " + longRunningMinutes
                    + " 分钟，属于长事务（长事务会长期占用锁与日志，导致其他事务等待）");
        }
        if (neverCommitted) {
            reasons.add("提交次数与回滚次数均为 0，该事务从未提交/回滚，属于未提交事务（悬空事务），"
                    + "存在锁无法释放与日志堆积风险");
        }
        if (isIdleOrWait) {
            reasons.add("应用/工作单元处于 UOWWAIT 状态，事务已把锁握在手里但不再推进，"
                    + "典型场景是应用侧拿到游标后不再 fetch/commit（连接被长事务占用）");
        }
        if (holdsLock) {
            reasons.add("该进程在 " + tableSchema + "." + tableName + " 等表上持有锁，"
                    + "会阻塞同表其他事务的写操作，以及该表的 DDL（DDL 需要 Z 锁，与任何锁都不兼容）");
        }
        if (blockingOthers) {
            reasons.add("有 " + waitByCount + " 个会话正在等待该进程释放锁，已形成阻塞链，"
                    + "该进程是当前阻塞源，影响面正在扩大");
        }
        if (blockedSelf) {
            reasons.add("该进程自身正在等待 " + waitSelfCount + " 个锁，说明它也是被阻塞方，需继续向上游追溯");
        }
        if (sameTableCount > 0) {
            reasons.add("同一张表（" + tableSchema + "." + tableName + "）上另有 " + sameTableCount
                    + " 个锁持有者，属于并发访问热点表，需注意锁升级（LOCK_ESCALATION）风险");
        }

        String level;
        if (blockingOthers && (isLongRunning || neverCommitted)) {
            level = "高（正在阻塞其他会话，且事务长期未提交）";
        } else if (blockingOthers || isLongRunning || neverCommitted) {
            level = "中";
        } else if (holdsLock) {
            level = "低";
        } else {
            level = "低（未发现明显异常）";
        }

        sb.append('\n').append("  【四、风险判定】").append(level).append('\n');
        if (reasons.isEmpty()) {
            sb.append("     未命中任何异常规则：事务时长达标、提交正常、未形成阻塞链。\n");
        } else {
            sb.append("     判定依据：\n");
            for (int i = 0; i < reasons.size(); i++) {
                sb.append("       ").append(i + 1).append(". ").append(reasons.get(i)).append('\n');
            }
        }

        // ----- 处置建议 -----
        sb.append('\n').append("  【五、处置建议】\n");
        if (!holdingAnyIssue(isLongRunning, neverCommitted, isIdleOrWait, holdsLock, blockingOthers)) {
            sb.append("     暂无需处置，可继续观察。\n");
        } else {
            sb.append("     1. 先确认业务侧是否仍有该会话对应的操作在进行");
            if (!clientIp.isEmpty()) {
                sb.append("（客户端IP：").append(clientIp).append("）");
            }
            sb.append("。该进程长期挂起但客户端未提交，多数情况下是应用侧连接被占用/异常退出导致。\n");
            sb.append("     2. 确认无业务影响后，可强制终止该会话释放锁：\n");
            sb.append("          CALL SYSPROC.ADMIN_CMD('force application (").append(agentId).append(")');\n");
            if (blockingOthers) {
                sb.append("        注意：当前已有 ").append(waitByCount)
                        .append(" 个会话被它阻塞，建议优先处理。\n");
            }
            sb.append("     3. 若该现象反复出现，建议开启/收紧 Maximo 长连接自动回收（7.5.0.3+）：\n");
            sb.append("          mxe.db.closelongrunconn=true\n");
            sb.append("          mxe.db.longruntimelimit=").append(Math.max(longRunningMinutes, 30)).append('\n');
            sb.append("          mxe.db.detectlongrunconninterval=30\n");
            sb.append("     4. 建议排查应用侧：是否存在查询后未关闭游标 / 未 commit 的代码路径。\n");
        }
        return sb.toString();
    }

    private static DiagnoseStep stepAt(List<DiagnoseStep> steps, int idx) {
        return (steps != null && idx >= 0 && idx < steps.size()) ? steps.get(idx) : null;
    }

    private static boolean holdingAnyIssue(boolean... flags) {
        for (boolean f : flags) {
            if (f) {
                return true;
            }
        }
        return false;
    }

    /** 由事务开始时间推算已持续分钟数，解析失败返回 -1 */
    private static long computeAgeMinutes(Object uowStartObj) {
        LocalDateTime start = null;
        if (uowStartObj instanceof Timestamp) {
            start = ((Timestamp) uowStartObj).toLocalDateTime();
        } else if (uowStartObj instanceof String) {
            String s = ((String) uowStartObj).trim();
            String[] patterns = {"yyyy-MM-dd HH:mm:ss.SSSSSS", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"};
            for (String p : patterns) {
                try {
                    start = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(p));
                    break;
                } catch (Exception ignore) {
                    // try next
                }
            }
        }
        if (start == null) {
            return -1L;
        }
        return Duration.between(start, LocalDateTime.now()).toMinutes();
    }
}
