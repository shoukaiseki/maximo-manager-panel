package com.sks.server.controller;

import com.sks.server.model.LoggerMxConfig;
import com.sks.server.model.LoggerMxGroup;
import com.sks.server.model.LoggerMxSaveReq;
import com.sks.server.model.RestResult;
import com.sks.server.service.LoggerMxService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;

/**
 * MXLogger 日志管理配置接口（MySQL 持久化；推送到 Maximo 由前端直连脚本完成）
 */
@Controller
public class LoggerMxController {

    @Inject
    private LoggerMxService loggerMxService;

    /**
     * 查询 MXLogger 配置树（顶层 → 一层子级）
     * GET /solonapi/loggerlevel/mx/list?groupId=1
     */
    @Mapping(value = "/loggerlevel/mx/list", method = MethodType.GET)
    public RestResult<List<LoggerMxConfig>> list(Long groupId) {
        try {
            return RestResult.ok(loggerMxService.listTree(groupId));
        } catch (Exception e) {
            return RestResult.error("查询MXLogger配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存 MXLogger 配置（清空指定组/旧数据后重建树）
     * POST /solonapi/loggerlevel/mx/save
     * body: { groupId?: 1, loggers: [{ logger, level, active, remark, sortOrder, children: [...] }] }
     */
    @Mapping(value = "/loggerlevel/mx/save", method = MethodType.POST)
    public RestResult<List<LoggerMxConfig>> save(@Body LoggerMxSaveReq req) {
        try {
            Long groupId = req == null ? null : req.getGroupId();
            return RestResult.ok(loggerMxService.saveTree(req == null ? null : req.getLoggers(), groupId));
        } catch (Exception e) {
            return RestResult.error("保存MXLogger配置失败: " + e.getMessage());
        }
    }

    /**
     * 查询全部 MXLogger 组
     * GET /solonapi/loggerlevel/mx/group/list
     */
    @Mapping(value = "/loggerlevel/mx/group/list", method = MethodType.GET)
    public RestResult<List<LoggerMxGroup>> groupList() {
        try {
            return RestResult.ok(loggerMxService.listGroups());
        } catch (Exception e) {
            return RestResult.error("查询MXLogger组失败: " + e.getMessage());
        }
    }

    /**
     * 创建 MXLogger 组
     * POST /solonapi/loggerlevel/mx/group/create
     * body: { name, description? }
     */
    @Mapping(value = "/loggerlevel/mx/group/create", method = MethodType.POST)
    public RestResult<LoggerMxGroup> groupCreate(@Body LoggerMxGroup req) {
        try {
            return RestResult.ok(loggerMxService.createGroup(req == null ? null : req.getName(),
                    req == null ? null : req.getDescription()));
        } catch (Exception e) {
            return RestResult.error("创建MXLogger组失败: " + e.getMessage());
        }
    }

    /**
     * 更新 MXLogger 组（重命名/描述）
     * POST /solonapi/loggerlevel/mx/group/update?id=1
     * body: { name, description? }
     */
    @Mapping(value = "/loggerlevel/mx/group/update", method = MethodType.POST)
    public RestResult<LoggerMxGroup> groupUpdate(Long id, @Body LoggerMxGroup req) {
        try {
            return RestResult.ok(loggerMxService.updateGroup(id, req == null ? null : req.getName(),
                    req == null ? null : req.getDescription()));
        } catch (Exception e) {
            return RestResult.error("更新MXLogger组失败: " + e.getMessage());
        }
    }

    /**
     * 删除 MXLogger 组（级联删除其下节点）
     * POST /solonapi/loggerlevel/mx/group/delete?id=1
     */
    @Mapping(value = "/loggerlevel/mx/group/delete", method = MethodType.POST)
    public RestResult<String> groupDelete(Long id) {
        try {
            loggerMxService.deleteGroup(id);
            return RestResult.ok("删除成功");
        } catch (Exception e) {
            return RestResult.error("删除MXLogger组失败: " + e.getMessage());
        }
    }
}