package com.sks.server.config;

import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;

import java.io.File;

@Component
public class AppConfig {

    /**
     * 获取配置文件中的 apiKey
     */
    public String getApiKey() {
        return Solon.cfg().get("solon.apiKey", "");
    }

    /**
     * 公开文件存储目录（默认 user.home/maximo-panel/pubfiles）
     */
    public String getPubFileDocPath() {
        String path = Solon.cfg().get("pubfile.docpath", "");
        if (path == null || path.isEmpty()) {
            path = System.getProperty("user.home") + File.separator + "maximo-panel" + File.separator + "pubfiles";
        }
        return path;
    }

    /**
     * 公开文件访问 URL 前缀（后端据此拼出返回给前端的文件 url；留空则返回相对路径 /pubfiles/xxx）
     */
    public String getPubFileUrlPrefix() {
        return Solon.cfg().get("pubfile.url-prefix", "");
    }
}