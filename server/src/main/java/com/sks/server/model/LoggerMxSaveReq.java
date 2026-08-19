package com.sks.server.model;

import java.util.List;

/**
 * MXLogger 日志管理全量保存请求（前端树结构整体提交，服务端扁平化落库）
 */
public class LoggerMxSaveReq {

    /** 所属组ID；null 为旧模式（group_id IS NULL） */
    private Long groupId;
    private List<LoggerMxConfig> loggers;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public List<LoggerMxConfig> getLoggers() { return loggers; }
    public void setLoggers(List<LoggerMxConfig> loggers) { this.loggers = loggers; }
}