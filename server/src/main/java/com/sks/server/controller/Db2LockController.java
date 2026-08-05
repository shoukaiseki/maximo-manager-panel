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
}
