package com.sks.server.model;

/**
 * 日志级别分组新建/更新请求
 */
public class LoggerGroupCreateReq {

    private String name;
    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
