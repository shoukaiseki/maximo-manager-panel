package com.sks.server.service;

import com.sks.server.model.*;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class MaxObjectService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询 MAXOBJECT 列表（支持分页 + objectname/description 模糊搜索）
     */
    public Map<String, Object> queryMaxObjectList(String keyword, int pageNum, int pageSize) {
        String pattern = "%" + (keyword != null ? keyword.trim().toUpperCase() : "") + "%";

        // 1. 总数查询
        String countSql = "SELECT COUNT(*) AS total FROM MAXOBJECT " +
                          "WHERE OBJECTNAME LIKE ? OR MAXOBJECT.DESCRIPTION LIKE ?";

        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXOBJECT 总数失败: " + e.getMessage(), e);
        }

        // 2. 分页数据查询（DB2 分页语法）
        String dataSql = "SELECT l.DESCRIPTION AS l_description, OBJECTNAME, MAXOBJECT.DESCRIPTION " +
                         "FROM MAXOBJECT " +
                         "LEFT JOIN L_MAXOBJECT AS l ON (MAXOBJECTID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                         "WHERE OBJECTNAME LIKE ? OR MAXOBJECT.DESCRIPTION LIKE ? " +
                         "ORDER BY OBJECTNAME " +
                         "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<MaxObjectInfo> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, offset);
            ps.setInt(4, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MaxObjectInfo obj = new MaxObjectInfo();
                    obj.setObjectName(rs.getString("OBJECTNAME"));
                    obj.setDescription(rs.getString("DESCRIPTION"));
                    obj.setDescriptionCn(rs.getString("l_description"));
                    rows.add(obj);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXOBJECT 列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 查询 MAXOBJECT 主信息
     */
    public Map<String, Object> queryMaxObjectMain(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = "SELECT mo.*, l.DESCRIPTION AS L_DESCRIPTION, l.LANGCODE AS L_LANGCODE, l.OWNERID AS L_OWNERID, " +
                     "l.L_MAXOBJECTID AS L_MAXOBJECTID, l.ROWSTAMP AS L_ROWSTAMP " +
                     "FROM MAXOBJECT mo " +
                     "LEFT JOIN L_MAXOBJECT AS l ON (mo.MAXOBJECTID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                     "WHERE mo.OBJECTNAME = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, objectName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rowToMap(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXOBJECT 详情失败: " + e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    /**
     * 查询对象的所有属性（返回所有字段）
     */
    public List<Map<String, Object>> queryAttributes(String objectName) {
        String sql = "SELECT ma.*, l.TITLE L_TITLE, l.REMARKS L_REMARKS " +
                     "FROM MAXATTRIBUTE ma " +
                     "LEFT JOIN L_MAXATTRIBUTE AS l ON (ma.MAXATTRIBUTEID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                     "WHERE ma.OBJECTNAME = ? " +
                     "ORDER BY ma.ATTRIBUTENAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, objectName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询对象属性失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询对象的所有关系（返回所有字段）
     */
    public List<Map<String, Object>> queryRelationships(String parentObject) {
        String sql = "SELECT * FROM MAXRELATIONSHIP " +
                     "WHERE PARENT = ? " +
                     "ORDER BY NAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentObject.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询对象关系失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询对象的所有索引（返回所有字段 + 索引列）
     */
    public List<Map<String, Object>> queryIndexes(String objectName) {
        // 1. 先查 MAXSYSINDEXES 获取索引列表
        String indexSql = "SELECT * FROM MAXSYSINDEXES WHERE TBNAME = ?";

        // 获取实体表名（如果是视图则不查询索引）
        Map<String, Object> mainInfo = queryMaxObjectMain(objectName);
        String entityName = mainInfo != null ? (String) mainInfo.get("ENTITYNAME") : null;
        if (entityName == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(indexSql)) {
            ps.setString(1, entityName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> idx = rowToMap(rs);
                    // 查询索引列，存为逗号分隔字符串
                    String ixName = rs.getString("NAME");
                    idx.put("_COLUMNS", queryIndexColumns(conn, ixName));
                    result.add(idx);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询对象索引失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询索引的列
     */
    private List<String> queryIndexColumns(Connection conn, String ixName) throws SQLException {
        String sql = "SELECT COLNAME FROM MAXSYSKEYS WHERE IXNAME = ? ORDER BY COLSEQ";
        List<String> columns = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ixName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString("COLNAME"));
                }
            }
        }
        return columns;
    }

    /**
     * 查询对象的完整详情
     */
    public MaxObjectDetail queryFullDetail(String objectName) {
        MaxObjectDetail detail = new MaxObjectDetail();
        detail.setMainInfo(queryMaxObjectMain(objectName));
        detail.setAttributes(queryAttributes(objectName));
        detail.setRelationships(queryRelationships(objectName));
        detail.setIndexes(queryIndexes(objectName));
        return detail;
    }

    /**
     * 查询对象的摘要详情（主信息 + 属性）
     */
    public Map<String, Object> queryDetail(String objectName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mainInfo", queryMaxObjectMain(objectName));
        result.put("attributes", queryAttributes(objectName));
        return result;
    }

    // ========== 工具方法 ==========

    private Integer getInt(ResultSet rs, String column) throws SQLException {
        int val = rs.getInt(column);
        return rs.wasNull() ? null : val;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String name = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            map.put(name, value);
        }
        return map;
    }
}