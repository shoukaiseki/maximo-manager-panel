package com.sks.server.model;

import java.util.ArrayList;
import java.util.List;

/**
 * MXLogger 日志管理配置项（层级最多一层子级，树结构由前端组装/后端 list 时组装）
 * logkey 不落库，由 logkey 生成规则推导：顶层 log4j.logger.maximo.&lt;logger&gt;，子级追加 .&lt;子logger&gt;
 */
public class LoggerMxConfig {

    private Long id;
    private Long groupId;
    private Long parentId;
    private String logger;
    private String level;
    private Boolean active;
    private String remark;
    private Integer sortOrder;
    private List<LoggerMxConfig> children = new ArrayList<>();

    public LoggerMxConfig() {
    }

    public LoggerMxConfig(String logger, String level, Boolean active, String remark, Integer sortOrder) {
        this.logger = logger;
        this.level = level;
        this.active = active;
        this.remark = remark;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<LoggerMxConfig> getChildren() { return children; }
    public void setChildren(List<LoggerMxConfig> children) { this.children = children; }

    public void addChild(LoggerMxConfig child) { this.children.add(child); }
}