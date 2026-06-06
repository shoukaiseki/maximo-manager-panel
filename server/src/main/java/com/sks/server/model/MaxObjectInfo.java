package com.sks.server.model;

/**
 * MAXOBJECT 列表查询结果
 */
public class MaxObjectInfo {

    private String objectName;
    private String description;
    private String descriptionCn;

    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDescriptionCn() { return descriptionCn; }
    public void setDescriptionCn(String descriptionCn) { this.descriptionCn = descriptionCn; }
}