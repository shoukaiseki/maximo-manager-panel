package com.sks.server.model;

/**
 * MXLogger 日志管理组（先建组，再在组下添加节点树）
 */
public class LoggerMxGroup {

    private Long id;
    private String name;
    private String description;

    public LoggerMxGroup() {
    }

    public LoggerMxGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}