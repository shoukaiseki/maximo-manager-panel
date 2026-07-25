package com.sks.server.model;

import java.time.LocalDateTime;

/**
 * Excel 导入配置方案
 */
public class ExcelImportScheme {

    private Long id;
    private String schemeName;
    private String xmlConfig;
    private String description;
    private String sheetName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getXmlConfig() { return xmlConfig; }
    public void setXmlConfig(String xmlConfig) { this.xmlConfig = xmlConfig; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
