package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class QueryDbService {

    @Inject
    private DataSource dataSource;

    public List<Map<String, Object>> getSearchFields() {
        String sql = "select distinct ENTITYNAME, COLUMNNAME, MAXTYPE " +
                     "from MAXATTRIBUTE as ma " +
                     "where MAXTYPE in ('ALN','UPPER') " +
                     "and not exists(select 1 from MAXVIEW where ma.OBJECTNAME=VIEWNAME) " +
                     "order by ENTITYNAME, COLUMNNAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ENTITYNAME", rs.getString("ENTITYNAME"));
                row.put("COLUMNNAME", rs.getString("COLUMNNAME"));
                row.put("MAXTYPE", rs.getString("MAXTYPE"));
                result.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取搜索字段列表失败: " + e.getMessage(), e);
        }
        return result;
    }

    public interface SearchCallback {
        void onProgress(int current, int total);
        void onResult(Map<String, Object> result);
        void onCompleted(long totalTime);
        void onError(String error);
    }

    public void searchData(String keyword, boolean exactMatch, SearchCallback callback) {
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> fields = getSearchFields();
        int total = fields.size();
        
        try {
            callback.onProgress(0, total);
            
            for (int i = 0; i < total; i++) {
                Map<String, Object> field = fields.get(i);
                String entityName = (String) field.get("ENTITYNAME");
                String columnName = (String) field.get("COLUMNNAME");
                String maxType = (String) field.get("MAXTYPE");
                
                Map<String, Object> result = searchInField(entityName, columnName, maxType, keyword, exactMatch);
                if ((Integer) result.get("COUNT") > 0) {
                    callback.onResult(result);
                }
                
                callback.onProgress(i + 1, total);
            }
            long totalTime = System.currentTimeMillis() - startTime;
            callback.onCompleted(totalTime);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    public Map<String, Object> searchInField(String entityName, String columnName, String maxType, 
                                              String keyword, boolean exactMatch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ENTITYNAME", entityName);
        result.put("COLUMNNAME", columnName);
        result.put("MAXTYPE", maxType);

        String querySql;
        if (exactMatch) {
            querySql = String.format("SELECT COUNT(1) AS cnt FROM %s WHERE UPPER(%s) = UPPER(?)", 
                                    entityName, columnName);
        } else {
            querySql = String.format("SELECT COUNT(1) AS cnt FROM %s WHERE UPPER(%s) LIKE UPPER(?)", 
                                    entityName, columnName);
        }

        String displaySql;
        if (exactMatch) {
            displaySql = String.format("SELECT COUNT(1) FROM %s WHERE %s = '%s'", 
                                      entityName, columnName, keyword);
        } else {
            displaySql = String.format("SELECT COUNT(1) FROM %s WHERE %s LIKE '%%%s%%'", 
                                      entityName, columnName, keyword);
        }
        result.put("QUERY_SQL", displaySql);

        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            if (exactMatch) {
                ps.setString(1, keyword);
            } else {
                ps.setString(1, "%" + keyword + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            result.put("COUNT", 0);
            result.put("ERROR", e.getMessage());
            result.put("TIME_MS", System.currentTimeMillis() - startTime);
            return result;
        }

        long timeMs = System.currentTimeMillis() - startTime;
        result.put("COUNT", count);
        result.put("TIME_MS", timeMs);

        return result;
    }
}
