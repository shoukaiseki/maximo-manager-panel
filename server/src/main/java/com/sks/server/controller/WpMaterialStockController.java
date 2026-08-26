package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.WpMaterialStockService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.Map;

/**
 * 工单库存余量分析接口
 */
@Controller
public class WpMaterialStockController {

    @Inject
    private WpMaterialStockService wpMaterialStockService;

    /**
     * 工单库存余量分页查询
     * GET /solonapi/wpmaterialstock/list?wonum=&itemnum=&pageNum=&pageSize=
     */
    @Mapping(value = "/wpmaterialstock/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(String wonum, String itemnum, Integer pageNum, Integer pageSize) {
        try {
            int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
            int ps = pageSize == null || pageSize < 1 ? 20 : pageSize;
            return RestResult.ok(wpMaterialStockService.queryList(wonum, itemnum, pn, ps));
        } catch (Exception e) {
            return RestResult.error("查询工单库存余量失败: " + e.getMessage());
        }
    }

    /**
     * 工单库存余量详情(三类明细)
     * GET /solonapi/wpmaterialstock/detail?siteid=&location=&itemnum=&wonum=
     */
    @Mapping(value = "/wpmaterialstock/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(String siteid, String location, String itemnum, String wonum) {
        try {
            return RestResult.ok(wpMaterialStockService.queryDetail(siteid, location, itemnum, wonum));
        } catch (Exception e) {
            return RestResult.error("查询工单库存余量详情失败: " + e.getMessage());
        }
    }
}