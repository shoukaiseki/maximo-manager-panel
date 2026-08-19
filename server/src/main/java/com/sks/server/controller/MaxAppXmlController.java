package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MaxAppXmlService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * MAXPRESENTATION 应用XML查询接口
 */
@Controller
public class MaxAppXmlController {

    @Inject
    private MaxAppXmlService maxAppXmlService;

    /**
     * MAXPRESENTATION 列表查询（支持分页）
     * GET /solonapi/maxappxml/list?app=&description=&content=&contentCaseSensitive=true&pageNum=1&pageSize=20
     */
    @Mapping(value = "/maxappxml/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String app, String description, String content,
            @Param(defaultValue = "true") boolean contentCaseSensitive,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = maxAppXmlService.queryList(
                app, description, content, contentCaseSensitive, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * 获取 PRESENTATION 源码
     * GET /solonapi/maxappxml/source?app=xxx
     */
    @Mapping(value = "/maxappxml/source", method = MethodType.GET)
    public RestResult<String> source(String app) {
        if (app == null || app.trim().isEmpty()) {
            return RestResult.error("APP 不能为空");
        }
        String source = maxAppXmlService.getSource(app);
        return RestResult.ok(source);
    }

    /**
     * 获取详情（含源码）
     * GET /solonapi/maxappxml/detail?maxPresentationId=xxx&app=xxx
     */
    @Mapping(value = "/maxappxml/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(
            @Param(defaultValue = "0") Long maxPresentationId,
            String app) {
        Map<String, Object> detail = maxAppXmlService.getDetail(app, maxPresentationId);
        if (detail == null) {
            return RestResult.error("未找到记录");
        }
        return RestResult.ok(detail);
    }

    /**
     * A_MAXPRESENTATION 审计记录列表（按 EAUDITTRANSID 倒序）
     * GET /solonapi/maxappxml/audit/list?app=&pageNum=1&pageSize=20
     */
    @Mapping(value = "/maxappxml/audit/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> auditList(
            String app,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = maxAppXmlService.queryAuditList(app, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * A_MAXPRESENTATION 审计记录详情（按 EAUDITTRANSID，含 PRESENTATION 源码）
     * GET /solonapi/maxappxml/audit/detail?eaudittransid=xxx
     */
    @Mapping(value = "/maxappxml/audit/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> auditDetail(String eaudittransid) {
        if (eaudittransid == null || eaudittransid.trim().isEmpty()) {
            return RestResult.error("EAUDITTRANSID不能为空");
        }
        Map<String, Object> data = maxAppXmlService.queryAuditDetail(eaudittransid);
        return RestResult.ok(data);
    }
}
