package com.sks.server.model;

/**
 * 保存查询请求参数
 */
public class SavedQueryReq {

    private String app;
    private String queryname;
    private String whereclause;
    private String description;

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }
    public String getQueryname() { return queryname; }
    public void setQueryname(String queryname) { this.queryname = queryname; }
    public String getWhereclause() { return whereclause; }
    public void setWhereclause(String whereclause) { this.whereclause = whereclause; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
