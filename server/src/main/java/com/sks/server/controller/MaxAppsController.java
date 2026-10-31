package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MaxAppsService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * MAXAPPS 应用查询接口
 */
@Controller
public class MaxAppsController {

    @Inject
    private MaxAppsService maxAppsService;

    /**
     * MAXAPPS 列表查询（支持分页）
     * GET /solonapi/maxapps/list?app=&description=&content=&contentCaseSensitive=true&tab=all&pageNum=1&pageSize=20
     * tab: all=所有应用；xml=有 XML 的应用；mafapp=MAFAPP（APPTYPE in ('WC')）
     */
    @Mapping(value = "/maxapps/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String app, String description, String content,
            @Param(defaultValue = "true") boolean contentCaseSensitive,
            @Param(defaultValue = "all") String tab,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = maxAppsService.queryList(
                app, description, content, contentCaseSensitive, tab, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * MAXAPPS 单个应用详情（完整字段）
     * GET /solonapi/maxapps/detail?app=ACTION
     */
    @Mapping(value = "/maxapps/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(String app) {
        return RestResult.ok(maxAppsService.getDetail(app));
    }
}
