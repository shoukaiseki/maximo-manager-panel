package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class AutoScriptService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询自动化脚本列表（支持查询模式/诊断模式）
     */
    public Map<String, Object> queryAutoScriptList(
            String autoscript, String description, String objectname,
            String attributename, String launchpointname, String source,
            String mode, int pageNum, int pageSize) {

        boolean isDiagMode = !"query".equalsIgnoreCase(mode);

        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (isDiagMode) {
            // 诊断模式
            if (objectname != null && !objectname.trim().isEmpty()) {
                String objUpper = objectname.trim().toUpperCase();
                whereSql.append(" WHERE (");
                // 条件1: 启动点 OBJECTNAME 匹配
                whereSql.append("EXISTS (SELECT 1 FROM SCRIPTLAUNCHPOINT sl WHERE sl.AUTOSCRIPT = a.AUTOSCRIPT AND sl.OBJECTNAME = ?)");
                params.add(objUpper);
                // 条件2: 脚本名为 <OBJECTNAME>.NEW
                whereSql.append(" OR a.AUTOSCRIPT = ?");
                params.add(objUpper + ".NEW");
                // 条件3: 如果 ATTRIBUTENAME 也有值，SOURCE 包含 ATTRIBUTENAME（CLOB 不支持 UPPER，直接用 LIKE）
                if (attributename != null && !attributename.trim().isEmpty()) {
                    whereSql.append(" OR a.SOURCE LIKE ?");
                    params.add("%" + attributename.trim() + "%");
                }
                whereSql.append(")");
            }
            // 追加其他过滤条件
            if (autoscript != null && !autoscript.trim().isEmpty()) {
                whereSql.append(whereSql.length() == 0 ? " WHERE " : " AND ");
                whereSql.append("a.AUTOSCRIPT LIKE ?");
                params.add("%" + autoscript.trim().toUpperCase() + "%");
            }
            if (description != null && !description.trim().isEmpty()) {
                whereSql.append(" AND ");
                whereSql.append("a.DESCRIPTION LIKE ?");
                params.add("%" + description.trim() + "%");
            }
            if (launchpointname != null && !launchpointname.trim().isEmpty()) {
                whereSql.append(" AND ");
                whereSql.append("EXISTS (SELECT 1 FROM SCRIPTLAUNCHPOINT sl2 WHERE sl2.AUTOSCRIPT = a.AUTOSCRIPT AND sl2.LAUNCHPOINTNAME LIKE ?)");
                params.add("%" + launchpointname.trim().toUpperCase() + "%");
            }
        } else {
            // 查询模式: 所有条件都要匹配
            whereSql.append(" WHERE 1=1");
            if (objectname != null && !objectname.trim().isEmpty()) {
                whereSql.append(" AND EXISTS (SELECT 1 FROM SCRIPTLAUNCHPOINT sl WHERE sl.AUTOSCRIPT = a.AUTOSCRIPT AND sl.OBJECTNAME = ?)");
                params.add(objectname.trim().toUpperCase());
            }
            if (attributename != null && !attributename.trim().isEmpty()) {
                whereSql.append(" AND EXISTS (SELECT 1 FROM SCRIPTLAUNCHPOINT sl3 WHERE sl3.AUTOSCRIPT = a.AUTOSCRIPT AND sl3.ATTRIBUTENAME = ?)");
                params.add(attributename.trim().toUpperCase());
            }
            if (autoscript != null && !autoscript.trim().isEmpty()) {
                whereSql.append(" AND a.AUTOSCRIPT LIKE ?");
                params.add("%" + autoscript.trim().toUpperCase() + "%");
            }
            if (description != null && !description.trim().isEmpty()) {
                whereSql.append(" AND a.DESCRIPTION LIKE ?");
                params.add("%" + description.trim() + "%");
            }
            if (launchpointname != null && !launchpointname.trim().isEmpty()) {
                whereSql.append(" AND EXISTS (SELECT 1 FROM SCRIPTLAUNCHPOINT sl4 WHERE sl4.AUTOSCRIPT = a.AUTOSCRIPT AND sl4.LAUNCHPOINTNAME LIKE ?)");
                params.add("%" + launchpointname.trim().toUpperCase() + "%");
            }
            if (source != null && !source.trim().isEmpty()) {
                whereSql.append(" AND a.SOURCE LIKE ?");
                params.add("%" + source.trim() + "%");
            }
        }

        String whereStr = whereSql.toString();

        // 总数查询
        String countSql = "SELECT COUNT(*) AS total FROM AUTOSCRIPT a" + whereStr;
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
            throw new RuntimeException("查询 AUTOSCRIPT 总数失败: " + e.getMessage(), e);
        }

        // 分页数据查询（不包含 SOURCE 大字段）
        String dataSql = "SELECT a.AUTOSCRIPT, a.DESCRIPTION, a.SCRIPTLANGUAGE, a.VERSION, " +
                "a.ACTIVE, a.USERDEFINED, a.LOGLEVEL, a.STATUS, a.OWNERID, a.OWNERNAME, " +
                "a.CREATEDBYID, a.CREATEDBYNAME, a.CREATEDDATE, a.CHANGEDATE, a.CHANGEBY, " +
                "a.COMMENTS, a.CATEGORY, a.INTERFACE, a.IBM_PACKAGEPATH " +
                "FROM AUTOSCRIPT a " +
                whereStr +
                " ORDER BY a.AUTOSCRIPT " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();
        // 诊断模式下需要在 Java 层做源码包含过滤（CLOB 不能在 SQL 中 UPPER）
        boolean needSourceFilter = isDiagMode && objectname != null && !objectname.trim().isEmpty()
                && attributename != null && !attributename.trim().isEmpty();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.setInt(params.size() + 1, offset);
            ps.setInt(params.size() + 2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = rowToMap(rs);
                    // 诊断模式下查询匹配类型
                    if (isDiagMode && objectname != null && !objectname.trim().isEmpty()) {
                        String scriptName = (String) row.get("AUTOSCRIPT");
                        List<String> matchTypes = new ArrayList<>();
                        String objUpper = objectname.trim().toUpperCase();

                        // 检查对象匹配
                        if (hasLaunchPointWithObject(conn, scriptName, objUpper)) {
                            matchTypes.add("对象匹配");
                        }
                        // 检查脚本名匹配
                        if ((objUpper + ".NEW").equals(scriptName)) {
                            matchTypes.add("脚本名匹配");
                        }
                        // 检查源码包含（Java 层读取 CLOB）
                        if (needSourceFilter) {
                            if (sourceContainsAttribute(conn, scriptName, attributename.trim())) {
                                matchTypes.add("源码包含");
                            }
                        }
                        row.put("_matchType", String.join(", ", matchTypes));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 AUTOSCRIPT 列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 检查脚本是否有匹配对象的启动点
     */
    private boolean hasLaunchPointWithObject(Connection conn, String autoscript, String objectname) {
        String sql = "SELECT 1 FROM SCRIPTLAUNCHPOINT WHERE AUTOSCRIPT = ? AND OBJECTNAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript);
            ps.setString(2, objectname);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 检查脚本源码是否包含指定属性名
     */
    private boolean sourceContainsAttribute(Connection conn, String autoscript, String attributename) {
        String sql = "SELECT SOURCE FROM AUTOSCRIPT WHERE AUTOSCRIPT = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String src = rs.getString("SOURCE");
                    return src != null && src.toUpperCase().contains(attributename.toUpperCase());
                }
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    /**
     * 查询脚本详情（主表全字段，排除 SOURCE/BINARYSCRIPTSOURCE）
     */
    public Map<String, Object> queryAutoScriptDetail(String autoscript) {
        String sql = "SELECT a.AUTOSCRIPT, a.DESCRIPTION, a.SCRIPTLANGUAGE, a.VERSION, " +
                "a.ACTIVE, a.USERDEFINED, a.LOGLEVEL, a.STATUS, a.SCHEDULEDSTATUS, " +
                "a.OWNERID, a.OWNERNAME, a.OWNERPHONE, a.OWNEREMAIL, " +
                "a.CREATEDBYID, a.CREATEDBYNAME, a.CREATEDBYPHONE, a.CREATEDBYEMAIL, " +
                "a.CREATEDBY, a.CREATEDDATE, a.CHANGEBY, a.CHANGEDATE, a.STATUSDATE, " +
                "a.COMMENTS, a.CATEGORY, a.INTERFACE, a.ORGID, a.SITEID, a.ACTION, " +
                "a.HASLD, a.LANGCODE, a.IBM_PACKAGEPATH, a.AUTOSCRIPTID " +
                "FROM AUTOSCRIPT a " +
                "WHERE a.AUTOSCRIPT = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询脚本详情失败: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    /**
     * 查询脚本源码
     */
    public Map<String, Object> queryAutoScriptSource(String autoscript) {
        String sql = "SELECT AUTOSCRIPT, SOURCE, SCRIPTLANGUAGE FROM AUTOSCRIPT WHERE AUTOSCRIPT = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询脚本源码失败: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    /**
     * 查询启动点列表（含 LAUNCHPOINTVARS）
     */
    public List<Map<String, Object>> queryLaunchPoints(String autoscript) {
        String sql = "SELECT sl.* " +
                "FROM SCRIPTLAUNCHPOINT sl " +
                "WHERE sl.AUTOSCRIPT = ? ORDER BY sl.LAUNCHPOINTNAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> lp = rowToMap(rs);
                    // 查询启动点变量
                    String lpName = (String) lp.get("LAUNCHPOINTNAME");
                    lp.put("_VARS", queryLaunchPointVars(conn, autoscript, lpName));
                    result.add(lp);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询启动点失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询启动点变量
     */
    private List<Map<String, Object>> queryLaunchPointVars(Connection conn, String autoscript, String launchpointname) {
        String sql = "SELECT * FROM LAUNCHPOINTVARS WHERE AUTOSCRIPT = ? AND LAUNCHPOINTNAME = ? ORDER BY VARNAME";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript.trim().toUpperCase());
            ps.setString(2, launchpointname);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            // 不抛出异常，返回空列表
        }
        return result;
    }

    /**
     * 查询脚本变量
     */
    public List<Map<String, Object>> queryAutoScriptVars(String autoscript) {
        String sql = "SELECT av.* " +
                "FROM AUTOSCRIPTVARS av " +
                "WHERE av.AUTOSCRIPT = ? ORDER BY av.VARTYPE, av.VARNAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, autoscript.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询脚本变量失败: " + e.getMessage(), e);
        }
        return result;
    }

    // ========== 工具方法 ==========

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String name = meta.getColumnLabel(i).toUpperCase();
            Object value = rs.getObject(i);
            // 处理 CLOB 类型：连接关闭前读取为字符串
            if (value instanceof java.sql.Clob) {
                java.sql.Clob clob = (java.sql.Clob) value;
                try (java.io.Reader reader = clob.getCharacterStream()) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = reader.read(buf)) != -1) {
                        sb.append(buf, 0, n);
                    }
                    value = sb.toString();
                } catch (java.io.IOException e) {
                    value = null;
                }
            }
            map.put(name, value);
        }
        return map;
    }
}
