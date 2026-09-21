package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.Db2LockService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * DB2 锁表查询接口
 */
@Controller
public class Db2LockController {

    @Inject
    private Db2LockService db2LockService;

    /**
     * 锁表列表查询
     * GET /solonapi/db2lock/list?tabName=&pageNum=1&pageSize=20
     */
    @Mapping(value = "/db2lock/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String tabName,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = db2LockService.queryList(tabName, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * 锁诊断：对指定进程执行固定诊断 SQL 集，返回汇总分析报告
     * GET /solonapi/db2lock/diagnose?agentId=36485&tabName=IBM_DO
     *
     * @param agentId 目标进程号
     * @param tabName 表名（可空，作为兜底；后端优先使用锁信息中解析出的表名）
     */
    @Mapping(value = "/db2lock/diagnose", method = MethodType.GET)
    public RestResult<Map<String, Object>> diagnose(@Param long agentId, String tabName) {
        Map<String, Object> data = db2LockService.diagnose(agentId, tabName);
        return RestResult.ok(data);
    }
}
