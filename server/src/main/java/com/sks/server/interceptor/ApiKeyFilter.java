package com.sks.server.interceptor;

import com.sks.server.config.AppConfig;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

@Component
public class ApiKeyFilter implements Filter {

    @Inject
    private AppConfig appConfig;

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        String path = ctx.path();

        // 健康检查接口不需要 apiKey
        if ("/api/health".equals(path)) {
            chain.doFilter(ctx);
            return;
        }

        // 验证 apiKey（支持从请求头或 URL 参数获取）
        String apiKey = ctx.header("X-API-Key");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = ctx.param("apiKey");
        }
        String expectedApiKey = appConfig.getApiKey();

        if (expectedApiKey.isEmpty()) {
            ctx.status(500);
            ctx.contentType("application/json");
            ctx.output("{\"code\":500,\"message\":\"Server configuration error: apiKey not set. Please configure 'solon.apiKey' in app.yml\"}");
            return;
        }

        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            ctx.status(401);
            ctx.contentType("application/json");
            ctx.output("{\"code\":401,\"message\":\"Invalid or missing API Key. Please check X-API-Key header and app.yml configuration\"}");
            return;
        }

        chain.doFilter(ctx);
    }
}