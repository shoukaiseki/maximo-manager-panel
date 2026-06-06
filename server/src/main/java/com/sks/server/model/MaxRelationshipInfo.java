package com.sks.server.model;

/**
 * MAXRELATIONSHIP 关系信息
 */
public class MaxRelationshipInfo {

    private String name;
    private String child;
    private String whereClause;
    private String cardinality;
    private String remarks;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getChild() { return child; }
    public void setChild(String child) { this.child = child; }
    public String getWhereClause() { return whereClause; }
    public void setWhereClause(String whereClause) { this.whereClause = whereClause; }
    public String getCardinality() { return cardinality; }
    public void setCardinality(String cardinality) { this.cardinality = cardinality; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}