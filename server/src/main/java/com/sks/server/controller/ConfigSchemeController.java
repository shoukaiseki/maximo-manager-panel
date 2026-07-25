package com.sks.server.controller;

import com.sks.server.model.ExcelImportScheme;
import com.sks.server.model.RestResult;
import com.sks.server.service.ConfigSchemeService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;

/**
 * Excel 配置方案管理 API
 */
@Controller
public class ConfigSchemeController {

    @Inject
    private ConfigSchemeService configSchemeService;

    /**
     * 获取所有配置方案列表
     * GET /solonapi/excelimport/schemes
     */
    @Mapping(value = "/excelimport/schemes", method = MethodType.GET)
    public RestResult<List<ExcelImportScheme>> list() {
        try {
            List<ExcelImportScheme> list = configSchemeService.listSchemes();
            return RestResult.ok(list);
        } catch (Exception e) {
            return RestResult.error("查询方案列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个配置方案
     * GET /solonapi/excelimport/schemes/{id}
     */
    @Mapping(value = "/excelimport/schemes/{id}", method = MethodType.GET)
    public RestResult<ExcelImportScheme> get(@Path("id") Long id) {
        try {
            ExcelImportScheme scheme = configSchemeService.getScheme(id);
            if (scheme == null) {
                return RestResult.error("方案不存在");
            }
            return RestResult.ok(scheme);
        } catch (Exception e) {
            return RestResult.error("查询方案失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置方案（新增）
     * POST /solonapi/excelimport/schemes
     */
    @Mapping(value = "/excelimport/schemes", method = MethodType.POST)
    public RestResult<ExcelImportScheme> save(ExcelImportScheme scheme) {
        if (scheme.getSchemeName() == null || scheme.getSchemeName().trim().isEmpty()) {
            return RestResult.error("方案名称不能为空");
        }
        if (scheme.getXmlConfig() == null || scheme.getXmlConfig().trim().isEmpty()) {
            return RestResult.error("XML 配置不能为空");
        }
        try {
            ExcelImportScheme saved = configSchemeService.saveScheme(scheme);
            return RestResult.ok(saved);
        } catch (Exception e) {
            return RestResult.error("保存方案失败: " + e.getMessage());
        }
    }

    /**
     * 更新配置方案
     * PUT /solonapi/excelimport/schemes/{id}
     */
    @Mapping(value = "/excelimport/schemes/{id}", method = MethodType.PUT)
    public RestResult<ExcelImportScheme> update(@Path("id") Long id, ExcelImportScheme scheme) {
        if (scheme.getSchemeName() == null || scheme.getSchemeName().trim().isEmpty()) {
            return RestResult.error("方案名称不能为空");
        }
        try {
            ExcelImportScheme updated = configSchemeService.updateScheme(id, scheme);
            return RestResult.ok(updated);
        } catch (Exception e) {
            return RestResult.error("更新方案失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置方案
     * DELETE /solonapi/excelimport/schemes/{id}
     */
    @Mapping(value = "/excelimport/schemes/{id}", method = MethodType.DELETE)
    public RestResult<String> delete(@Path("id") Long id) {
        try {
            configSchemeService.deleteScheme(id);
            return RestResult.ok("删除成功");
        } catch (Exception e) {
            return RestResult.error("删除方案失败: " + e.getMessage());
        }
    }
}
