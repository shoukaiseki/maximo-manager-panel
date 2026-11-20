package com.sks.server.service;

import com.sks.server.model.CondDebugRecord;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 条件表达式调试记录管理（MySQL）
 */
@Component
public class CondDebugService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    private static final String COLS = "id, record_name, mbo_name, mbo_where, cond_type, parser_type, expression, fields, max_rows, description, created_at, updated_at";

    /**
     * 查询调试记录列表（按更新时间倒序，keyword 匹配名称/对象/表达式）
     */
    public List<CondDebugRecord> listRecords(String keyword) {
        List<CondDebugRecord> list = new ArrayList<>();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String sql = "SELECT " + COLS + " FROM cond_debug_record"
                + (hasKeyword ? " WHERE record_name LIKE ? OR mbo_name LIKE ? OR expression LIKE ?" : "")
                + " ORDER BY updated_at DESC";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasKeyword) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询调试记录列表失败: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * 获取单条调试记录
     */
    public CondDebugRecord getRecord(Long id) {
        String sql = "SELECT " + COLS + " FROM cond_debug_record WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询调试记录失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 保存调试记录：按 record_name 匹配，存在则更新，否则新增
     */
    public CondDebugRecord saveRecord(CondDebugRecord req) {
        if (req.getRecordName() == null || req.getRecordName().trim().isEmpty()) {
            throw new RuntimeException("记录名称不能为空");
        }
        String name = req.getRecordName().trim();
        String condType = req.getCondType() == null || req.getCondType().trim().isEmpty() ? "WORKFLOW" : req.getCondType().trim();
        String parserType = req.getParserType() == null || req.getParserType().trim().isEmpty() ? "SYSTEM" : req.getParserType().trim();
        int maxRows = req.getMaxRows() == null || req.getMaxRows() <= 0 ? 10 : req.getMaxRows();
        String desc = req.getDescription() == null ? "" : req.getDescription().trim();

        Long existingId = findByName(name);
        if (existingId != null) {
            String sql = "UPDATE cond_debug_record SET mbo_name = ?, mbo_where = ?, cond_type = ?, parser_type = ?, expression = ?, fields = ?, max_rows = ?, description = ? WHERE id = ?";
            try (Connection conn = mysqlDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, req.getMboName());
                ps.setString(2, req.getMboWhere());
                ps.setString(3, condType);
                ps.setString(4, parserType);
                ps.setString(5, req.getExpression());
                ps.setString(6, req.getFields());
                ps.setInt(7, maxRows);
                ps.setString(8, desc);
                ps.setLong(9, existingId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("更新调试记录失败: " + e.getMessage(), e);
            }
            return getRecord(existingId);
        }

        String sql = "INSERT INTO cond_debug_record (record_name, mbo_name, mbo_where, cond_type, parser_type, expression, fields, max_rows, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, req.getMboName());
            ps.setString(3, req.getMboWhere());
            ps.setString(4, condType);
            ps.setString(5, parserType);
            ps.setString(6, req.getExpression());
            ps.setString(7, req.getFields());
            ps.setInt(8, maxRows);
            ps.setString(9, desc);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return getRecord(keys.getLong(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("保存调试记录失败: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 删除调试记录
     */
    public boolean deleteRecord(Long id) {
        String sql = "DELETE FROM cond_debug_record WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除调试记录失败: " + e.getMessage(), e);
        }
    }

    private Long findByName(String recordName) {
        String sql = "SELECT id FROM cond_debug_record WHERE record_name = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recordName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询调试记录失败: " + e.getMessage(), e);
        }
        return null;
    }

    private CondDebugRecord mapRow(ResultSet rs) throws SQLException {
        CondDebugRecord r = new CondDebugRecord();
        r.setId(rs.getLong("id"));
        r.setRecordName(rs.getString("record_name"));
        r.setMboName(rs.getString("mbo_name"));
        r.setMboWhere(rs.getString("mbo_where"));
        r.setCondType(rs.getString("cond_type"));
        r.setParserType(rs.getString("parser_type"));
        r.setExpression(rs.getString("expression"));
        r.setFields(rs.getString("fields"));
        r.setMaxRows(rs.getInt("max_rows"));
        r.setDescription(rs.getString("description"));
        r.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : null);
        r.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toString() : null);
        return r;
    }
}
