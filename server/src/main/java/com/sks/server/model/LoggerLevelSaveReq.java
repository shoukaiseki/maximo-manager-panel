package com.sks.server.model;

import java.util.List;

/**
 * 日志级别配置批量保存请求
 */
public class LoggerLevelSaveReq {

    private Long groupId; // 非空时同时导入到该分组（级别取 JSON 中的级别）

    private List<LoggerLevelConfig> loggers;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public List<LoggerLevelConfig> getLoggers() { return loggers; }
    public void setLoggers(List<LoggerLevelConfig> loggers) { this.loggers = loggers; }
}
