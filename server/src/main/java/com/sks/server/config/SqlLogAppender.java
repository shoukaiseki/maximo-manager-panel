package com.sks.server.config;

import com.p6spy.engine.spy.appender.StdoutLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * p6spy SQL 日志输出：控制台 + 文件（${user.home}/logs/sql.log）
 */
public class SqlLogAppender extends StdoutLogger {
    private static final PrintWriter FILE_WRITER;

    static {
        PrintWriter w = null;
        try {
            File dir = new File(System.getProperty("user.home"), "logs");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            w = new PrintWriter(new FileWriter(new File(dir, "sql.log"), true), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FILE_WRITER = w;
    }

    @Override
    public void logText(String text) {
        super.logText(text); // 控制台
        if (FILE_WRITER != null) {
            FILE_WRITER.println(text);
        }
    }
}
