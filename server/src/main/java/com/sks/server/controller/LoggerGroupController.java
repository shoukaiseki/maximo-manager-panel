package com.sks.server.controller;

import com.sks.server.model.LoggerGroupAddReq;
import com.sks.server.model.LoggerGroupCreateReq;
import com.sks.server.model.LoggerGroupItemSaveReq;
import com.sks.server.model.RestResult;
import com.sks.server.service.LoggerGroupService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.List;
import java.util.Map;

/**
 * 日志级别分组接口（用户自建分组及条目；默认分组由 LoggerLevelController 负责）
 */
@Controller
public class LoggerGroupController {

    @Inject
    private LoggerGroupService loggerGroupService;

    // ============ 分组 ============

    @Mapping(value = "/loggerlevel/group/list", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> listGroups() {
        try {
            return RestResult.ok(loggerGroupService.listGroups());
        } catch (Exception e) {
            return RestResult.error("查询分组失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> getGroup(Long id) {
        try {
            return RestResult.ok(loggerGroupService.getGroup(id));
        } catch (Exception e) {
            return RestResult.error("查询分组失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/create", method = MethodType.POST)
    public RestResult<Map<String, Object>> createGroup(@Body LoggerGroupCreateReq req) {
        try {
            return RestResult.ok(loggerGroupService.createGroup(
                    req == null ? null : req.getName(),
                    req == null ? null : req.getDescription()));
        } catch (Exception e) {
            return RestResult.error("新建分组失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/update", method = MethodType.POST)
    public RestResult<Map<String, Object>> updateGroup(Long id, @Body LoggerGroupCreateReq req) {
        try {
            return RestResult.ok(loggerGroupService.updateGroup(id,
                    req == null ? null : req.getName(),
                    req == null ? null : req.getDescription()));
        } catch (Exception e) {
            return RestResult.error("更新分组失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/delete", method = MethodType.POST)
    public RestResult<Boolean> deleteGroup(Long id) {
        try {
            return RestResult.ok(loggerGroupService.deleteGroup(id));
        } catch (Exception e) {
            return RestResult.error("删除分组失败: " + e.getMessage());
        }
    }

    // ============ 分组条目 ============

    @Mapping(value = "/loggerlevel/group/items", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> listItems(Long groupId) {
        try {
            return RestResult.ok(loggerGroupService.listItems(groupId));
        } catch (Exception e) {
            return RestResult.error("查询分组条目失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/items/save", method = MethodType.POST)
    public RestResult<List<Map<String, Object>>> saveItems(@Body LoggerGroupItemSaveReq req) {
        try {
            Long groupId = req == null ? null : req.getGroupId();
            return RestResult.ok(loggerGroupService.saveItems(groupId, req == null ? null : req.getItems()));
        } catch (Exception e) {
            return RestResult.error("保存分组条目失败: " + e.getMessage());
        }
    }

    @Mapping(value = "/loggerlevel/group/items/delete", method = MethodType.POST)
    public RestResult<Boolean> deleteItem(Long id) {
        try {
            return RestResult.ok(loggerGroupService.deleteItem(id));
        } catch (Exception e) {
            return RestResult.error("删除分组条目失败: " + e.getMessage());
        }
    }

    /**
     * 增量添加日志器到分组（已存在跳过，默认 level=INFO）
     * POST /solonapi/loggerlevel/group/items/add
     * body: { groupId, loggerNames: ["maximo.script", ...] }
     * 返回: { added, skipped, items }
     */
    @Mapping(value = "/loggerlevel/group/items/add", method = MethodType.POST)
    public RestResult<Map<String, Object>> addItems(@Body LoggerGroupAddReq req) {
        try {
            Long groupId = req == null ? null : req.getGroupId();
            return RestResult.ok(loggerGroupService.addItems(groupId, req == null ? null : req.getLoggerNames()));
        } catch (Exception e) {
            return RestResult.error("添加日志器到分组失败: " + e.getMessage());
        }
    }
}
