package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Component
public class MaxMenuService {

    @Inject
    private DataSource dataSource;

    /**
     * 查询所有模块列表（带中文描述）
     */
    public List<Map<String, Object>> queryModules(String sortBy) {
        String orderBy;
        switch (sortBy != null ? sortBy : "zh") {
            case "en":
                orderBy = "m.DESCRIPTION";
                break;
            case "module":
                orderBy = "m.MODULE";
                break;
            case "zh":
            default:
                orderBy = "L.DESCRIPTION, m.DESCRIPTION";
                break;
        }
        String sql = "SELECT m.MODULE, m.DESCRIPTION, m.MAXMODULESID, m.SKIPNAVIGATION, m.SKIPENTITLEMENT, m.MAXPRODID, m.ROWSTAMP, " +
                "L.DESCRIPTION AS L_DESCRIPTION " +
                "FROM MAXMODULES m " +
                "LEFT JOIN L_MAXMODULES L ON L.OWNERID = m.MAXMODULESID AND L.LANGCODE = 'ZH' " +
                "ORDER BY " + orderBy;

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rowToMap(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXMODULES 失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询指定模块的菜单树（MENUTYPE=MODULE）
     */
    public List<Map<String, Object>> queryModuleMenuTree(String moduleName) {
        String sql = "SELECT mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, " +
                "mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, " +
                "COALESCE(a.DESCRIPTION, mm.HEADERDESCRIPTION) AS HEADERDESCRIPTION, " +
                "COALESCE(la.DESCRIPTION, L.HEADERDESCRIPTION, mm.HEADERDESCRIPTION) AS L_HEADERDESCRIPTION " +
                "FROM MAXMENU mm " +
                "LEFT JOIN L_MAXMENU L ON L.OWNERID = mm.MAXMENUID AND L.LANGCODE = 'ZH' " +
                "LEFT JOIN MAXAPPS a ON a.APP = mm.KEYVALUE " +
                "LEFT JOIN L_MAXAPPS la ON la.OWNERID = a.MAXAPPSID AND la.LANGCODE = 'ZH' " +
                "WHERE mm.MENUTYPE = 'MODULE' AND mm.MODULEAPP = ? " +
                "ORDER BY mm.POSITION, mm.SUBPOSITION";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, moduleName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询模块菜单树失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询所有模块的完整菜单树（一次性返回）
     */
    public List<Map<String, Object>> queryFullMenuTree(String sortBy) {
        // 先获取所有模块
        List<Map<String, Object>> modules = queryModules(sortBy);

        // 获取所有 MODULE 类型的菜单
        String sql = "SELECT mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, " +
                "mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, " +
                "COALESCE(a.DESCRIPTION, mm.HEADERDESCRIPTION) AS HEADERDESCRIPTION, " +
                "COALESCE(la.DESCRIPTION, L.HEADERDESCRIPTION, mm.HEADERDESCRIPTION) AS L_HEADERDESCRIPTION " +
                "FROM MAXMENU mm " +
                "LEFT JOIN L_MAXMENU L ON L.OWNERID = mm.MAXMENUID AND L.LANGCODE = 'ZH' " +
                "LEFT JOIN MAXAPPS a ON a.APP = mm.KEYVALUE " +
                "LEFT JOIN L_MAXAPPS la ON la.OWNERID = a.MAXAPPSID AND la.LANGCODE = 'ZH' " +
                "WHERE mm.MENUTYPE = 'MODULE' " +
                "ORDER BY mm.MODULEAPP, mm.POSITION, mm.SUBPOSITION";

        // 按 MODULEAPP 分组
        Map<String, List<Map<String, Object>>> menuByModule = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = rowToMap(rs);
                String moduleApp = (String) row.get("MODULEAPP");
                menuByModule.computeIfAbsent(moduleApp, k -> new ArrayList<>()).add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜单树失败: " + e.getMessage(), e);
        }

        // 组装树结构
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> module : modules) {
            String moduleName = (String) module.get("MODULE");
            Map<String, Object> node = new LinkedHashMap<>(module);
            List<Map<String, Object>> menuItems = menuByModule.getOrDefault(moduleName, Collections.emptyList());
            // 提取 ELEMENTTYPE='MODULE' 的记录，合并到模块节点
            List<Map<String, Object>> nonModuleItems = new ArrayList<>();
            for (Map<String, Object> item : menuItems) {
                if ("MODULE".equals(item.get("ELEMENTTYPE"))) {
                    // 将所有 MAXMENU 字段以 MENU_ 前缀合并到模块节点
                    node.put("MENU_VISIBLE", item.get("VISIBLE"));
                    node.put("MENU_PINNED", item.get("PINNED"));
                    node.put("MENU_POSITION", item.get("POSITION"));
                    node.put("MENU_MAXMENUID", item.get("MAXMENUID"));
                    node.put("MENU_ROWSTAMP", item.get("ROWSTAMP"));
                    node.put("MENU_KEYVALUE", item.get("KEYVALUE"));
                    node.put("MENU_MODULEAPP", item.get("MODULEAPP"));
                    node.put("MENU_MENUTYPE", item.get("MENUTYPE"));
                    node.put("MENU_SUBPOSITION", item.get("SUBPOSITION"));
                    node.put("MENU_ELEMENTTYPE", item.get("ELEMENTTYPE"));
                    node.put("MENU_URL", item.get("URL"));
                    node.put("MENU_IMAGE", item.get("IMAGE"));
                    node.put("MENU_ACCESSKEY", item.get("ACCESSKEY"));
                    node.put("MENU_TABDISPLAY", item.get("TABDISPLAY"));
                    node.put("MENU_HEADERDESCRIPTION", item.get("HEADERDESCRIPTION"));
                    node.put("MENU_L_HEADERDESCRIPTION", item.get("L_HEADERDESCRIPTION"));
                } else {
                    nonModuleItems.add(item);
                }
            }
            node.put("children", buildTreeFromFlatList(nonModuleItems));
            node.put("_menuCount", nonModuleItems.size());
            tree.add(node);
        }
        return tree;
    }

    /**
     * 将扁平菜单列表构建为树结构
     * POSITION 代表一级分组（HEADER），SUBPOSITION 代表子项
     */
    private List<Map<String, Object>> buildTreeFromFlatList(List<Map<String, Object>> flatList) {
        // 按 POSITION 分组
        Map<Integer, List<Map<String, Object>>> byPosition = new LinkedHashMap<>();
        for (Map<String, Object> item : flatList) {
            int pos = item.get("POSITION") != null ? ((Number) item.get("POSITION")).intValue() : 0;
            byPosition.computeIfAbsent(pos, k -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<Integer, List<Map<String, Object>>> entry : byPosition.entrySet()) {
            List<Map<String, Object>> items = entry.getValue();
            // 找到 HEADER 类型的项作为父节点
            Map<String, Object> header = null;
            List<Map<String, Object>> children = new ArrayList<>();

            for (Map<String, Object> item : items) {
                String elementType = (String) item.get("ELEMENTTYPE");
                int subPos = item.get("SUBPOSITION") != null ? ((Number) item.get("SUBPOSITION")).intValue() : 0;

                if ("HEADER".equals(elementType) && subPos == 0) {
                    header = item;
                } else {
                    children.add(item);
                }
            }

            if (header != null) {
                Map<String, Object> node = new LinkedHashMap<>(header);
                node.put("children", children);
                tree.add(node);
            } else {
                // 没有 header，直接作为子项
                tree.addAll(items);
            }
        }
        return tree;
    }

    /**
     * 查询指定应用的 APPMENU（应用菜单）
     */
    public List<Map<String, Object>> queryAppMenu(String appName) {
        String sql = "SELECT mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, " +
                "mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, " +
                "COALESCE(a.DESCRIPTION, mm.HEADERDESCRIPTION) AS HEADERDESCRIPTION, " +
                "COALESCE(la.DESCRIPTION, L.HEADERDESCRIPTION, mm.HEADERDESCRIPTION) AS L_HEADERDESCRIPTION " +
                "FROM MAXMENU mm " +
                "LEFT JOIN L_MAXMENU L ON L.OWNERID = mm.MAXMENUID AND L.LANGCODE = 'ZH' " +
                "LEFT JOIN MAXAPPS a ON a.APP = mm.KEYVALUE " +
                "LEFT JOIN L_MAXAPPS la ON la.OWNERID = a.MAXAPPSID AND la.LANGCODE = 'ZH' " +
                "WHERE mm.MENUTYPE = 'APPMENU' AND mm.MODULEAPP = ? " +
                "ORDER BY mm.POSITION, mm.SUBPOSITION";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询应用菜单失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询指定应用的 APPTOOL（工具栏）
     */
    public List<Map<String, Object>> queryAppTool(String appName) {
        String sql = "SELECT mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, " +
                "mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, " +
                "COALESCE(a.DESCRIPTION, mm.HEADERDESCRIPTION) AS HEADERDESCRIPTION, " +
                "COALESCE(la.DESCRIPTION, L.HEADERDESCRIPTION, mm.HEADERDESCRIPTION) AS L_HEADERDESCRIPTION " +
                "FROM MAXMENU mm " +
                "LEFT JOIN L_MAXMENU L ON L.OWNERID = mm.MAXMENUID AND L.LANGCODE = 'ZH' " +
                "LEFT JOIN MAXAPPS a ON a.APP = mm.KEYVALUE " +
                "LEFT JOIN L_MAXAPPS la ON la.OWNERID = a.MAXAPPSID AND la.LANGCODE = 'ZH' " +
                "WHERE mm.MENUTYPE = 'APPTOOL' AND mm.MODULEAPP = ? " +
                "ORDER BY mm.POSITION, mm.SUBPOSITION";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appName.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询应用工具栏失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 按条件搜索菜单
     */
    public List<Map<String, Object>> searchMenu(String keyword, String menuType, String elementType) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, ");
        sql.append("mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, ");
        sql.append("COALESCE(a.DESCRIPTION, mm.HEADERDESCRIPTION) AS HEADERDESCRIPTION, ");
        sql.append("COALESCE(la.DESCRIPTION, L.HEADERDESCRIPTION, mm.HEADERDESCRIPTION) AS L_HEADERDESCRIPTION ");
        sql.append("FROM MAXMENU mm ");
        sql.append("LEFT JOIN L_MAXMENU L ON L.OWNERID = mm.MAXMENUID AND L.LANGCODE = 'ZH' ");
        sql.append("LEFT JOIN MAXAPPS a ON a.APP = mm.KEYVALUE ");
        sql.append("LEFT JOIN L_MAXAPPS la ON la.OWNERID = a.MAXAPPSID AND la.LANGCODE = 'ZH' ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (menuType != null && !menuType.trim().isEmpty()) {
            sql.append("AND mm.MENUTYPE = ? ");
            params.add(menuType.trim().toUpperCase());
        }
        if (elementType != null && !elementType.trim().isEmpty()) {
            sql.append("AND mm.ELEMENTTYPE = ? ");
            params.add(elementType.trim().toUpperCase());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (mm.KEYVALUE LIKE ? OR mm.HEADERDESCRIPTION LIKE ? OR mm.MODULEAPP LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        sql.append("ORDER BY mm.MENUTYPE, mm.MODULEAPP, mm.POSITION, mm.SUBPOSITION");

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("搜索菜单失败: " + e.getMessage(), e);
        }
        return result;
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

    /**
     * 查询菜单列表（按 MENUTYPE 分组）
     * 根据 TASK06 需求，SQL 需要包含 SIGOPTION 的描述信息
     */
    public List<Map<String, Object>> queryMenuList(String moduleApp, String menuType) {
        String sql = "SELECT DISTINCT " +
                "SIGOPTION.DESCRIPTION AS DESCRIPTIONEN, " +
                "l.DESCRIPTION AS DESCRIPTIONZH, " +
                "mm.MAXMENUID, mm.MENUTYPE, mm.MODULEAPP, mm.ELEMENTTYPE, mm.POSITION, mm.SUBPOSITION, " +
                "mm.KEYVALUE, mm.URL, mm.VISIBLE, mm.IMAGE, mm.ACCESSKEY, mm.TABDISPLAY, mm.PINNED, mm.ROWSTAMP, " +
                "mm.HEADERDESCRIPTION, LMM.HEADERDESCRIPTION AS L_HEADERDESCRIPTION " +
                "FROM MAXMENU mm " +
                "LEFT JOIN SIGOPTION ON mm.KEYVALUE = SIGOPTION.OPTIONNAME AND SIGOPTION.APP = mm.MODULEAPP " +
                "LEFT JOIN L_SIGOPTION l ON l.OWNERID = SIGOPTION.SIGOPTIONID AND l.LANGCODE = 'ZH' " +
                "LEFT JOIN L_MAXMENU LMM ON LMM.OWNERID = mm.MAXMENUID AND LMM.LANGCODE = 'ZH' " +
                "WHERE mm.MODULEAPP = ? AND mm.MENUTYPE != 'MODULE' ";

        if (menuType != null && !menuType.trim().isEmpty()) {
            sql += "AND mm.MENUTYPE = ? ";
        }

        sql += "ORDER BY mm.MENUTYPE, mm.POSITION, mm.SUBPOSITION";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, moduleApp.trim().toUpperCase());
            if (menuType != null && !menuType.trim().isEmpty()) {
                ps.setString(2, menuType.trim().toUpperCase());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询菜单列表失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询签名选项（SIGOPTION）
     * 根据 TASK06 需求，SQL 需要包含 L_SIGOPTION 的中文描述
     */
    public List<Map<String, Object>> querySigOption(String app) {
        String sql = "SELECT " +
                "l.DESCRIPTION, " +
                "SIGOPTION.SIGOPTIONID, SIGOPTION.APP, SIGOPTION.OPTIONNAME, " +
                "SIGOPTION.DESCRIPTION AS EN_DESCRIPTION, SIGOPTION.ESIGENABLED, " +
                "SIGOPTION.VISIBLE, SIGOPTION.ALSOGRANTS, SIGOPTION.ALSOREVOKES, " +
                "SIGOPTION.PREREQUISITE, SIGOPTION.HASLD, SIGOPTION.ROWSTAMP " +
                "FROM SIGOPTION " +
                "LEFT JOIN L_SIGOPTION l ON l.OWNERID = SIGOPTION.SIGOPTIONID AND l.LANGCODE = 'ZH' " +
                "WHERE SIGOPTION.APP = ? " +
                "ORDER BY SIGOPTION.OPTIONNAME";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, app.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询签名选项失败: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 查询签名条件属性（CTRLGROUP → CTRLCONDITION → CTRLCONDPROP）
     * 按照层级结构返回
     */
    public List<Map<String, Object>> queryCtrlGroup(String app) {
        String ctrlGroupSql = "SELECT cg.CTRLGROUPID, cg.GROUPNAME, cg.OPTIONNAME, cg.APP, cg.GROUPSEQ, cg.ROWSTAMP " +
                "FROM CTRLGROUP cg " +
                "WHERE cg.APP = ? " +
                "ORDER BY cg.GROUPSEQ";

        String ctrlConditionSql = "SELECT cc.CTRLCONDITIONID, cc.CTRLGROUPID, cc.CONDITIONNUM, cc.CONDITIONSEQ, cc.REEVALUATE, cc.ROWSTAMP " +
                "FROM CTRLCONDITION cc " +
                "WHERE cc.CTRLGROUPID = ? " +
                "ORDER BY cc.CONDITIONSEQ";

        String ctrlCondPropSql = "SELECT cp.CTRLCONDPROPID, cp.CTRLCONDITIONID, cp.PROPERTY, cp.PROPERTYVALUE, " +
                "cp.CONDITIONRESULT, cp.ROWSTAMP, " +
                "l.PROPERTYVALUE AS L_PROPERTYVALUE " +
                "FROM CTRLCONDPROP cp " +
                "LEFT JOIN L_CTRLCONDPROP l ON l.OWNERID = cp.CTRLCONDPROPID AND l.LANGCODE = 'ZH' " +
                "WHERE cp.CTRLCONDITIONID = ? " +
                "ORDER BY cp.PROPERTY";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(ctrlGroupSql);
             PreparedStatement ps2 = conn.prepareStatement(ctrlConditionSql);
             PreparedStatement ps3 = conn.prepareStatement(ctrlCondPropSql)) {
            ps.setString(1, app.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> group = rowToMap(rs);
                    Long ctrlGroupId = rs.getLong("CTRLGROUPID");
                    List<Map<String, Object>> conditions = new ArrayList<>();

                    ps2.setLong(1, ctrlGroupId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        while (rs2.next()) {
                            Map<String, Object> condition = rowToMap(rs2);
                            Long ctrlConditionId = rs2.getLong("CTRLCONDITIONID");
                            List<Map<String, Object>> props = new ArrayList<>();

                            ps3.setLong(1, ctrlConditionId);
                            try (ResultSet rs3 = ps3.executeQuery()) {
                                while (rs3.next()) {
                                    props.add(rowToMap(rs3));
                                }
                            }
                            condition.put("properties", props);
                            conditions.add(condition);
                        }
                    }
                    group.put("conditions", conditions);
                    result.add(group);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询签名条件属性失败: " + e.getMessage(), e);
        }
        return result;
    }
}
