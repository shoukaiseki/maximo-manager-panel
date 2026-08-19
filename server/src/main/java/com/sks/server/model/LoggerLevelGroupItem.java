package com.sks.server.model;

/**
 * 日志级别分组条目（各组独立存 level/ignored）
 */
public class LoggerLevelGroupItem {

    private Long id;
    private Long groupId;
    private String loggerName;
    private String level;
    private Boolean ignored;
    private String description;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getIgnored() { return ignored; }
    public void setIgnored(Boolean ignored) { this.ignored = ignored; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
