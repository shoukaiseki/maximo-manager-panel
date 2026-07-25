package com.sks.server.model;

import java.util.List;
import java.util.Map;

/**
 * Excel 预览结果
 */
public class ExcelPreviewResult {

    /** 解析成功的数据行数 */
    private int rowCount;
    /** 列数 */
    private int columnCount;
    /** 解析后的 JSON 数据（按 Sheet 分组） */
    private Map<String, Object> data;
    /** 数据列名列表 */
    private List<String> columnNames;
    /** Sheet 名称列表 */
    private List<String> sheetNames;
    /** 当前使用的 Sheet 名称 */
    private String sheetName;
    /** 解析状态信息 */
    private String statusInfo;

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }
    public int getColumnCount() { return columnCount; }
    public void setColumnCount(int columnCount) { this.columnCount = columnCount; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public List<String> getColumnNames() { return columnNames; }
    public void setColumnNames(List<String> columnNames) { this.columnNames = columnNames; }
    public List<String> getSheetNames() { return sheetNames; }
    public void setSheetNames(List<String> sheetNames) { this.sheetNames = sheetNames; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getStatusInfo() { return statusInfo; }
    public void setStatusInfo(String statusInfo) { this.statusInfo = statusInfo; }
}
