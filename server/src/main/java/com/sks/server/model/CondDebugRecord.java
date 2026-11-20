package com.sks.server.model;

/**
 * 条件表达式调试记录
 */
public class CondDebugRecord {

    private Long id;
    /** 记录名称（唯一） */
    private String recordName;
    /** MBO 对象名称 */
    private String mboName;
    /** MBO 记录过滤条件 */
    private String mboWhere;
    /** 条件表达式类型: WORKFLOW=工作流 / SIGOPTION=签名 */
    private String condType;
    /** 解析器类型: SYSTEM=系统 / CUSTOM=自定义SQL */
    private String parserType;
    /** 条件表达式内容 */
    private String expression;
    /** 附加输出字段（逗号分隔） */
    private String fields;
    /** 多条匹配时返回的候选条数 */
    private Integer maxRows;
    /** 描述 */
    private String description;
    private String createdAt;
    private String updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRecordName() { return recordName; }
    public void setRecordName(String recordName) { this.recordName = recordName; }
    public String getMboName() { return mboName; }
    public void setMboName(String mboName) { this.mboName = mboName; }
    public String getMboWhere() { return mboWhere; }
    public void setMboWhere(String mboWhere) { this.mboWhere = mboWhere; }
    public String getCondType() { return condType; }
    public void setCondType(String condType) { this.condType = condType; }
    public String getParserType() { return parserType; }
    public void setParserType(String parserType) { this.parserType = parserType; }
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    public String getFields() { return fields; }
    public void setFields(String fields) { this.fields = fields; }
    public Integer getMaxRows() { return maxRows; }
    public void setMaxRows(Integer maxRows) { this.maxRows = maxRows; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
