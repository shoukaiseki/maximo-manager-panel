package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MessageImportService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.handle.UploadedFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * 多语言 Excel 导入 API。
 * <p>
 * 上传后文件仅缓存在服务端内存中，返回 sessionId 与 Excel 前 10 行；
 * 确认更新时按 sessionId 和导入目标重新读取对应的工作表。
 */
@Controller
public class MessageImportController {

    @Inject
    private MessageImportService messageImportService;

    /**
     * 查询已启用语言、基准语言及后台默认语言
     * GET /solonapi/system/languages
     */
    @Mapping(value = "/system/languages", method = MethodType.GET)
    public RestResult<Map<String, Object>> languages() {
        try {
            return RestResult.ok(messageImportService.queryLanguages());
        } catch (Exception e) {
            return RestResult.error("查询支持的语言失败: " + e.getMessage());
        }
    }

    /**
     * 上传预览：读取 Excel 前 10 行并生成文件缓存会话
     * POST /solonapi/messageimport/preview (multipart: file, trim, attribute, target)
     */
    @Mapping(value = "/messageimport/preview", method = MethodType.POST)
    public RestResult<Map<String, Object>> preview(
            UploadedFile file,
            @Param(defaultValue = "false") boolean trim,
            @Param(defaultValue = "false") boolean attribute,
            String target) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传 Excel 文件");
        }
        String fileName = file.getName() != null ? file.getName() : "";
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".xls") && !lower.endsWith(".xlsx")) {
            return RestResult.error("请上传 .xls 或 .xlsx 格式的 Excel 文件");
        }

        try {
            byte[] bytes = readAll(file.getContent());
            return RestResult.ok(messageImportService.preview(bytes, fileName, trim, attribute, target));
        } catch (IllegalArgumentException e) {
            return RestResult.error(e.getMessage());
        } catch (Exception e) {
            return RestResult.error("Excel 解析失败: " + e.getMessage());
        }
    }

    /**
     * 确认更新：按 sessionId 读取缓存文件，仅更新勾选的语言
     * POST /solonapi/messageimport/execute?sessionId=&trim=&langcodes=EN,JA&attribute=&target=
     */
    @Mapping(value = "/messageimport/execute", method = MethodType.POST)
    public RestResult<Map<String, Object>> execute(
            String sessionId,
            @Param(defaultValue = "false") boolean trim,
            String langcodes,
            @Param(defaultValue = "false") boolean attribute,
            String target) {
        try {
            return RestResult.ok(messageImportService.execute(sessionId, trim, langcodes, attribute, target));
        } catch (IllegalArgumentException e) {
            return RestResult.error(e.getMessage());
        } catch (Exception e) {
            return RestResult.error("执行更新失败: " + e.getMessage());
        }
    }

    private byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
