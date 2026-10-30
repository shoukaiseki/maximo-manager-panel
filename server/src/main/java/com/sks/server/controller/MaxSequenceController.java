package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MaxSequenceService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

/**
 * MAXSEQUENCE（唯一标识序列预留）查询与诊断 API。
 * GET /solonapi/maxsequence/list?tbname=&name=&sequencename=&pageNum=1&pageSize=20&where=
 * GET /solonapi/maxsequence/diagnose?ids=1,2,3 或 tbname=&name=&sequencename=&where=
 */
@Controller
public class MaxSequenceController {

    @Inject
    private MaxSequenceService maxSequenceService;

    @Mapping(value = "/maxsequence/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String tbname, String name, String sequencename,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize,
            String where) {
        try {
            return RestResult.ok(maxSequenceService.list(tbname, name, sequencename, pageNum, pageSize, where));
        } catch (Exception e) {
            return RestResult.error("查询 MAXSEQUENCE 失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/maxsequence/diagnose", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> diagnose(
            String ids, String tbname, String name, String sequencename, String where) {
        try {
            return RestResult.ok(maxSequenceService.diagnose(ids, tbname, name, sequencename, where));
        } catch (Exception e) {
            return RestResult.error("诊断 MAXSEQUENCE 失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/maxsequence/repair", method = MethodType.POST)
    public RestResult<List<Map<String, Object>>> repair(String ids) {
        try {
            return RestResult.ok(maxSequenceService.repair(ids));
        } catch (Exception e) {
            return RestResult.error("修复序列失败: " + e.getMessage());
        }
    }
}
