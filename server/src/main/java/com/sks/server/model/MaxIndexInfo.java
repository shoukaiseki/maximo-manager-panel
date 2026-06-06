package com.sks.server.model;

import java.util.List;

/**
 * MAXSYSINDEXES 索引信息
 */
public class MaxIndexInfo {

    private String name;
    private String tbName;
    private String uniqueRule;
    private List<String> columns;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTbName() { return tbName; }
    public void setTbName(String tbName) { this.tbName = tbName; }
    public String getUniqueRule() { return uniqueRule; }
    public void setUniqueRule(String uniqueRule) { this.uniqueRule = uniqueRule; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
}