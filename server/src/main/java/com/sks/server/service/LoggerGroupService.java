package com.sks.server.service;

import com.sks.server.model.LoggerLevelGroupItem;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 日志级别分组管理（MySQL）
 * 管理用户自建分组及其条目（各组独立存 level/ignored）；默认分组由 LoggerLevelService 负责。
 */
@Component
public class LoggerGroupService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    private static final Set<String> VALID_LEVELS = new HashSet<>(
            Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));

    // ============ 分组 ============

    /**
     * 查询全部分组（含条目数）
     */
    public List<Map<String, Object>> listGroups() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT g.id, g.name, g.description, g.created_at, g.updated_at, " +
                "(SELECT COUNT(*) FROM logger_level_group_item i WHERE i.group_id = g.id) AS item_count " +
                "FROM logger_level_group g ORDER BY g.id ASC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapGroupRow(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("查询日志级别分组失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 查询单个分组
     */
    public Map<String, Object> getGroup(Long id) {
        String sql = "SELECT g.id, g.name, g.description, g.created_at, g.updated_at, " +
                "(SELECT COUNT(*) FROM logger_level_group_item i WHERE i.group_id = g.id) AS item_count " +
                "FROM logger_level_group g WHERE g.id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapGroupRow(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询分组失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 新建分组
     */
    public Map<String, Object> createGroup(String name, String description) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        String d = description == null ? "" : description.trim();
        String sql = "INSERT INTO logger_level_group (name, description) VALUES (?, ?)";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n);
            ps.setString(2, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getGroup(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                throw new RuntimeException("分组名称已存在: " + n);
            }
            throw new RuntimeException("新建分组失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 更新分组
     */
    public Map<String, Object> updateGroup(Long id, String name, String description) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new RuntimeException("分组名称不能为空");
        }
        String d = description == null ? "" : description.trim();
        String sql = "UPDATE logger_level_group SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n);
            ps.setString(2, d);
            ps.setLong(3, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("分组不存在: id=" + id);
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                throw new RuntimeException("分组名称已存在: " + n);
            }
            throw new RuntimeException("更新分组失败: " + e.getMessage(), e);
        }
        return getGroup(id);
    }

    /**
     * 删除分组（事务：先删条目再删分组）
     */
    public boolean deleteGroup(Long id) {
        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM logger_level_group_item WHERE group_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM logger_level_group WHERE id = ?")) {
                    ps.setLong(1, id);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        throw new RuntimeException("分组不存在: id=" + id);
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("删除分组失败: " + e.getMessage(), e);
        }
        return true;
    }

    // ============ 分组条目 ============

    /**
     * 查询分组条目
     */
    public List<Map<String, Object>> listItems(Long groupId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, group_id, logger_name, log_level, ignored, description, sort_order, created_at, updated_at " +
                "FROM logger_level_group_item WHERE group_id = ? ORDER BY sort_order ASC, logger_name ASC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapItemRow(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询分组条目失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 全量保存分组条目（覆盖式 upsert，对齐 LoggerLevelService.saveConfigs）
     */
    public List<Map<String, Object>> saveItems(Long groupId, List<LoggerLevelGroupItem> items) {
        if (groupId == null) {
            throw new RuntimeException("groupId 不能为空");
        }
        if (!groupExists(groupId)) {
            throw new RuntimeException("分组不存在: id=" + groupId);
        }
        if (items == null) {
            items = Collections.emptyList();
        }

        // 规整 + 校验 + 组内去重
        List<LoggerLevelGroupItem> valid = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (LoggerLevelGroupItem c : items) {
            String name = c.getLoggerName() == null ? "" : c.getLoggerName().trim();
            if (name.isEmpty()) {
                continue;
            }
            String level = c.getLevel() == null ? "INFO" : c.getLevel().trim().toUpperCase();
            if (!VALID_LEVELS.contains(level)) {
                throw new RuntimeException("不支持的日志级别: " + c.getLevel() + " (logger: " + name + ")");
            }
            if (!seen.add(name)) {
                throw new RuntimeException("日志器名称重复: " + name);
            }
            LoggerLevelGroupItem n = new LoggerLevelGroupItem();
            n.setLoggerName(name);
            n.setLevel(level);
            n.setIgnored(Boolean.TRUE.equals(c.getIgnored()));
            n.setDescription(c.getDescription() == null ? "" : c.getDescription().trim());
            valid.add(n);
        }

        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 删除不在提交列表中的行
                if (valid.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM logger_level_group_item WHERE group_id = ?")) {
                        ps.setLong(1, groupId);
                        ps.executeUpdate();
                    }
                } else {
                    String placeholders = String.join(",", Collections.nCopies(valid.size(), "?"));
                    String delSql = "DELETE FROM logger_level_group_item WHERE group_id = ? AND logger_name NOT IN (" + placeholders + ")";
                    try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                        int idx = 1;
                        ps.setLong(idx++, groupId);
                        for (LoggerLevelGroupItem c : valid) {
                            ps.setString(idx++, c.getLoggerName());
                        }
                        ps.executeUpdate();
                    }
                }

                // upsert
                String upsertSql = "INSERT INTO logger_level_group_item (group_id, logger_name, log_level, ignored, description, sort_order) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE log_level = VALUES(log_level), ignored = VALUES(ignored), " +
                        "description = VALUES(description), sort_order = VALUES(sort_order)";
                try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
                    int order = 0;
                    for (LoggerLevelGroupItem c : valid) {
                        ps.setLong(1, groupId);
                        ps.setString(2, c.getLoggerName());
                        ps.setString(3, c.getLevel());
                        ps.setInt(4, Boolean.TRUE.equals(c.getIgnored()) ? 1 : 0);
                        ps.setString(5, c.getDescription());
                        ps.setInt(6, c.getSortOrder() != null && c.getSortOrder() != 0 ? c.getSortOrder() : order++);
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
            throw new RuntimeException("保存分组条目失败: " + e.getMessage(), e);
        }

        return listItems(groupId);
    }

    /**
     * 删除单条条目
     */
    public boolean deleteItem(Long id) {
        String sql = "DELETE FROM logger_level_group_item WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除分组条目失败: " + e.getMessage(), e);
        }
    }

    /**
     * 增量添加日志器到分组（已存在的跳过，默认 level=INFO）
     */
    public Map<String, Object> addItems(Long groupId, List<String> loggerNames) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (groupId == null) {
            throw new RuntimeException("groupId 不能为空");
        }
        if (!groupExists(groupId)) {
            throw new RuntimeException("分组不存在: id=" + groupId);
        }
        if (loggerNames == null) {
            loggerNames = Collections.emptyList();
        }

        // 规整 + 去空 + 去重
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String s : loggerNames) {
            String n = s == null ? "" : s.trim();
            if (!n.isEmpty()) {
                names.add(n);
            }
        }

        if (names.isEmpty()) {
            result.put("added", 0);
            result.put("skipped", 0);
            result.put("items", listItems(groupId));
            return result;
        }

        try (Connection conn = mysqlDataSource.getConnection()) {
            // 查组内已存在
            Set<String> existing = new HashSet<>();
            String querySql = "SELECT logger_name FROM logger_level_group_item WHERE group_id = ? AND logger_name IN (" +
                    String.join(",", Collections.nCopies(names.size(), "?")) + ")";
            try (PreparedStatement ps = conn.prepareStatement(querySql)) {
                int idx = 1;
                ps.setLong(idx++, groupId);
                for (String name : names) {
                    ps.setString(idx++, name);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        existing.add(rs.getString("logger_name"));
                    }
                }
            }

            List<String> toInsert = new ArrayList<>();
            for (String name : names) {
                if (!existing.contains(name)) {
                    toInsert.add(name);
                }
            }

            if (!toInsert.isEmpty()) {
                // 添加时级别取默认配置(logger_level_config)中的级别，缺省 INFO
                Map<String, String> defaultLevels = queryDefaultLevels(toInsert);
                conn.setAutoCommit(false);
                try {
                    String insertSql = "INSERT INTO logger_level_group_item (group_id, logger_name, log_level, ignored, description, sort_order) " +
                            "VALUES (?, ?, ?, 0, '', ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        int order = 0;
                        for (String name : toInsert) {
                            ps.setLong(1, groupId);
                            ps.setString(2, name);
                            ps.setString(3, defaultLevels.getOrDefault(name, "INFO"));
                            ps.setInt(4, order++);
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
            result.put("skipped", names.size() - toInsert.size());
            result.put("items", listItems(groupId));
        } catch (Exception e) {
            throw new RuntimeException("添加日志器到分组失败: " + e.getMessage(), e);
        }
        return result;
    }

    // ============ 工具 ============

    /**
     * 查询默认配置表(logger_level_config)中各日志器的级别（仅返回入参中存在的名称）
     */
    private Map<String, String> queryDefaultLevels(List<String> names) {
        Map<String, String> result = new HashMap<>();
        if (names == null || names.isEmpty()) {
            return result;
        }
        String sql = "SELECT logger_name, log_level FROM logger_level_config WHERE logger_name IN (" +
                String.join(",", Collections.nCopies(names.size(), "?")) + ")";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String name : names) {
                ps.setString(idx++, name);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("logger_name"), rs.getString("log_level"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询默认配置级别失败: " + e.getMessage(), e);
        }
        return result;
    }

    private boolean groupExists(Long groupId) {
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM logger_level_group WHERE id = ?")) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException("校验分组存在失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mapGroupRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("name", rs.getString("name"));
        row.put("description", rs.getString("description"));
        row.put("itemCount", rs.getInt("item_count"));
        row.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null);
        row.put("updatedAt", rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : null);
        return row;
    }

    private Map<String, Object> mapItemRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", rs.getLong("id"));
        row.put("groupId", rs.getLong("group_id"));
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
