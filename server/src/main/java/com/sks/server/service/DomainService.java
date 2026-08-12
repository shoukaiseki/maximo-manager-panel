package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * 域（MAXDOMAIN）子表信息查询
 */
@Component
public class DomainService {

    @Inject
    private DataSource dataSource;

    /**
     * 根据域类型获取对应的类型化子表名
     */
    private String tableForType(String domaintype) {
        if (domaintype == null) return null;
        switch (domaintype.trim().toUpperCase()) {
            case "ALN": return "ALNDOMAIN";
            case "SYNONYM": return "SYNONYMDOMAIN";
            case "NUMERIC": return "NUMERICDOMAIN";
            case "NUMRANGE": return "NUMRANGEDOMAIN";
            case "CROSSOVER": return "CROSSOVERDOMAIN";
            case "TABLE": return "MAXTABLEDOMAIN";
            default: return null;
        }
    }

    /**
     * 查询域的域值子表 + 域值多语言 + 本地化描述
     * @param domainid 域ID
     * @param domaintype 域类型
     */
    public Map<String, Object> querySubtables(String domainid, String domaintype) {
        if (domainid == null || domainid.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        String did = domainid.trim();

        // 1. 域值（类型化子表）
        String table = tableForType(domaintype);
        List<Map<String, Object>> values = new ArrayList<>();
        if (table != null) {
            String orderBy;
            switch (table) {
                case "NUMRANGEDOMAIN": orderBy = "RANGESEGMENT"; break;
                case "CROSSOVERDOMAIN": orderBy = "SEQUENCE"; break;
                case "MAXTABLEDOMAIN": orderBy = "MAXTABLEDOMAINID"; break;
                default: orderBy = "VALUE";
            }
            values = queryRows("SELECT * FROM " + table + " WHERE DOMAINID = ? ORDER BY " + orderBy, did);
            // 域值多语言（L_<域值表名>，按 OWNERID 关联子表主键）
            attachValueTranslations(values, table);
        }
        result.put("values", values);
        result.put("valueTable", table);

        // 2. 本地化描述（L_MAXDOMAIN 关联主表）
        String transSql = "SELECT l.LANGCODE, l.DESCRIPTION FROM L_MAXDOMAIN l " +
                "JOIN MAXDOMAIN m ON l.OWNERID = m.MAXDOMAINID WHERE m.DOMAINID = ? ORDER BY l.LANGCODE";
        result.put("translations", queryRows(transSql, did));

        return result;
    }

    /**
     * 域值对应的多语言表（L_<域值表名>），无多语言表的类型返回 null
     */
    private String langTableFor(String table) {
        if (table == null) return null;
        switch (table) {
            case "ALNDOMAIN": return "L_ALNDOMAIN";
            case "SYNONYMDOMAIN": return "L_SYNONYMDOMAIN";
            case "NUMERICDOMAIN": return "L_NUMERICDOMAIN";
            default: return null;
        }
    }

    /**
     * 域值子表主键（多语言表 OWNERID 关联字段）
     */
    private String pkFor(String table) {
        if (table == null) return null;
        switch (table) {
            case "ALNDOMAIN": return "ALNDOMAINID";
            case "SYNONYMDOMAIN": return "SYNONYMDOMAINID";
            case "NUMERICDOMAIN": return "NUMERICDOMAINID";
            default: return null;
        }
    }

    /**
     * 为每条域值附加多语言记录字段 _TRANSLATIONS（[{LANGCODE, DESCRIPTION}]）
     */
    private void attachValueTranslations(List<Map<String, Object>> values, String table) {
        String langTable = langTableFor(table);
        String pk = pkFor(table);
        if (langTable == null || pk == null || values.isEmpty()) return;

        // 收集所有主键值
        Set<Object> ids = new LinkedHashSet<>();
        for (Map<String, Object> v : values) {
            Object id = v.get(pk);
            if (id != null) ids.add(id);
        }
        if (ids.isEmpty()) return;

        // 查询多语言
        StringJoiner placeholders = new StringJoiner(",");
        for (int i = 0; i < ids.size(); i++) placeholders.add("?");
        String sql = "SELECT OWNERID, LANGCODE, DESCRIPTION FROM " + langTable +
                " WHERE OWNERID IN (" + placeholders + ") ORDER BY OWNERID, LANGCODE";

        Map<Object, List<Map<String, Object>>> transMap = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Object id : ids) ps.setObject(idx++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("LANGCODE", rs.getString("LANGCODE"));
                    t.put("DESCRIPTION", rs.getString("DESCRIPTION"));
                    Object ownerId = rs.getObject("OWNERID");
                    transMap.computeIfAbsent(ownerId, k -> new ArrayList<>()).add(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询域值多语言失败: " + e.getMessage(), e);
        }

        // 合并到每条域值（子表展示用）
        for (Map<String, Object> v : values) {
            List<Map<String, Object>> list = transMap.get(v.get(pk));
            if (list == null || list.isEmpty()) continue;
            v.put("_TRANSLATIONS", list);
        }
    }

    private List<Map<String, Object>> queryRows(String sql, String param) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询域子表失败: " + e.getMessage(), e);
        }
        return rows;
    }
}
