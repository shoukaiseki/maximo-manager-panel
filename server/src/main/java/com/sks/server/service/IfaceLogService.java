package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class IfaceLogService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询 IBM_IFACELOG 列表（支持分页 + 多字段模糊搜索）
     */
    public Map<String, Object> queryList(String app, String ownerTable, String status,
                                          String description, String extSystem, String ifaceStatus,
                                          int pageNum, int pageSize) {
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        whereSql.append(" WHERE 1=1");

        if (app != null && !app.trim().isEmpty()) {
            String v = app.trim();
            if (v.startsWith("=")) {
                whereSql.append(" AND APP = ?");
                params.add(v.substring(1));
            } else {
                whereSql.append(" AND UPPER(APP) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }
        if (ownerTable != null && !ownerTable.trim().isEmpty()) {
            String v = ownerTable.trim();
            if (v.startsWith("=")) {
                whereSql.append(" AND OWNERTABLE = ?");
                params.add(v.substring(1));
            } else {
                whereSql.append(" AND UPPER(OWNERTABLE) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }
        if (status != null && !status.trim().isEmpty()) {
            String v = status.trim();
            if (v.startsWith("=")) {
                whereSql.append(" AND STATUS = ?");
                params.add(v.substring(1));
            } else {
                whereSql.append(" AND UPPER(STATUS) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }
        if (description != null && !description.trim().isEmpty()) {
            String v = "%" + description.trim().toUpperCase() + "%";
            whereSql.append(" AND UPPER(DESCRIPTION) LIKE ?");
            params.add(v);
        }
        if (extSystem != null && !extSystem.trim().isEmpty()) {
            String v = "%" + extSystem.trim().toUpperCase() + "%";
            whereSql.append(" AND UPPER(EXTSYSTEM) LIKE ?");
            params.add(v);
        }
        if (ifaceStatus != null && !ifaceStatus.trim().isEmpty()) {
            String v = "%" + ifaceStatus.trim().toUpperCase() + "%";
            whereSql.append(" AND UPPER(IFACESTATUS) LIKE ?");
            params.add(v);
        }

        String whereStr = whereSql.toString();

        // 总数
        String countSql = "SELECT COUNT(*) AS total FROM IBM_IFACELOG" + whereStr;
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
            throw new RuntimeException("查询 IBM_IFACELOG 总数失败: " + e.getMessage(), e);
        }

        // 分页数据
        String dataSql = "SELECT IBM_IFACELOGID, DESCRIPTION, OWNERTABLE, STATUS, " +
                "CHANGEBY, CHANGEDATE, MEMO, ORGID, SITEID, OWNERID, APP, EXTSYSTEM, IFACESTATUS " +
                "FROM IBM_IFACELOG" + whereStr +
                " ORDER BY CHANGEDATE DESC, IBM_IFACELOGID DESC " +
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
            throw new RuntimeException("查询 IBM_IFACELOG 列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取详情（含 CLOB 内容）
     */
    public Map<String, Object> getDetail(Long logId) {
        String sql = "SELECT IBM_IFACELOGID, DESCRIPTION, OWNERTABLE, STATUS, " +
                "CHANGEBY, CHANGEDATE, MEMO, ORGID, SITEID, OWNERID, APP, EXTSYSTEM, IFACESTATUS, " +
                "REQBODY, RESPBODY " +
                "FROM IBM_IFACELOG WHERE IBM_IFACELOGID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, logId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("IBM_IFACELOGID", rs.getLong("IBM_IFACELOGID"));
                    row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
                    row.put("OWNERTABLE", rs.getString("OWNERTABLE"));
                    row.put("STATUS", rs.getString("STATUS"));
                    row.put("CHANGEBY", rs.getString("CHANGEBY"));
                    row.put("CHANGEDATE", rs.getTimestamp("CHANGEDATE"));
                    row.put("MEMO", rs.getString("MEMO"));
                    row.put("ORGID", rs.getString("ORGID"));
                    row.put("SITEID", rs.getString("SITEID"));
                    row.put("OWNERID", rs.getLong("OWNERID"));
                    row.put("APP", rs.getString("APP"));
                    row.put("EXTSYSTEM", rs.getString("EXTSYSTEM"));
                    row.put("IFACESTATUS", rs.getString("IFACESTATUS"));
                    row.put("REQBODY", getClobString(rs, "REQBODY"));
                    row.put("RESPBODY", getClobString(rs, "RESPBODY"));
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 IBM_IFACELOG 详情失败: " + e.getMessage(), e);
        }
        return null;
    }

    private String getClobString(ResultSet rs, String col) throws SQLException {
        Clob clob = rs.getClob(col);
        if (clob != null) {
            long len = clob.length();
            if (len > 0) {
                return clob.getSubString(1, (int) Math.min(len, Integer.MAX_VALUE));
            }
        }
        return rs.getString(col);
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("IBM_IFACELOGID", rs.getLong("IBM_IFACELOGID"));
        row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
        row.put("OWNERTABLE", rs.getString("OWNERTABLE"));
        row.put("STATUS", rs.getString("STATUS"));
        row.put("CHANGEBY", rs.getString("CHANGEBY"));
        row.put("CHANGEDATE", rs.getTimestamp("CHANGEDATE"));
        row.put("MEMO", rs.getString("MEMO"));
        row.put("ORGID", rs.getString("ORGID"));
        row.put("SITEID", rs.getString("SITEID"));
        row.put("OWNERID", rs.getLong("OWNERID"));
        row.put("APP", rs.getString("APP"));
        row.put("EXTSYSTEM", rs.getString("EXTSYSTEM"));
        row.put("IFACESTATUS", rs.getString("IFACESTATUS"));
        return row;
    }
}
