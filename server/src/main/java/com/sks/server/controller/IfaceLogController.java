package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.IfaceLogService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * IBM_IFACELOG 接口日志查询接口
 */
@Controller
public class IfaceLogController {

    @Inject
    private IfaceLogService ifaceLogService;

    /**
     * 列表查询
     * GET /solonapi/ifacelog/list?app=&ownerTable=&status=&description=&extSystem=&ifaceStatus=&pageNum=1&pageSize=20
     */
    @Mapping(value = "/ifacelog/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String app, String ownerTable, String status, String description,
            String extSystem, String ifaceStatus,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = ifaceLogService.queryList(
                app, ownerTable, status, description, extSystem, ifaceStatus, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * 详情查询
     * GET /solonapi/ifacelog/detail?logId=xxx
     */
    @Mapping(value = "/ifacelog/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(Long logId) {
        if (logId == null || logId <= 0) {
            return RestResult.error("logId 不能为空");
        }
        Map<String, Object> detail = ifaceLogService.getDetail(logId);
        if (detail == null) {
            return RestResult.error("未找到记录");
        }
        return RestResult.ok(detail);
    }
}
