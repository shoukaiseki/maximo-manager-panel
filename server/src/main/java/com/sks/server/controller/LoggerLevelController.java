package com.sks.server.controller;

import com.sks.server.model.LoggerLevelSaveReq;
import com.sks.server.model.RestResult;
import com.sks.server.service.LoggerLevelService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

/**
 * 日志级别配置接口（仅 MySQL 持久化；推送到 Maximo 由前端直连脚本完成）
 */
@Controller
public class LoggerLevelController {

    @Inject
    private LoggerLevelService loggerLevelService;

    /**
     * 查询全部日志级别配置
     * GET /solonapi/loggerlevel/list
     */
    @Mapping(value = "/loggerlevel/list", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> list() {
        try {
            return RestResult.ok(loggerLevelService.listConfigs());
        } catch (Exception e) {
            return RestResult.error("查询日志级别配置失败: " + e.getMessage());
        }
    }

    /**
     * 查询未忽略的日志级别配置（用于"更新到 Maximo"）
     * GET /solonapi/loggerlevel/active
     */
    @Mapping(value = "/loggerlevel/active", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> active() {
        try {
            return RestResult.ok(loggerLevelService.listActiveConfigs());
        } catch (Exception e) {
            return RestResult.error("查询未忽略日志级别配置失败: " + e.getMessage());
        }
    }

    /**
     * 批量保存日志级别配置（全量覆盖）
     * POST /solonapi/loggerlevel/save
     * body: { loggers: [{ loggerName, level, ignored, description, sortOrder }] }
     */
    @Mapping(value = "/loggerlevel/save", method = MethodType.POST)
    public RestResult<List<Map<String, Object>>> save(@Body LoggerLevelSaveReq req) {
        try {
            return RestResult.ok(loggerLevelService.saveConfigs(req == null ? null : req.getLoggers()));
        } catch (Exception e) {
            return RestResult.error("保存日志级别配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除单条日志级别配置
     * POST /solonapi/loggerlevel/delete?id=1
     */
    @Mapping(value = "/loggerlevel/delete", method = MethodType.POST)
    public RestResult<Boolean> delete(Long id) {
        try {
            return RestResult.ok(loggerLevelService.deleteConfig(id));
        } catch (Exception e) {
            return RestResult.error("删除日志级别配置失败: " + e.getMessage());
        }
    }

    /**
     * 增量导入日志级别配置到默认表（已存在的跳过）
     * POST /solonapi/loggerlevel/import
     * body: { loggers: [{ loggerName, level, ignored, description? }] }
     * 返回: { added, skipped, total }
     */
    @Mapping(value = "/loggerlevel/import", method = MethodType.POST)
    public RestResult<java.util.Map<String, Object>> importConfigs(@Body LoggerLevelSaveReq req) {
        try {
            return RestResult.ok(loggerLevelService.importConfigs(req == null ? null : req.getLoggers()));
        } catch (Exception e) {
            return RestResult.error("导入日志级别配置失败: " + e.getMessage());
        }
    }
}
