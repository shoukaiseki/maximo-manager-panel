package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.SavedQueryService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

/**
 * 保存的查询管理接口
 */
@Controller
public class SavedQueryController {

    @Inject
    private SavedQueryService savedQueryService;

    /**
     * 查询某应用下的保存查询列表
     * GET /solonapi/savedquery/list?app=DOMAIN
     */
    @Mapping(value = "/savedquery/list", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> list(String app) {
        return RestResult.ok(savedQueryService.listQueries(app));
    }

    /**
     * 获取单个保存查询
     * GET /solonapi/savedquery/detail?id=1
     */
    @Mapping(value = "/savedquery/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(Long id) {
        return RestResult.ok(savedQueryService.getQuery(id));
    }

    /**
     * 保存查询（存在 app+queryname 则更新）
     * POST /solonapi/savedquery/save
     * body: {app, queryname, whereclause, description}
     */
    @Mapping(value = "/savedquery/save", method = MethodType.POST)
    public RestResult<Map<String, Object>> save(com.sks.server.model.SavedQueryReq req) {
        Map<String, Object> query = savedQueryService.saveQuery(req.getApp(), req.getQueryname(), req.getWhereclause(), req.getDescription());
        return RestResult.ok(query);
    }

    /**
     * 删除保存的查询
     * POST /solonapi/savedquery/delete?id=1
     */
    @Mapping(value = "/savedquery/delete", method = MethodType.POST)
    public RestResult<Boolean> delete(Long id) {
        return RestResult.ok(savedQueryService.deleteQuery(id));
    }
}
