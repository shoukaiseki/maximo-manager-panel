package com.sks.server.controller;

import com.sks.server.service.QueryDbService;
import org.noear.solon.annotation.*;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.handle.Context;

import java.util.List;
import java.util.Map;

@Controller
public class QueryDbController {

    @Inject
    private QueryDbService queryDbService;

    @Mapping(value = "/querydb/search", method = MethodType.GET)
    public void search(Context ctx, String keyword, @Param(defaultValue = "false") boolean exactMatch) {
        if (keyword == null || keyword.trim().isEmpty()) {
            ctx.status(400);
            ctx.output("keyword 参数不能为空");
            return;
        }

        ctx.contentType("text/event-stream");
        ctx.header("Cache-Control", "no-cache");
        ctx.header("Connection", "keep-alive");

        queryDbService.searchData(keyword.trim(), exactMatch, new QueryDbService.SearchCallback() {
            @Override
            public void onProgress(int current, int total) {
                try {
                    String json = "{\"type\":\"progress\",\"current\":" + current + ",\"total\":" + total + "}";
                    ctx.output("data: " + json + "\n\n");
                    ctx.flush();
                } catch (Exception e) {
                    // ignore
                }
            }

            @Override
            public void onResult(Map<String, Object> result) {
                try {
                    String json = "{\"type\":\"result\",\"data\":" + mapToJson(result) + "}";
                    ctx.output("data: " + json + "\n\n");
                    ctx.flush();
                } catch (Exception e) {
                    // ignore
                }
            }

            @Override
            public void onCompleted(long totalTime) {
                try {
                    String json = "{\"type\":\"completed\",\"totalTime\":" + totalTime + "}";
                    ctx.output("data: " + json + "\n\n");
                    ctx.flush();
                    ctx.close();
                } catch (Exception e) {
                    // ignore
                }
            }

            @Override
            public void onError(String error) {
                try {
                    String json = "{\"type\":\"error\",\"message\":\"" + escapeJson(error) + "\"}";
                    ctx.output("data: " + json + "\n\n");
                    ctx.flush();
                    ctx.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        });
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Number) {
                sb.append(value);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Mapping(value = "/querydb/fields", method = MethodType.GET)
    public List<Map<String, Object>> getFields() {
        return queryDbService.getSearchFields();
    }
}
