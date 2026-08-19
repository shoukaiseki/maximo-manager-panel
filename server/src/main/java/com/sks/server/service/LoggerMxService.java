package com.sks.server.service;

import com.sks.server.model.LoggerMxConfig;
import com.sks.server.model.LoggerMxGroup;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * MXLogger 日志管理配置（MySQL）
 * 树结构（最多一层子级）以 parent_id 扁平化存储，list 时组装；logkey 为派生值不落库。
 */
@Component
public class LoggerMxService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    private static final Set<String> VALID_LEVELS = new HashSet<>(
            Arrays.asList("DEBUG", "INFO", "WARN", "ERROR"));

    /**
     * 查询 MXLogger 配置（组装为树：顶层 → 子级，最多一层）
     * @param groupId 组ID；null 查询所有（含旧数据 group_id IS NULL）
     */
    public List<LoggerMxConfig> listTree(Long groupId) {
        List<LoggerMxConfig> all = new ArrayList<>();
        String base = "SELECT id, group_id, parent_id, logger, log_level, active, remark, sort_order " +
                "FROM logger_mx_config ";
        String sql;
        if (groupId == null) {
            sql = base + "ORDER BY parent_id IS NOT NULL ASC, sort_order ASC, id ASC";
        } else {
            sql = base + "WHERE group_id = ? " +
                    "ORDER BY parent_id IS NOT NULL ASC, sort_order ASC, id ASC";
        }
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (groupId != null) {
                ps.setLong(1, groupId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoggerMxConfig c = new LoggerMxConfig();
                    c.setId(rs.getLong("id"));
                    long gid = rs.getLong("group_id");
                    c.setGroupId(rs.wasNull() ? null : gid);
                    long pid = rs.getLong("parent_id");
                    c.setParentId(rs.wasNull() ? null : pid);
                    c.setLogger(rs.getString("logger"));
                    c.setLevel(rs.getString("log_level"));
                    c.setActive(rs.getInt("active") != 0);
                    c.setRemark(rs.getString("remark"));
                    c.setSortOrder(rs.getInt("sort_order"));
                    all.add(c);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询MXLogger配置失败: " + e.getMessage(), e);
        }

        // 组装树（parent_id 指向的父必须在列表内；找不到的当作顶层）
        List<LoggerMxConfig> roots = new ArrayList<>();
        Map<Long, LoggerMxConfig> byId = new HashMap<>();
        for (LoggerMxConfig c : all) {
            byId.put(c.getId(), c);
        }
        List<LoggerMxConfig> orphans = new ArrayList<>();
        for (LoggerMxConfig c : all) {
            if (c.getParentId() != null && byId.containsKey(c.getParentId())) {
                byId.get(c.getParentId()).addChild(c);
            } else {
                c.setParentId(null);
                orphans.add(c);
            }
        }
        // 有独立子级的节点在前（有 children 视为组），简单起见保持查询顺序
        roots.addAll(orphans);
        return roots;
    }

    /**
     * 保存指定组的树：清空该组旧数据 → 插入新树（顶层先插，子级再用生成的 id 关联 parent_id）。
     * @param loggers 顶层节点（含 children）
     * @param groupId 组ID；null 为旧模式（保存到 group_id IS NULL）
     */
    public List<LoggerMxConfig> saveTree(List<LoggerMxConfig> loggers, Long groupId) {
        if (loggers == null) {
            loggers = Collections.emptyList();
        }
        // 校验
        List<LoggerMxConfig> tops = new ArrayList<>();
        for (LoggerMxConfig t : loggers) {
            String name = t.getLogger() == null ? "" : t.getLogger().trim();
            if (name.isEmpty()) {
                continue;
            }
            String lv = normalizeLevel(t.getLevel(), false);
            if (lv.isEmpty()) {
                throw new RuntimeException("不支持的日志级别: " + t.getLevel() + " (logger: " + name + ")");
            }
            t.setLogger(name);
            t.setLevel(lv);
            t.setActive(Boolean.TRUE.equals(t.getActive()));
            // 校验子级
            if (t.getChildren() != null) {
                for (LoggerMxConfig ch : t.getChildren()) {
                    String cn = ch.getLogger() == null ? "" : ch.getLogger().trim();
                    if (cn.isEmpty()) {
                        throw new RuntimeException("顶级 " + name + " 的子级缺少日志器名称");
                    }
                    String clv = normalizeLevel(ch.getLevel(), false);
                    if (clv.isEmpty()) {
                        throw new RuntimeException("不支持的日志级别: " + ch.getLevel() + " (child: " + cn + ")");
                    }
                    ch.setLogger(cn);
                    ch.setLevel(clv);
                    ch.setActive(Boolean.TRUE.equals(ch.getActive()));
                }
            }
            tops.add(t);
        }

        String insertSql = "INSERT INTO logger_mx_config (group_id, parent_id, logger, log_level, active, remark, sort_order) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (groupId == null) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate("DELETE FROM logger_mx_config WHERE group_id IS NULL");
                    }
                } else {
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM logger_mx_config WHERE group_id = ?")) {
                        del.setLong(1, groupId);
                        del.executeUpdate();
                    }
                }
                int order = 0;
                for (LoggerMxConfig t : tops) {
                    long topId;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        if (groupId == null) {
                            ps.setNull(1, Types.BIGINT);
                        } else {
                            ps.setLong(1, groupId);
                        }
                        ps.setNull(2, Types.BIGINT);
                        ps.setString(3, t.getLogger());
                        ps.setString(4, t.getLevel());
                        ps.setInt(5, Boolean.TRUE.equals(t.getActive()) ? 1 : 0);
                        ps.setString(6, t.getRemark() == null ? "" : t.getRemark().trim());
                        ps.setInt(7, t.getSortOrder() != null ? t.getSortOrder() : order);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            keys.next();
                            topId = keys.getLong(1);
                        }
                    }
                    if (t.getChildren() != null) {
                        int cOrder = 0;
                        for (LoggerMxConfig ch : t.getChildren()) {
                            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                                if (groupId == null) {
                                    ps.setNull(1, Types.BIGINT);
                                } else {
                                    ps.setLong(1, groupId);
                                }
                                ps.setLong(2, topId);
                                ps.setString(3, ch.getLogger());
                                ps.setString(4, ch.getLevel());
                                ps.setInt(5, Boolean.TRUE.equals(ch.getActive()) ? 1 : 0);
                                ps.setString(6, ch.getRemark() == null ? "" : ch.getRemark().trim());
                                ps.setInt(7, ch.getSortOrder() != null ? ch.getSortOrder() : cOrder++);
                                ps.executeUpdate();
                            }
                        }
                    }
                    order++;
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("保存MXLogger配置失败: " + e.getMessage(), e);
        }
        return listTree(groupId);
    }

    private String normalizeLevel(String level, boolean allowEmpty) {
        if (level == null) {
            return allowEmpty ? "" : "ERROR";
        }
        String s = level.trim().toUpperCase();
        if (s.isEmpty()) {
            return allowEmpty ? "" : "ERROR";
        }
        return VALID_LEVELS.contains(s) ? s : "";
    }

    // ==================== 组管理 ====================

    /**
     * 查询全部 MXLogger 组（按 id 排序）
     */
    public List<LoggerMxGroup> listGroups() {
        List<LoggerMxGroup> groups = new ArrayList<>();
        String sql = "SELECT id, name, description FROM logger_mx_group ORDER BY id ASC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                LoggerMxGroup g = new LoggerMxGroup();
                g.setId(rs.getLong("id"));
                g.setName(rs.getString("name"));
                g.setDescription(rs.getString("description"));
                groups.add(g);
            }
        } catch (Exception e) {
            throw new RuntimeException("查询MXLogger组失败: " + e.getMessage(), e);
        }
        return groups;
    }

    /**
     * 创建组（名称去重）
     */
    public LoggerMxGroup createGroup(String name, String description) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new RuntimeException("组名称不能为空");
        }
        String sql = "INSERT INTO logger_mx_group (name, description) VALUES (?, ?)";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n);
            ps.setString(2, description == null ? "" : description.trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                LoggerMxGroup g = new LoggerMxGroup();
                g.setId(keys.getLong(1));
                g.setName(n);
                g.setDescription(description == null ? "" : description.trim());
                return g;
            }
        } catch (Exception e) {
            throw new RuntimeException("创建MXLogger组失败: " + e.getMessage(), e);
        }
    }

    /**
     * 重命名/更新组描述
     */
    public LoggerMxGroup updateGroup(Long id, String name, String description) {
        if (id == null) {
            throw new RuntimeException("组ID不能为空");
        }
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new RuntimeException("组名称不能为空");
        }
        String sql = "UPDATE logger_mx_group SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n);
            ps.setString(2, description == null ? "" : description.trim());
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("更新MXLogger组失败: " + e.getMessage(), e);
        }
        LoggerMxGroup g = new LoggerMxGroup();
        g.setId(id);
        g.setName(n);
        g.setDescription(description == null ? "" : description.trim());
        return g;
    }

    /**
     * 删除组（级联删除其下所有配置节点）
     */
    public void deleteGroup(Long id) {
        if (id == null) {
            throw new RuntimeException("组ID不能为空");
        }
        try (Connection conn = mysqlDataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delItems = conn.prepareStatement("DELETE FROM logger_mx_config WHERE group_id = ?");
                 PreparedStatement delGroup = conn.prepareStatement("DELETE FROM logger_mx_group WHERE id = ?")) {
                delItems.setLong(1, id);
                delItems.executeUpdate();
                delGroup.setLong(1, id);
                delGroup.executeUpdate();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("删除MXLogger组失败: " + e.getMessage(), e);
        }
    }
}