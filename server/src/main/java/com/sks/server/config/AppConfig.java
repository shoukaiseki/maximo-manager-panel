package com.sks.server.config;

import org.noear.solon.Solon;
import org.noear.solon.annotation.Component;

@Component
public class AppConfig {

    /**
     * 获取配置文件中的 apiKey
     */
    public String getApiKey() {
        return Solon.cfg().get("solon.apiKey", "");
    }
}