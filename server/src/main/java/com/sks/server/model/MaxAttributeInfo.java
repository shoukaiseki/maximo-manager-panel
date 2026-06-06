package com.sks.server.model;

/**
 * MAXATTRIBUTE 属性信息
 */
public class MaxAttributeInfo {

    private String attributeName;
    private Integer attributeNo;
    private String domainId;
    private String isPositive;
    private Integer length;
    private String maxType;
    private String required;
    private Integer primaryKeyColSeq;
    private String sameAsObject;
    private Integer scale;
    private String title;
    private String remarks;

    public String getAttributeName() { return attributeName; }
    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }
    public Integer getAttributeNo() { return attributeNo; }
    public void setAttributeNo(Integer attributeNo) { this.attributeNo = attributeNo; }
    public String getDomainId() { return domainId; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public String getIsPositive() { return isPositive; }
    public void setIsPositive(String isPositive) { this.isPositive = isPositive; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public String getMaxType() { return maxType; }
    public void setMaxType(String maxType) { this.maxType = maxType; }
    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }
    public Integer getPrimaryKeyColSeq() { return primaryKeyColSeq; }
    public void setPrimaryKeyColSeq(Integer primaryKeyColSeq) { this.primaryKeyColSeq = primaryKeyColSeq; }
    public String getSameAsObject() { return sameAsObject; }
    public void setSameAsObject(String sameAsObject) { this.sameAsObject = sameAsObject; }
    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}