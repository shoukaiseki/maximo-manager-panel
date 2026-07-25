package com.sks.server.controller;

import com.sks.server.model.ExcelImportResult;
import com.sks.server.model.ExcelPreviewResult;
import com.sks.server.model.RestResult;
import com.sks.server.service.ExcelImportService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.handle.UploadedFile;

import java.util.List;
import java.util.Map;

/**
 * Excel 导入 API
 */
@Controller
public class ExcelImportController {

    @Inject
    private ExcelImportService excelImportService;

    /**
     * 预览 Excel - 读取原始数据并返回 JSON
     * POST /solonapi/excelimport/preview
     */
    @Mapping(value = "/excelimport/preview", method = MethodType.POST)
    public RestResult<Map<String, Object>> preview(UploadedFile file) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传 Excel 文件");
        }

        String fileName = file.getName() != null ? file.getName() : "";
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            return RestResult.error("请上传 .xls 或 .xlsx 格式的 Excel 文件");
        }

        try {
            Map<String, Object> result = excelImportService.previewExcel(file.getContent());
            result.put("fileName", fileName);
            result.put("fileSize", file.getContentSize());
            return RestResult.ok(result);
        } catch (Exception e) {
            return RestResult.error("Excel 解析失败: " + e.getMessage());
        }
    }

    /**
     * 使用 jxls XML 配置解析 Excel 并预览
     * POST /solonapi/excelimport/previewWithConfig
     */
    @Mapping(value = "/excelimport/previewWithConfig", method = MethodType.POST)
    public RestResult<Map<String, Object>> previewWithConfig(UploadedFile file, String xmlConfig) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传 Excel 文件");
        }
        if (xmlConfig == null || xmlConfig.trim().isEmpty()) {
            return RestResult.error("请提供 jxls XML 配置");
        }

        try {
            Map<String, Object> result = excelImportService.parseWithConfig(file.getContent(), xmlConfig);
            result.put("fileName", file.getName());
            result.put("fileSize", file.getContentSize());
            return RestResult.ok(result);
        } catch (Exception e) {
            return RestResult.error("配置解析失败: " + e.getMessage());
        }
    }

    /**
     * 执行导入（预留）
     * POST /solonapi/excelimport/execute
     */
    @Mapping(value = "/excelimport/execute", method = MethodType.POST)
    public RestResult<ExcelImportResult> execute(UploadedFile file, String xmlConfig, String params) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传 Excel 文件");
        }
        if (xmlConfig == null || xmlConfig.trim().isEmpty()) {
            return RestResult.error("请提供 jxls XML 配置");
        }

        try {
            Map<String, Object> parsedData = excelImportService.parseWithConfig(file.getContent(), xmlConfig);

            // 此处预留实际导入逻辑（如写入数据库等）
            ExcelImportResult result = new ExcelImportResult();
            Map<String, Object> parsed = (Map<String, Object>) parsedData.get("parsed");
            List<List<Object>> raw = (List<List<Object>>) parsedData.get("raw");
            result.setTotalRows(raw != null ? raw.size() : 0);
            result.setSuccessRows(raw != null ? raw.size() : 0);
            result.setFailedRows(0);
            result.setSuccess(true);
            result.setErrorMessage(null);

            return RestResult.ok(result);
        } catch (Exception e) {
            ExcelImportResult result = new ExcelImportResult();
            result.setSuccess(false);
            result.setErrorMessage("导入失败: " + e.getMessage());
            return RestResult.failure(500, "导入失败: " + e.getMessage());
        }
    }
}
