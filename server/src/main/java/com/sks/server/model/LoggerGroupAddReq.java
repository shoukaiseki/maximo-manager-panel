package com.sks.server.model;

import java.util.List;

/**
 * 日志级别分组增量添加日志器请求（跨组添加/从查询添加）
 */
public class LoggerGroupAddReq {

    private Long groupId;
    private List<String> loggerNames;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public List<String> getLoggerNames() { return loggerNames; }
    public void setLoggerNames(List<String> loggerNames) { this.loggerNames = loggerNames; }
}
