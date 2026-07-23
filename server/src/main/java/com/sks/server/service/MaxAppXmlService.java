package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class MaxAppXmlService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询 MAXPRESENTATION 列表（支持分页 + APP/描述/内容模糊搜索）
     */
    public Map<String, Object> queryList(String app, String description, String content,
                                          boolean contentCaseSensitive,
                                          int pageNum, int pageSize) {
        StringBuilder whereSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        whereSql.append(" WHERE 1=1");

        if (app != null && !app.trim().isEmpty()) {
            String appStr = app.trim();
            if (appStr.startsWith("=")) {
                whereSql.append(" AND p.APP = ?");
                params.add(appStr.substring(1).toUpperCase());
            } else {
                whereSql.append(" AND p.APP LIKE ?");
                params.add("%" + appStr.toUpperCase() + "%");
            }
        }

        // description 搜索（同时搜索中文描述和英文描述）
        if (description != null && !description.trim().isEmpty()) {
            String descStr = "%" + description.trim().toUpperCase() + "%";
            whereSql.append(" AND (UPPER(l_zh.DESCRIPTION) LIKE ? OR UPPER(a.DESCRIPTION) LIKE ?)");
            params.add(descStr);
            params.add(descStr);
        }

        // content 区分大小写时在 SQL 中用 LIKE 过滤（快）
        boolean needJavaFilter = content != null && !content.trim().isEmpty() && !contentCaseSensitive;
        if (content != null && !content.trim().isEmpty() && contentCaseSensitive) {
            whereSql.append(" AND p.PRESENTATION LIKE ?");
            params.add("%" + content.trim() + "%");
        }

        String whereStr = whereSql.toString();

        // 总数查询
        String countSql = "SELECT COUNT(*) AS total FROM MAXPRESENTATION p " +
                "LEFT JOIN MAXAPPS a ON p.APP = a.APP " +
                "LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'" +
                whereStr;
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
            throw new RuntimeException("查询 MAXPRESENTATION 总数失败: " + e.getMessage(), e);
        }

        // 分页数据查询（不包含 PRESENTATION 大字段）
        String dataSql = "SELECT p.APP, l_zh.DESCRIPTION AS L_DESCRIPTION, a.DESCRIPTION, p.MAXPRESENTATIONID " +
                "FROM MAXPRESENTATION p " +
                "LEFT JOIN MAXAPPS a ON p.APP = a.APP " +
                "LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'" +
                whereStr +
                " ORDER BY p.APP " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();

        if (needJavaFilter) {
            // Step 1: 查询所有匹配的 APP 列表
            String nameSql = "SELECT p.APP FROM MAXPRESENTATION p " +
                    "LEFT JOIN MAXAPPS a ON p.APP = a.APP " +
                    "LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'" +
                    whereStr +
                    " ORDER BY p.APP";
            List<String> allApps = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(nameSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allApps.add(rs.getString("APP"));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("查询 APP 名称列表失败: " + e.getMessage(), e);
            }

            // Step 2: 读取 PRESENTATION 内容做不区分大小写过滤
            String srcKeyword = content.trim().replace("%", "");
            List<String> filteredApps = new ArrayList<>();
            for (String appName : allApps) {
                String src = getPresentationContent(appName);
                if (src != null && src.toLowerCase().contains(srcKeyword.toLowerCase())) {
                    filteredApps.add(appName);
                }
            }

            total = filteredApps.size();

            // Step 3: 分页
            int fromIndex = Math.min(offset, filteredApps.size());
            int toIndex = Math.min(offset + pageSize, filteredApps.size());
            List<String> pageApps = filteredApps.subList(fromIndex, toIndex);

            // Step 4: 查询分页后的完整数据
            if (!pageApps.isEmpty()) {
                StringJoiner sj = new StringJoiner(",");
                for (int i = 0; i < pageApps.size(); i++) sj.add("?");
                String fullSql = "SELECT p.APP, l_zh.DESCRIPTION AS L_DESCRIPTION, a.DESCRIPTION, p.MAXPRESENTATIONID " +
                        "FROM MAXPRESENTATION p " +
                        "LEFT JOIN MAXAPPS a ON p.APP = a.APP " +
                        "LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'" +
                        " WHERE p.APP IN (" + sj.toString() + ") ORDER BY p.APP";
                try (Connection conn = dataSource.getConnection();
                     PreparedStatement ps = conn.prepareStatement(fullSql)) {
                    for (int i = 0; i < pageApps.size(); i++) {
                        ps.setString(i + 1, pageApps.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            rows.add(rowToMap(rs));
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("查询分页数据失败: " + e.getMessage(), e);
                }
            }
        } else {
            // 普通分页查询
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
                throw new RuntimeException("查询 MAXPRESENTATION 列表失败: " + e.getMessage(), e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取 PRESENTATION 源码内容
     */
    public String getSource(String app) {
        String sql = "SELECT PRESENTATION FROM MAXPRESENTATION WHERE APP = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Clob clob = rs.getClob("PRESENTATION");
                    if (clob != null) {
                        return clob.getSubString(1, (int) clob.length());
                    }
                    return rs.getString("PRESENTATION");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取 PRESENTATION 源码失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取某条记录的完整字段信息（用于详情显示）
     */
    public Map<String, Object> getDetail(String app, Long presentationId) {
        String sql = "SELECT p.APP, p.PRESENTATION, p.MAXPRESENTATIONID, " +
                "a.DESCRIPTION, l_zh.DESCRIPTION AS L_DESCRIPTION " +
                "FROM MAXPRESENTATION p " +
                "LEFT JOIN MAXAPPS a ON p.APP = a.APP " +
                "LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'";
        if (presentationId != null && presentationId > 0) {
            sql += " WHERE p.MAXPRESENTATIONID = ?";
        } else if (app != null && !app.trim().isEmpty()) {
            sql += " WHERE p.APP = ?";
        } else {
            return null;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (presentationId != null && presentationId > 0) {
                ps.setLong(1, presentationId);
            } else {
                ps.setString(1, app);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("APP", rs.getString("APP"));
                    row.put("MAXPRESENTATIONID", rs.getLong("MAXPRESENTATIONID"));
                    row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
                    row.put("L_DESCRIPTION", rs.getString("L_DESCRIPTION"));
                    Clob clob = rs.getClob("PRESENTATION");
                    if (clob != null) {
                        row.put("PRESENTATION", clob.getSubString(1, (int) clob.length()));
                    } else {
                        row.put("PRESENTATION", rs.getString("PRESENTATION"));
                    }
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXPRESENTATION 详情失败: " + e.getMessage(), e);
        }
        return null;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("APP", rs.getString("APP"));
        row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
        row.put("L_DESCRIPTION", rs.getString("L_DESCRIPTION"));
        row.put("MAXPRESENTATIONID", rs.getLong("MAXPRESENTATIONID"));
        return row;
    }

    private String getPresentationContent(String app) {
        String sql = "SELECT PRESENTATION FROM MAXPRESENTATION WHERE APP = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Clob clob = rs.getClob("PRESENTATION");
                    if (clob != null) {
                        return clob.getSubString(1, (int) clob.length());
                    }
                    return rs.getString("PRESENTATION");
                }
            }
        } catch (SQLException e) {
            // ignore
        }
        return null;
    }
}
