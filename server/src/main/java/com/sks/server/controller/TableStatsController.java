package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.TableStatsService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;

import java.util.*;

/**
 * 表数据统计接口
 */
@Controller
public class TableStatsController {

    @Inject
    private TableStatsService tableStatsService;

    /**
     * 表数据统计列表
     * GET /solonapi/tablestats/list
     * 返回全部表的表名/英文描述/中文描述/数据行数(COUNT, 统计失败为 null)
     * 排序在前端本地完成（切换排序方式不重新统计）
     */
    @Mapping(value = "/tablestats/list", method = MethodType.GET)
    public RestResult<List<Map<String, Object>>> list() {
        try {
            List<Map<String, Object>> rows = tableStatsService.listTables();
            List<String> names = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                names.add((String) row.get("OBJECTNAME"));
            }
            Map<String, Long> counts = tableStatsService.countAllTables(names);
            for (Map<String, Object> row : rows) {
                Long cnt = counts.get((String) row.get("OBJECTNAME"));
                row.put("COUNT", cnt == null ? null : cnt);
            }
            return RestResult.ok(rows);
        } catch (Exception e) {
            return RestResult.error("查询表数据统计失败: " + e.getMessage());
        }
    }
}