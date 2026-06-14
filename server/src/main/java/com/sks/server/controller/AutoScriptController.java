package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.AutoScriptService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.*;

/**
 * 自动化脚本查询诊断接口
 */
@Controller
public class AutoScriptController {

    @Inject
    private AutoScriptService autoScriptService;

    /**
     * 脚本列表查询（支持分页 + 查询模式/诊断模式）
     * GET /autoscript/list?autoscript=&description=&objectname=&attributename=&launchpointname=&source=&mode=diag&pageNum=1&pageSize=20
     */
    @Mapping(value = "/autoscript/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String autoscript, String description, String objectname,
            String attributename, String launchpointname, String source,
            @Param(defaultValue = "diag") String mode,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = autoScriptService.queryAutoScriptList(
                autoscript, description, objectname, attributename,
                launchpointname, source, mode, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * 脚本详情（主表全字段，排除 SOURCE）
     * GET /autoscript/detail?name=xxx
     */
    @Mapping(value = "/autoscript/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(String name) {
        if (name == null || name.trim().isEmpty()) {
            return RestResult.error("脚本名称不能为空");
        }
        Map<String, Object> data = autoScriptService.queryAutoScriptDetail(name);
        return RestResult.ok(data);
    }

    /**
     * 脚本源码查询
     * GET /autoscript/source?name=xxx
     */
    @Mapping(value = "/autoscript/source", method = MethodType.GET)
    public RestResult<Map<String, Object>> source(String name) {
        if (name == null || name.trim().isEmpty()) {
            return RestResult.error("脚本名称不能为空");
        }
        Map<String, Object> data = autoScriptService.queryAutoScriptSource(name);
        return RestResult.ok(data);
    }

    /**
     * 脚本完整详情（主表 + 启动点 + 变量）
     * GET /autoscript/fullDetail?name=xxx
     */
    @Mapping(value = "/autoscript/fullDetail", method = MethodType.GET)
    public RestResult<Map<String, Object>> fullDetail(String name) {
        if (name == null || name.trim().isEmpty()) {
            return RestResult.error("脚本名称不能为空");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mainInfo", autoScriptService.queryAutoScriptDetail(name));
        result.put("launchPoints", autoScriptService.queryLaunchPoints(name));
        result.put("vars", autoScriptService.queryAutoScriptVars(name));
        return RestResult.ok(result);
    }

    /**
     * 启动点列表
     * GET /autoscript/launchpoints?name=xxx
     */
    @Mapping(value = "/autoscript/launchpoints", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> launchPoints(String name) {
        if (name == null || name.trim().isEmpty()) {
            return RestResult.error("脚本名称不能为空");
        }
        List<Map<String, Object>> data = autoScriptService.queryLaunchPoints(name);
        return RestResult.ok(data);
    }
}
