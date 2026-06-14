package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MaxMenuService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.*;

/**
 * Maximo 菜单管理接口
 */
@Controller
public class MaxMenuController {

    @Inject
    private MaxMenuService maxMenuService;

    /**
     * 查询所有模块列表
     * GET /maxmenu/modules
     */
    @Mapping(value = "/maxmenu/modules", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> modules(String sortBy) {
        List<Map<String, Object>> data = maxMenuService.queryModules(sortBy);
        return RestResult.ok(data);
    }

    /**
     * 查询指定模块的菜单树
     * GET /maxmenu/moduleTree?module=ASSET
     */
    @Mapping(value = "/maxmenu/moduleTree", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> moduleTree(String module) {
        if (module == null || module.trim().isEmpty()) {
            return RestResult.error("模块名称不能为空");
        }
        List<Map<String, Object>> data = maxMenuService.queryModuleMenuTree(module);
        return RestResult.ok(data);
    }

    /**
     * 查询完整菜单树（所有模块 + 菜单项）
     * GET /maxmenu/fullTree
     */
    @Mapping(value = "/maxmenu/fullTree", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> fullTree(String sortBy) {
        List<Map<String, Object>> data = maxMenuService.queryFullMenuTree(sortBy);
        return RestResult.ok(data);
    }

    /**
     * 查询应用菜单（APPMENU）
     * GET /maxmenu/appMenu?app=ASSET
     */
    @Mapping(value = "/maxmenu/appMenu", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> appMenu(String app) {
        if (app == null || app.trim().isEmpty()) {
            return RestResult.error("应用名称不能为空");
        }
        List<Map<String, Object>> data = maxMenuService.queryAppMenu(app);
        return RestResult.ok(data);
    }

    /**
     * 查询应用工具栏（APPTOOL）
     * GET /maxmenu/appTool?app=ASSET
     */
    @Mapping(value = "/maxmenu/appTool", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> appTool(String app) {
        if (app == null || app.trim().isEmpty()) {
            return RestResult.error("应用名称不能为空");
        }
        List<Map<String, Object>> data = maxMenuService.queryAppTool(app);
        return RestResult.ok(data);
    }

    /**
     * 搜索菜单
     * GET /maxmenu/search?keyword=&menuType=&elementType=
     */
    @Mapping(value = "/maxmenu/search", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> search(
            String keyword, String menuType, String elementType) {
        List<Map<String, Object>> data = maxMenuService.searchMenu(keyword, menuType, elementType);
        return RestResult.ok(data);
    }
}
