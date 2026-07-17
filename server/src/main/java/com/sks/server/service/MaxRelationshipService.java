package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class MaxRelationshipService {

    @Inject
    private DataSource dataSource;

    public Map<String, Object> queryList(String name, String parent, String child, String keyword, int pageNum, int pageSize) {
        LikeCondition nameCond = buildLikeCondition(name, "NAME");
        LikeCondition parentCond = buildLikeCondition(parent, "PARENT");
        LikeCondition childCond = buildLikeCondition(child, "CHILD");

        StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) AS total FROM MAXRELATIONSHIP mr LEFT JOIN MAXOBJECT mo ON mr.PARENT = mo.OBJECTNAME WHERE 1=1");
        StringBuilder dataSqlBuilder = new StringBuilder("SELECT mr.*, mo.DESCRIPTION AS PARENT_DESC, l.DESCRIPTION AS PARENT_DESC_CN FROM MAXRELATIONSHIP mr LEFT JOIN MAXOBJECT mo ON mr.PARENT = mo.OBJECTNAME LEFT JOIN L_MAXOBJECT l ON mo.MAXOBJECTID = l.OWNERID AND l.LANGCODE = 'ZH' WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (nameCond != null) {
            countSqlBuilder.append(" AND ").append(nameCond.getSql());
            dataSqlBuilder.append(" AND ").append(nameCond.getSql());
            params.add(nameCond.getValue());
        }
        if (parentCond != null) {
            countSqlBuilder.append(" AND ").append(parentCond.getSql());
            dataSqlBuilder.append(" AND ").append(parentCond.getSql());
            params.add(parentCond.getValue());
        }
        if (childCond != null) {
            countSqlBuilder.append(" AND ").append(childCond.getSql());
            dataSqlBuilder.append(" AND ").append(childCond.getSql());
            params.add(childCond.getValue());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kwPattern = "%" + keyword.trim().toUpperCase() + "%";
            countSqlBuilder.append(" AND (UPPER(mr.NAME) LIKE ? OR UPPER(mr.PARENT) LIKE ? OR UPPER(mr.CHILD) LIKE ? OR UPPER(mr.WHERECLAUSE) LIKE ? OR UPPER(mr.REMARKS) LIKE ?)");
            dataSqlBuilder.append(" AND (UPPER(mr.NAME) LIKE ? OR UPPER(mr.PARENT) LIKE ? OR UPPER(mr.CHILD) LIKE ? OR UPPER(mr.WHERECLAUSE) LIKE ? OR UPPER(mr.REMARKS) LIKE ?)");
            for (int i = 0; i < 5; i++) {
                params.add(kwPattern);
            }
        }

        dataSqlBuilder.append(" ORDER BY mr.PARENT, mr.NAME");
        dataSqlBuilder.append(" LIMIT ? OFFSET ?");
        params.add(String.valueOf(pageSize));
        params.add(String.valueOf((pageNum - 1) * pageSize));

        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSqlBuilder.toString())) {
            for (int i = 0; i < params.size() - 2; i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询关系总数失败: " + e.getMessage(), e);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                if (i == params.size() - 2 || i == params.size() - 1) {
                    ps.setInt(i + 1, Integer.parseInt(params.get(i)));
                } else {
                    ps.setString(i + 1, params.get(i));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询关系列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public Map<String, Object> getById(Long id) {
        String sql = "SELECT * FROM MAXRELATIONSHIP WHERE ID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询关系详情失败: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Map<String, Object>> queryAll() {
        String sql = "SELECT * FROM MAXRELATIONSHIP ORDER BY PARENT, NAME";
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询所有关系失败: " + e.getMessage(), e);
        }
        return result;
    }

    public List<Map<String, Object>> queryRelationshipsByParent(String parent) {
        String sql = "SELECT * FROM MAXRELATIONSHIP WHERE PARENT = ? ORDER BY NAME";
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parent.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询关系失败: " + e.getMessage(), e);
        }
        return result;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String colName = metaData.getColumnName(i);
            map.put(colName, rs.getObject(i));
        }
        return map;
    }

    private LikeCondition buildLikeCondition(String value, String column) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("=")) {
            String exactValue = trimmed.substring(1).trim().toUpperCase();
            return new LikeCondition(column + " = ?", exactValue);
        } else {
            String likeValue = "%" + trimmed.toUpperCase() + "%";
            return new LikeCondition("UPPER(" + column + ") LIKE ?", likeValue);
        }
    }

    private static class LikeCondition {
        private final String sql;
        private final String value;

        public LikeCondition(String sql, String value) {
            this.sql = sql;
            this.value = value;
        }

        public String getSql() {
            return sql;
        }

        public String getValue() {
            return value;
        }
    }
}