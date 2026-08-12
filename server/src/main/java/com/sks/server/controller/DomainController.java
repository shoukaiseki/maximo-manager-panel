package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.DomainService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * 域（MAXDOMAIN）子表信息查询接口
 */
@Controller
public class DomainController {

    @Inject
    private DomainService domainService;

    /**
     * 查询域的子表信息（域值 + 本地化描述）
     * GET /solonapi/domain/subtables?domainid=XXX&domaintype=SYNONYM
     */
    @Mapping(value = "/domain/subtables", method = MethodType.GET)
    public RestResult<Map<String, Object>> subtables(String domainid, String domaintype) {
        if (domainid == null || domainid.trim().isEmpty()) {
            return RestResult.error("domainid 不能为空");
        }
        Map<String, Object> data = domainService.querySubtables(domainid, domaintype);
        return RestResult.ok(data);
    }
}
