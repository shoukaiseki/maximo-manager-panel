package com.sks.server.model;

import java.util.List;

/**
 * 日志级别配置批量保存请求
 */
public class LoggerLevelSaveReq {

    private List<LoggerLevelConfig> loggers;

    public List<LoggerLevelConfig> getLoggers() { return loggers; }
    public void setLoggers(List<LoggerLevelConfig> loggers) { this.loggers = loggers; }
}
