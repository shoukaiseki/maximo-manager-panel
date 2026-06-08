package com.sks.server.controller;

import com.sks.server.model.MaxObjectDetail;
import com.sks.server.model.RestResult;
import com.sks.server.service.MaxObjectService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

/**
 * MAXOBJECT 元数据查询接口
 */
@Controller
public class MaxObjectController {

    @Inject
    private MaxObjectService maxObjectService;

    /**
     * 健康检查接口（不需要 apiKey）
     */
    @Mapping(value = "/api/health", method = MethodType.GET)
    public RestResult<String> health() {
        return RestResult.ok("ok");
    }

    /**
     * MAXOBJECT 列表查询（支持分页）
     * GET /solonapi/maxobject/list?keyword=ITEM&pageNum=1&pageSize=20
     */
    @Mapping(value = "/maxobject/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(String keyword,
                                                @Param(defaultValue = "1") int pageNum,
                                                @Param(defaultValue = "20") int pageSize) {
        Map<String, Object> data = maxObjectService.queryMaxObjectList(keyword, pageNum, pageSize);
        return RestResult.ok(data);
    }

    /**
     * 对象摘要详情（主信息 + 属性）
     * GET /solonapi/maxobject/ITEM
     */
    @Mapping(value = "/maxobject/{objectname}", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(@Path("objectname") String objectname) {
        if (objectname == null || objectname.trim().isEmpty()) {
            return RestResult.error("objectname 不能为空");
        }
        Map<String, Object> data = maxObjectService.queryDetail(objectname);
        return RestResult.ok(data);
    }

    /**
     * 对象完整详情（主信息 + 属性 + 关系 + 索引）
     * GET /solonapi/maxobject/ITEM/detail
     */
    @Mapping(value = "/maxobject/{objectname}/detail", method = MethodType.GET)
    public RestResult<MaxObjectDetail> fullDetail(@Path("objectname") String objectname) {
        if (objectname == null || objectname.trim().isEmpty()) {
            return RestResult.error("objectname 不能为空");
        }
        MaxObjectDetail data = maxObjectService.queryFullDetail(objectname);
        return RestResult.ok(data);
    }

    /**
     * 对象域信息
     * GET /solonapi/maxobject/{objectname}/domains
     */
    @Mapping(value = "/maxobject/{objectname}/domains", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> domains(@Path("objectname") String objectname) {
        if (objectname == null || objectname.trim().isEmpty()) {
            return RestResult.error("objectname 不能为空");
        }
        List<Map<String, Object>> data = maxObjectService.queryDomains(objectname);
        return RestResult.ok(data);
    }
}