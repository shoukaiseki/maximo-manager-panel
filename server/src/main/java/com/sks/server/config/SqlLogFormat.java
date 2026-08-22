package com.sks.server.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * p6spy SQL 日志格式：原始SQL与实际SQL各占一行，便于复制实际执行语句
 * 第一行: 时间戳|耗时|类别|连接|url|原始SQL(带?占位符)
 * 第二行: 仅实际SQL(已代入参数, 无前缀, 便于直接复制)
 */
public class SqlLogFormat implements MessageFormattingStrategy {
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        String prefix = now + "|" + elapsed + "|" + category + "|connection " + connectionId + "|url " + url;
        StringBuilder sb = new StringBuilder();
        if (prepared != null && !prepared.isEmpty()) {
            sb.append(prefix).append("|").append(prepared);
        }
        if (sql != null && !sql.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(sql);
        }
        return sb.toString();
    }
}
