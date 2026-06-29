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
     * 查询 MAXOBJECT 列表（支持分页 + objectname 精确/模糊搜索 + keyword 描述模糊搜索）
     */
    public Map<String, Object> queryMaxObjectList(String objectname, String keyword, int pageNum, int pageSize) {
        // 构建 objectname 搜索条件（支持 =精确 和 %模糊）
        LikeCondition objCond = buildLikeCondition(objectname, "OBJECTNAME");

        // 构建 keyword 描述模糊搜索
        String keywordPattern = "%" + (keyword != null ? keyword.trim().toUpperCase() : "") + "%";

        // 1. 总数查询
        StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) AS total FROM MAXOBJECT WHERE ");
        List<String> conditions = new ArrayList<>();
        List<String> params = new ArrayList<>();

        if (objCond != null) {
            conditions.add(objCond.getSql());
            params.add(objCond.getValue());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            conditions.add("MAXOBJECT.DESCRIPTION LIKE ?");
            params.add(keywordPattern);
        }

        // 如果没有任何条件，查询所有
        if (conditions.isEmpty()) {
            countSqlBuilder = new StringBuilder("SELECT COUNT(*) AS total FROM MAXOBJECT");
        } else {
            countSqlBuilder.append(String.join(" OR ", conditions));
        }

        int total = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(countSqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    total = rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXOBJECT 总数失败: " + e.getMessage(), e);
        }

        // 2. 分页数据查询（DB2 分页语法）
        StringBuilder dataSqlBuilder = new StringBuilder(
            "SELECT l.DESCRIPTION AS l_description, OBJECTNAME, MAXOBJECT.DESCRIPTION " +
            "FROM MAXOBJECT " +
            "LEFT JOIN L_MAXOBJECT AS l ON (MAXOBJECTID = l.OWNERID AND l.LANGCODE = 'ZH') ");

        if (conditions.isEmpty()) {
            dataSqlBuilder.append("ORDER BY OBJECTNAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        } else {
            dataSqlBuilder.append("WHERE ").append(String.join(" OR ", conditions))
                .append(" ORDER BY OBJECTNAME OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        }

        int offset = (pageNum - 1) * pageSize;
        List<MaxObjectInfo> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            ps.setInt(params.size() + 1, offset);
            ps.setInt(params.size() + 2, pageSize);
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

    /**
     * 查询对象的所有域及域值
     */
    public List<Map<String, Object>> queryDomains(String objectName) {
        // 1. 获取该对象所有属性中唯一的 DOMAINID
        String domainIdSql = "SELECT DISTINCT DOMAINID FROM MAXATTRIBUTE WHERE OBJECTNAME = ? AND DOMAINID IS NOT NULL";

        List<String> domainIds = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(domainIdSql)) {
            ps.setString(1, objectName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    domainIds.add(rs.getString("DOMAINID"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 DOMAINID 列表失败: " + e.getMessage(), e);
        }

        if (domainIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建 IN 子句查询 MAXDOMAIN
        List<Map<String, Object>> result = new ArrayList<>();
        String placeholders = String.join(",", Collections.nCopies(domainIds.size(), "?"));
        String domainSql = "SELECT mo.*, l.DESCRIPTION AS L_DESCRIPTION " +
                           "FROM MAXDOMAIN mo " +
                           "LEFT JOIN L_MAXDOMAIN AS l ON (mo.MAXDOMAINID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                           "WHERE mo.DOMAINID IN (" + placeholders + ") " +
                           "ORDER BY mo.DOMAINID";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(domainSql)) {
            for (int i = 0; i < domainIds.size(); i++) {
                ps.setString(i + 1, domainIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXDOMAIN 失败: " + e.getMessage(), e);
        }

        // 3. 查询域值
        for (Map<String, Object> domain : result) {
            String domainId = (String) domain.get("DOMAINID");
            String domainType = (String) domain.get("DOMAINTYPE");
            if (domainId == null || domainType == null) continue;

            List<Map<String, Object>> values = new ArrayList<>();
            String valueSql;
            String tableName;

            if ("NUMERIC".equalsIgnoreCase(domainType)) {
                tableName = "NUMERICDOMAIN";
                valueSql = "SELECT nd.VALUE, nd.DESCRIPTION, l.DESCRIPTION AS L_DESCRIPTION " +
                           "FROM " + tableName + " nd " +
                           "LEFT JOIN L_" + tableName + " AS l ON (nd." + tableName + "ID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                           "WHERE nd.DOMAINID = ? ORDER BY nd.VALUE";
            } else if ("ALN".equalsIgnoreCase(domainType)) {
                tableName = "ALNDOMAIN";
                valueSql = "SELECT nd.VALUE, nd.SITEID, nd.ORGID, nd.DESCRIPTION, l.DESCRIPTION AS L_DESCRIPTION " +
                           "FROM " + tableName + " nd " +
                           "LEFT JOIN L_" + tableName + " AS l ON (nd." + tableName + "ID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                           "WHERE nd.DOMAINID = ? ORDER BY nd.VALUE";
            } else if ("SYNONYM".equalsIgnoreCase(domainType)) {
                tableName = "SYNONYMDOMAIN";
                valueSql = "SELECT nd.VALUE, nd.MAXVALUE, nd.SITEID, nd.ORGID, nd.DEFAULTS, nd.DESCRIPTION, l.DESCRIPTION AS L_DESCRIPTION " +
                           "FROM " + tableName + " nd " +
                           "LEFT JOIN L_" + tableName + " AS l ON (nd." + tableName + "ID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                           "WHERE nd.DOMAINID = ? ORDER BY nd.VALUE";
            } else {
                continue;
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(valueSql)) {
                ps.setString(1, domainId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        values.add(rowToMap(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("查询域值失败: " + e.getMessage(), e);
            }

            // 将域值合并到域对象中
            StringBuilder valueSummary = new StringBuilder();
            for (Map<String, Object> v : values) {
                Object val = v.get("VALUE");
                Object desc = v.get("L_DESCRIPTION");
                if (desc == null || String.valueOf(desc).trim().isEmpty()) {
                    desc = v.get("DESCRIPTION");
                }
                if (valueSummary.length() > 0) valueSummary.append(", ");
                valueSummary.append(val != null ? val : "");
                if (desc != null && !String.valueOf(desc).trim().isEmpty()) {
                    valueSummary.append("(").append(desc).append(")");
                }
            }
            domain.put("_VALUES_SUMMARY", valueSummary.toString());
            domain.put("_VALUES", values);
            domain.put("_VALUES_COUNT", values.size());
        }

        return result;
    }

    // ========== 工具方法 ==========

    /**
     * 查询 MAXATTRIBUTE 列表（支持分页 + 多条件搜索）
     * 支持精确匹配（以=开头）和通配符（%）搜索
     */
    public Map<String, Object> queryMaxAttributeList(String objectname, String attributename, String description,
                                                      int pageNum, int pageSize) {
        List<Object> params = new ArrayList<>();
        StringBuilder whereSql = new StringBuilder("WHERE 1=1 ");

        // 对象名条件
        LikeCondition objCond = buildLikeCondition(objectname, "ma.OBJECTNAME");
        if (objCond != null) {
            whereSql.append("AND ").append(objCond.getSql());
            params.add(objCond.getValue());
        }

        // 属性名条件
        LikeCondition attrCond = buildLikeCondition(attributename, "ma.ATTRIBUTENAME");
        if (attrCond != null) {
            whereSql.append("AND ").append(attrCond.getSql());
            params.add(attrCond.getValue());
        }

        // 描述条件（搜索 L_TITLE 和 TITLE）
        String desc = description != null ? description.trim() : "";
        if (!desc.isEmpty()) {
            if (desc.startsWith("=")) {
                String exactVal = desc.substring(1).trim();
                whereSql.append("AND (l.TITLE = ? OR ma.TITLE = ?) ");
                params.add(exactVal);
                params.add(exactVal);
            } else if (desc.contains("%")) {
                whereSql.append("AND (l.TITLE LIKE ? OR ma.TITLE LIKE ?) ");
                params.add(desc.toUpperCase());
                params.add(desc.toUpperCase());
            } else {
                String pattern = "%" + desc.toUpperCase() + "%";
                whereSql.append("AND (l.TITLE LIKE ? OR ma.TITLE LIKE ?) ");
                params.add(pattern);
                params.add(pattern);
            }
        }

        // 1. 总数查询
        String countSql = "SELECT COUNT(*) AS total FROM MAXATTRIBUTE ma " +
                          "LEFT JOIN L_MAXATTRIBUTE AS l ON (ma.MAXATTRIBUTEID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                          whereSql;

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
            throw new RuntimeException("查询 MAXATTRIBUTE 总数失败: " + e.getMessage(), e);
        }

        // 2. 分页数据查询（DB2 分页语法）
        String dataSql = "SELECT ma.*, l.TITLE AS L_TITLE, l.REMARKS AS L_REMARKS " +
                         "FROM MAXATTRIBUTE ma " +
                         "LEFT JOIN L_MAXATTRIBUTE AS l ON (ma.MAXATTRIBUTEID = l.OWNERID AND l.LANGCODE = 'ZH') " +
                         whereSql +
                         "ORDER BY ma.OBJECTNAME, ma.ATTRIBUTENAME " +
                         "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            int paramIdx = 1;
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(paramIdx++, params.get(i));
            }
            ps.setInt(paramIdx++, offset);
            ps.setInt(paramIdx++, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXATTRIBUTE 列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 构建 LIKE 条件，支持精确匹配（=开头）和通配符（%）
     */
    private LikeCondition buildLikeCondition(String input, String columnName) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        String val = input.trim();
        if (val.startsWith("=")) {
            String exactVal = val.substring(1).trim().toUpperCase();
            return new LikeCondition(columnName + " = ?", exactVal);
        } else if (val.contains("%")) {
            return new LikeCondition(columnName + " LIKE ?", val.toUpperCase());
        } else {
            String pattern = "%" + val.toUpperCase() + "%";
            return new LikeCondition(columnName + " LIKE ?", pattern);
        }
    }

    /**
     * LIKE 条件辅助类
     */
    private static class LikeCondition {
        private final String sql;
        private final String value;

        LikeCondition(String sql, String value) {
            this.sql = sql;
            this.value = value;
        }

        String getSql() {
            return sql;
        }

        String getValue() {
            return value;
        }
    }

    private Integer getInt(ResultSet rs, String column) throws SQLException {
        int val = rs.getInt(column);
        return rs.wasNull() ? null : val;
    }

    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String name = meta.getColumnLabel(i).toUpperCase();
            Object value = rs.getObject(i);
            map.put(name, value);
        }
        return map;
    }
}