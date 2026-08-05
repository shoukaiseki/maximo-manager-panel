package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
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

        whereSql.append(" WHERE lh.TABSCHEMA = 'MAXIMO'");
        if (tabName != null && !tabName.trim().isEmpty()) {
            String v = tabName.trim();
            if (v.startsWith("=")) {
                whereSql.append(" AND lh.TABNAME = ?");
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
}
