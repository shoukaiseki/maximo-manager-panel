package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 保存的查询管理（MySQL，参考 Maximo QUERY 表结构，通过 app 区分应用）
 */
@Component
public class SavedQueryService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    /**
     * 查询某应用下的保存查询列表
     */
    public List<Map<String, Object>> listQueries(String app) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, app, queryname, whereclause, description, created_at, updated_at " +
                "FROM saved_query WHERE app = ? ORDER BY queryname";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("app", rs.getString("app"));
                    row.put("queryname", rs.getString("queryname"));
                    row.put("whereclause", rs.getString("whereclause"));
                    row.put("description", rs.getString("description"));
                    row.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null);
                    row.put("updatedAt", rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : null);
                    list.add(row);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询保存的查询列表失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 获取单个保存查询
     */
    public Map<String, Object> getQuery(Long id) {
        String sql = "SELECT id, app, queryname, whereclause, description FROM saved_query WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("app", rs.getString("app"));
                    row.put("queryname", rs.getString("queryname"));
                    row.put("whereclause", rs.getString("whereclause"));
                    row.put("description", rs.getString("description"));
                    return row;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询保存的查询详情失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 保存查询：存在(app+queryname)则更新，否则插入
     */
    public Map<String, Object> saveQuery(String app, String queryname, String whereclause, String description) {
        if (app == null || app.trim().isEmpty()) {
            throw new RuntimeException("应用名称不能为空");
        }
        if (queryname == null || queryname.trim().isEmpty()) {
            throw new RuntimeException("查询名称不能为空");
        }
        if (whereclause == null || whereclause.trim().isEmpty()) {
            throw new RuntimeException("WHERE子句不能为空");
        }
        String appTrim = app.trim();
        String nameTrim = queryname.trim();
        String whereTrim = whereclause.trim();
        String desc = description == null ? "" : description.trim();

        Long existingId = findByName(appTrim, nameTrim);
        if (existingId != null) {
            String sql = "UPDATE saved_query SET whereclause = ?, description = ? WHERE id = ?";
            try (Connection conn = mysqlDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, whereTrim);
                ps.setString(2, desc);
                ps.setLong(3, existingId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("更新保存的查询失败: " + e.getMessage(), e);
            }
            return getQuery(existingId);
        }

        String sql = "INSERT INTO saved_query (app, queryname, whereclause, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, appTrim);
            ps.setString(2, nameTrim);
            ps.setString(3, whereTrim);
            ps.setString(4, desc);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getQuery(keys.getLong(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("保存查询失败: " + e.getMessage(), e);
        }
        return null;
    }

    private Long findByName(String app, String queryname) {
        String sql = "SELECT id FROM saved_query WHERE app = ? AND queryname = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app);
            ps.setString(2, queryname);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询保存的查询失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除保存的查询
     */
    public boolean deleteQuery(Long id) {
        String sql = "DELETE FROM saved_query WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除保存的查询失败: " + e.getMessage(), e);
        }
    }
}
