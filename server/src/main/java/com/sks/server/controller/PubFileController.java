package com.sks.server.controller;

import com.sks.server.model.PubFile;
import com.sks.server.model.RestResult;
import com.sks.server.service.PubFileService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.handle.UploadedFile;

import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 公开文件管理 API。
 * <p>
 * 上传 /api/pubfiles/upload 由 ApiKeyFilter 统一校验 X-API-Key；
 * 列表 /api/pubfiles/list 与文件访问 /pubfiles/{storedName} 无需认证；
 * 文件的公开 URL 由后端按 pubfile.url-prefix 配置生成（图片链接可直接引用）。
 */
@Controller
public class PubFileController {

    @Inject
    private PubFileService pubFileService;

    /**
     * 上传文件
     * POST /solonapi/api/pubfiles/upload  (multipart: file)
     */
    @Mapping(value = "/api/pubfiles/upload", method = MethodType.POST)
    public RestResult<PubFile> upload(UploadedFile file) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请选择要上传的文件");
        }
        try {
            return RestResult.ok(pubFileService.save(file));
        } catch (Exception e) {
            return RestResult.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 文件列表（无需认证，走 /api/anon/ 前缀）
     * GET /solonapi/api/anon/pubfiles/list
     */
    @Mapping(value = "/api/anon/pubfiles/list", method = MethodType.GET)
    public RestResult<List<PubFile>> list() {
        try {
            return RestResult.ok(pubFileService.list());
        } catch (Exception e) {
            return RestResult.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 公开访问文件（无需认证，图片链接/下载直接用）
     * GET /api/anon/pubfiles/{storedName}?_action=preview   预览（默认，按扩展名内联返回）
     * GET /api/anon/pubfiles/{storedName}?_action=download  下载（application/octet-stream + attachment）
     */
    @Mapping(value = "/api/anon/pubfiles/{storedName}", method = MethodType.GET)
    public void preview(String storedName, @Param("_action") String action) throws Exception {
        Context ctx = Context.current();
        Path path = pubFileService.resolve(storedName);
        if (path == null) {
            ctx.status(404);
            ctx.contentType("text/plain;charset=UTF-8");
            ctx.output("File not found");
            return;
        }
        switch (action == null ? "" : action.toLowerCase()) {
            case "download":
                // 下载：强制以附件下载，文件名用 URLEncoder 编码（兼容中文/空格）
                ctx.contentType("application/octet-stream");
                ctx.headerSet("Content-Disposition",
                        "attachment; filename=\"" + URLEncoder.encode(storedName, "UTF-8").replace("+", "%20") + "\"");
                break;
            case "preview":
            default:
                // 预览（_action=preview 或其它值/未传）：按扩展名内联返回
                ctx.contentType(pubFileService.contentTypeOf(storedName));
        }
        ctx.headerSet("Content-Length", String.valueOf(Files.size(path)));
        Files.copy(path, ctx.outputStream());
    }
}
