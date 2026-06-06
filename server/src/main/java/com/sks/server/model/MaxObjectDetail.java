package com.sks.server.model;

import java.util.List;
import java.util.Map;

/**
 * MAXOBJECT 完整详情（主信息 + 属性 + 关系 + 索引）
 */
public class MaxObjectDetail {

    private Map<String, Object> mainInfo;
    private List<Map<String, Object>> attributes;
    private List<Map<String, Object>> relationships;
    private List<Map<String, Object>> indexes;

    public Map<String, Object> getMainInfo() { return mainInfo; }
    public void setMainInfo(Map<String, Object> mainInfo) { this.mainInfo = mainInfo; }
    public List<Map<String, Object>> getAttributes() { return attributes; }
    public void setAttributes(List<Map<String, Object>> attributes) { this.attributes = attributes; }
    public List<Map<String, Object>> getRelationships() { return relationships; }
    public void setRelationships(List<Map<String, Object>> relationships) { this.relationships = relationships; }
    public List<Map<String, Object>> getIndexes() { return indexes; }
    public void setIndexes(List<Map<String, Object>> indexes) { this.indexes = indexes; }
}