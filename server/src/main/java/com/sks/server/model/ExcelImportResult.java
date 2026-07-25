package com.sks.server.model;

/**
 * Excel 导入执行结果
 */
public class ExcelImportResult {

    /** 总行数 */
    private int totalRows;
    /** 成功行数 */
    private int successRows;
    /** 失败行数 */
    private int failedRows;
    /** 错误信息 */
    private String errorMessage;
    /** 是否成功 */
    private boolean success;

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getSuccessRows() { return successRows; }
    public void setSuccessRows(int successRows) { this.successRows = successRows; }
    public int getFailedRows() { return failedRows; }
    public void setFailedRows(int failedRows) { this.failedRows = failedRows; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
