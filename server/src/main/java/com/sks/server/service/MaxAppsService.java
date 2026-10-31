package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * MAXAPPS 应用查询
 */
@Component
public class MaxAppsService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询 MAXAPPS 列表（支持分页 + APP/描述/内容模糊搜索）
     * tab: all=所有应用；xml=有 XML 的应用（存在 MAXPRESENTATION）；mafapp=MAFAPP（APPTYPE in ('WC')）
     */
    public Map<String, Object> queryList(String app, String description, String content,
                                         boolean contentCaseSensitive, String tab,
                                         int pageNum, int pageSize) {
        String mode = (tab == null || tab.trim().isEmpty()) ? "all" : tab.trim().toLowerCase();

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (app != null && !app.trim().isEmpty()) {
            String appStr = app.trim();
            if (appStr.startsWith("=")) {
                whereSql.append(" AND a.APP = ?");
                params.add(appStr.substring(1).toUpperCase());
            } else {
                whereSql.append(" AND a.APP LIKE ?");
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

        if ("mafapp".equals(mode)) {
            whereSql.append(" AND a.APPTYPE IN ('WC')");
        } else if ("xml".equals(mode)) {
            whereSql.append(" AND EXISTS (SELECT 1 FROM MAXPRESENTATION xp WHERE xp.APP = a.APP)");
        }

        boolean hasContent = content != null && !content.trim().isEmpty();
        // content 区分大小写时在 SQL 中用 LIKE 过滤（快）
        boolean needJavaFilter = hasContent && !contentCaseSensitive;
        if (hasContent && contentCaseSensitive) {
            whereSql.append(" AND EXISTS (SELECT 1 FROM MAXPRESENTATION xp WHERE xp.APP = a.APP AND xp.PRESENTATION LIKE ?)");
            params.add("%" + content.trim() + "%");
        }

        String whereStr = whereSql.toString();
        String baseSql = " FROM MAXAPPS a" +
                " LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'";

        // 总数查询
        String countSql = "SELECT COUNT(*) AS total" + baseSql + whereStr;
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
            throw new RuntimeException("查询 MAXAPPS 总数失败: " + e.getMessage(), e);
        }

        // 分页数据查询（不包含 PRESENTATION 大字段）
        String dataSql = "SELECT a.APP, a.APPTYPE, l_zh.DESCRIPTION AS L_DESCRIPTION, a.DESCRIPTION," +
                " (SELECT MAX(p.MAXPRESENTATIONID) FROM MAXPRESENTATION p WHERE p.APP = a.APP) AS MAXPRESENTATIONID" +
                baseSql + whereStr +
                " ORDER BY a.APP" +
                " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();

        if (needJavaFilter) {
            // Step 1: 查询所有匹配的 APP 列表
            String nameSql = "SELECT a.APP" + baseSql + whereStr + " ORDER BY a.APP";
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
                String fullSql = "SELECT a.APP, a.APPTYPE, l_zh.DESCRIPTION AS L_DESCRIPTION, a.DESCRIPTION," +
                        " (SELECT MAX(p.MAXPRESENTATIONID) FROM MAXPRESENTATION p WHERE p.APP = a.APP) AS MAXPRESENTATIONID" +
                        baseSql + " WHERE a.APP IN (" + sj + ") ORDER BY a.APP";
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
                throw new RuntimeException("查询 MAXAPPS 列表失败: " + e.getMessage(), e);
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
     * 查询单个应用的 MAXAPPS 完整信息（含中文描述、PRESENTATION ID）
     */
    public Map<String, Object> getDetail(String app) {
        if (app == null || app.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT a.*, l_zh.DESCRIPTION AS L_DESCRIPTION," +
                " (SELECT MAX(p.MAXPRESENTATIONID) FROM MAXPRESENTATION p WHERE p.APP = a.APP) AS MAXPRESENTATIONID" +
                " FROM MAXAPPS a" +
                " LEFT JOIN L_MAXAPPS l_zh ON l_zh.OWNERID = a.MAXAPPSID AND l_zh.LANGCODE = 'ZH'" +
                " WHERE a.APP = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String column = meta.getColumnLabel(i);
                        if ("ROWSTAMP".equalsIgnoreCase(column)) continue;
                        row.put(column, rs.getObject(i));
                    }
                    return row;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXAPPS 详情失败: " + e.getMessage(), e);
        }
        return null;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("APP", rs.getString("APP"));
        row.put("APPTYPE", rs.getString("APPTYPE"));
        row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
        row.put("L_DESCRIPTION", rs.getString("L_DESCRIPTION"));
        Object presentationId = rs.getObject("MAXPRESENTATIONID");
        row.put("MAXPRESENTATIONID", presentationId == null ? null : rs.getLong("MAXPRESENTATIONID"));
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
