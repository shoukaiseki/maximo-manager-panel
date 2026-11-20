package com.sks.server.controller;

import com.sks.server.model.CondDebugRecord;
import com.sks.server.model.RestResult;
import com.sks.server.service.CondDebugService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;

/**
 * 条件表达式调试记录管理接口（MySQL）
 */
@Controller
public class CondDebugController {

    @Inject
    private CondDebugService condDebugService;

    /**
     * 查询调试记录列表
     * GET /solonapi/conddebug/list?keyword=
     */
    @Mapping(value = "/conddebug/list", method = MethodType.GET)
    public RestResult<List<CondDebugRecord>> list(String keyword) {
        try {
            return RestResult.ok(condDebugService.listRecords(keyword));
        } catch (Exception e) {
            return RestResult.error("查询调试记录失败: " + e.getMessage());
        }
    }

    /**
     * 获取单条调试记录
     * GET /solonapi/conddebug/detail?id=1
     */
    @Mapping(value = "/conddebug/detail", method = MethodType.GET)
    public RestResult<CondDebugRecord> detail(Long id) {
        try {
            CondDebugRecord record = condDebugService.getRecord(id);
            if (record == null) {
                return RestResult.error("调试记录不存在");
            }
            return RestResult.ok(record);
        } catch (Exception e) {
            return RestResult.error("查询调试记录失败: " + e.getMessage());
        }
    }

    /**
     * 保存调试记录（按 recordName 匹配，存在则更新）
     * POST /solonapi/conddebug/save
     */
    @Mapping(value = "/conddebug/save", method = MethodType.POST)
    public RestResult<CondDebugRecord> save(@Body CondDebugRecord req) {
        if (req.getRecordName() == null || req.getRecordName().trim().isEmpty()) {
            return RestResult.error("记录名称不能为空");
        }
        try {
            return RestResult.ok(condDebugService.saveRecord(req));
        } catch (Exception e) {
            return RestResult.error("保存调试记录失败: " + e.getMessage());
        }
    }

    /**
     * 删除调试记录
     * POST /solonapi/conddebug/delete?id=1
     */
    @Mapping(value = "/conddebug/delete", method = MethodType.POST)
    public RestResult<Boolean> delete(Long id) {
        try {
            return RestResult.ok(condDebugService.deleteRecord(id));
        } catch (Exception e) {
            return RestResult.error("删除调试记录失败: " + e.getMessage());
        }
    }
}
