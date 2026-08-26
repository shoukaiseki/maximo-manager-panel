package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * 工单库存余量分析服务
 * 基于 WPMATERIAL 统计未完工工单的物料需求/当前库存余量/预约数量
 */
@Component
public class WpMaterialStockService {

    @Inject
    private DataSource dataSource;

    /**
     * 基础查询 SQL（任务给定）：
     * WPMATERIAL 按 站点/工单/物料/位置 分组，关联 INVBALANCES 求当前库存余量，
     * 关联 INVRESERVE 求预约数量；仅统计未完工工单（排除 CAN/CLOSE/HISTEDIT 状态）
     */
    private static final String BASE_SQL =
            "SELECT t1.siteid, t1.wonum, t1.LOCATION, t1.itemnum, item.DESCRIPTION AS DESCRIPTION, t1.itemqty, t1.curbal, t1.RESERVEDQTY " +
            "FROM (" +
            "  SELECT siteid, wonum, itemnum, LOCATION, SUM(ITEMQTY) AS itemqty, " +
            "    (SELECT SUM(INVBALANCES.CURBAL) FROM INVBALANCES " +
            "      WHERE INVBALANCES.ITEMNUM = WPMATERIAL.ITEMNUM AND INVBALANCES.LOCATION = WPMATERIAL.LOCATION) AS curbal, " +
            "    COALESCE((SELECT SUM(INVRESERVE.RESERVEDQTY) FROM INVRESERVE " +
            "      WHERE INVRESERVE.siteid = WPMATERIAL.siteid " +
            "      AND INVRESERVE.ITEMNUM = WPMATERIAL.ITEMNUM " +
            "      AND INVRESERVE.WONUM != WPMATERIAL.WONUM), 0) AS RESERVEDQTY " +
            "  FROM WPMATERIAL " +
            "  WHERE EXISTS (SELECT 1 FROM WORKORDER " +
            "    WHERE WORKORDER.SITEID = WPMATERIAL.SITEID AND WORKORDER.WONUM = WPMATERIAL.WONUM " +
            "    AND WORKORDER.status NOT IN (SELECT VALUE FROM SYNONYMDOMAIN WHERE MAXVALUE IN ('CAN','CLOSE','HISTEDIT') AND DOMAINID = 'WOSTATUS')) " +
            "  GROUP BY siteid, wonum, itemnum, LOCATION" +
            ") t1 " +
            "LEFT JOIN item ON (item.itemnum = t1.itemnum)";

    /**
     * 分页查询工单库存余量
     * @param wonum    工单号过滤（=开头精确匹配，其余模糊）
     * @param itemnum  物料号过滤（=开头精确匹配，其余模糊）
     */
    public Map<String, Object> queryList(String wonum, String itemnum, int pageNum, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (wonum != null && !wonum.trim().isEmpty()) {
            String v = wonum.trim();
            if (v.startsWith("=")) {
                where.append(" AND RTRIM(t1.wonum) = ?");
                params.add(v.substring(1));
            } else {
                where.append(" AND UPPER(t1.wonum) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }
        if (itemnum != null && !itemnum.trim().isEmpty()) {
            String v = itemnum.trim();
            if (v.startsWith("=")) {
                where.append(" AND RTRIM(t1.itemnum) = ?");
                params.add(v.substring(1));
            } else {
                where.append(" AND UPPER(t1.itemnum) LIKE ?");
                params.add("%" + v.toUpperCase() + "%");
            }
        }

        // 总数
        String countSql = "SELECT COUNT(*) AS total FROM (" + BASE_SQL + ") t1" + where;
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
            throw new RuntimeException("查询工单库存余量总数失败: " + e.getMessage(), e);
        }

        // 分页数据
        String dataSql = "SELECT t1.siteid, t1.wonum, t1.LOCATION, t1.itemnum, t1.DESCRIPTION, t1.itemqty, t1.curbal, t1.RESERVEDQTY " +
                "FROM (" + BASE_SQL + ") t1" + where +
                " ORDER BY t1.wonum, t1.itemnum " +
                "OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(dataSql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ps.setInt(params.size() + 1, offset);
            ps.setInt(params.size() + 2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("SITEID", rs.getString("SITEID"));
                    row.put("WONUM", rs.getString("WONUM"));
                    row.put("ITEMNUM", rs.getString("ITEMNUM"));
                    row.put("LOCATION", rs.getString("LOCATION"));
                    row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
                    row.put("ITEMQTY", rs.getBigDecimal("ITEMQTY"));
                    row.put("CURBAL", rs.getBigDecimal("CURBAL"));
                    row.put("RESERVEDQTY", rs.getBigDecimal("RESERVEDQTY"));
                    // 状态: 需求数量+预约数量<=当前库存余量 显示充足,否则不足
                    BigDecimal itemqty = rs.getBigDecimal("ITEMQTY");
                    BigDecimal reserved = rs.getBigDecimal("RESERVEDQTY");
                    BigDecimal curbal = rs.getBigDecimal("CURBAL");
                    BigDecimal need = (itemqty == null ? BigDecimal.ZERO : itemqty)
                            .add(reserved == null ? BigDecimal.ZERO : reserved);
                    boolean sufficient = curbal != null && need.compareTo(curbal) <= 0;
                    row.put("V_STATUS", sufficient ? "充足" : "不足");
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询工单库存余量列表失败: " + e.getMessage(), e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rows", rows);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 行详情: 查询三类明细数据(字段标题由前端硬编码中文)
     * @param siteid   站点
     * @param location 位置
     * @param itemnum  物料号
     * @param wonum    工单号
     * @return { wp: [...], resCur: [...], resOther: [...] }
     */
    public Map<String, Object> queryDetail(String siteid, String location, String itemnum, String wonum) {
        // 当前工单物料
        String wpSql = "SELECT * FROM WPMATERIAL WHERE siteid = ? AND LOCATION = ? AND itemnum = ? AND wonum = ?";
        // 当前工单预留的数量
        String resCurSql = "SELECT * FROM INVRESERVE WHERE siteid = ? AND LOCATION = ? AND itemnum = ? AND wonum = ?";
        // 其它工单预留的数量(排除当前工单)
        String resOtherSql = "SELECT * FROM INVRESERVE WHERE siteid = ? AND LOCATION = ? AND itemnum = ? AND wonum != ?";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wp", queryDetailBySql(wpSql, siteid, location, itemnum, wonum));
        result.put("resCur", queryDetailBySql(resCurSql, siteid, location, itemnum, wonum));
        result.put("resOther", queryDetailBySql(resOtherSql, siteid, location, itemnum, wonum));
        return result;
    }

    private List<Map<String, Object>> queryDetailBySql(String sql, String siteid, String location, String itemnum, String wonum) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, siteid);
            ps.setString(2, location);
            ps.setString(3, itemnum);
            ps.setString(4, wonum);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int colCount = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(md.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询工单库存余量详情失败: " + e.getMessage(), e);
        }
        return rows;
    }
}