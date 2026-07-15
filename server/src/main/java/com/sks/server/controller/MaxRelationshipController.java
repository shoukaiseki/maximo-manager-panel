package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MaxRelationshipService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

@Controller
public class MaxRelationshipController {

    @Inject
    private MaxRelationshipService maxRelationshipService;

    @Mapping(value = "/relationship/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(String name,
                                                 String parent,
                                                 String child,
                                                 String keyword,
                                                 @Param(defaultValue = "1") int pageNum,
                                                 @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = maxRelationshipService.queryList(name, parent, child, keyword, pageNum, pageSize);
        return RestResult.ok(data);
    }

    @Mapping(value = "/relationship/{id}", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(@Path("id") Long id) {
        Map<String, Object> data = maxRelationshipService.getById(id);
        if (data == null) {
            return RestResult.error("关系不存在");
        }
        return RestResult.ok(data);
    }

    @Mapping(value = "/relationship/all", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> all() {
        List<Map<String, Object>> data = maxRelationshipService.queryAll();
        return RestResult.ok(data);
    }

    @Mapping(value = "/relationship/parent/{parent}", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> byParent(@Path("parent") String parent) {
        List<Map<String, Object>> data = maxRelationshipService.queryRelationshipsByParent(parent);
        return RestResult.ok(data);
    }
}