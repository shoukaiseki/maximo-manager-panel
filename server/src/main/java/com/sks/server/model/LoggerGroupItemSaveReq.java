package com.sks.server.model;

import java.util.List;

/**
 * 日志级别分组条目批量保存请求（全量覆盖）
 */
public class LoggerGroupItemSaveReq {

    private Long groupId;
    private List<LoggerLevelGroupItem> items;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public List<LoggerLevelGroupItem> getItems() { return items; }
    public void setItems(List<LoggerLevelGroupItem> items) { this.items = items; }
}
