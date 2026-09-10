package com.sks.server.controller;

import com.sks.server.model.RestResult;
import com.sks.server.service.MafAppDataService;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.handle.UploadedFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Map;

/**
 * MAFAPPDATA（graphite 移动端应用包）管理 API。
 * 直接操作 DB2 MAXIMO.MAFAPPDATA 表。
 */
@Controller
public class MafAppDataController {

    @Inject
    private MafAppDataService mafAppDataService;

    /**
     * 分页列表（不返回 APP BLOB）
     * GET /solonapi/mafappdata/list?appid=&status=&ismobile=&pageNum=1&pageSize=20
     */
    @Mapping(value = "/mafappdata/list", method = MethodType.GET)
    public RestResult<Map<String, Object>> list(
            String appid, String status, String ismobile,
            @Param(defaultValue = "1") int pageNum,
            @Param(defaultValue = "20") int pageSize) {
        return RestResult.ok(mafAppDataService.queryList(appid, status, ismobile, pageNum, pageSize));
    }

    /**
     * 详情（不含 BLOB，含 APPLEN）
     * GET /solonapi/mafappdata/detail?id=58
     */
    @Mapping(value = "/mafappdata/detail", method = MethodType.GET)
    public RestResult<Map<String, Object>> detail(long id) {
        try {
            return RestResult.ok(mafAppDataService.queryDetail(id));
        } catch (Exception e) {
            return RestResult.error(e.getMessage());
        }
    }

    /**
     * 分析上传 zip：校验合法性并尝试读取 build.json 提取 appid/version
     * POST /solonapi/mafappdata/analyze  (multipart: file)
     */
    @Mapping(value = "/mafappdata/analyze", method = MethodType.POST)
    public RestResult<Map<String, Object>> analyze(UploadedFile file) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传应用包文件(.zip/.apkg)");
        }
        try {
            byte[] bytes = readAll(file.getContent());
            Map<String, Object> result = mafAppDataService.analyze(bytes, file.getName());
            return RestResult.ok(result);
        } catch (Exception e) {
            return RestResult.error("分析失败: " + e.getMessage());
        }
    }

    /**
     * 导入应用包（.apkg 完整包 或 原始 graphite zip + 表单元数据）
     * POST /solonapi/mafappdata/import  (multipart: file, appid, version, appmode, status, ismobile, revision, deployby)
     */
    @Mapping(value = "/mafappdata/import", method = MethodType.POST)
    public RestResult<Map<String, Object>> importApp(
            UploadedFile file,
            String appid, String version, String appmode, String status,
            String ismobile, Integer revision, String deployby,
            Long id, Boolean updateOnly) {
        if (file == null || file.getContent() == null) {
            return RestResult.error("请上传应用包文件(.zip/.apkg)");
        }
        try {
            byte[] bytes = readAll(file.getContent());
            Map<String, Object> result = mafAppDataService.importApp(
                    bytes, file.getName(), appid, version, appmode, status, ismobile, revision, deployby,
                    id, updateOnly);
            return RestResult.ok(result);
        } catch (Exception e) {
            return RestResult.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 导出：单条直接输出 APP BLOB（生产 zip，命名 {APPID}__ver-{VERSION}.zip）；多条外套 zip。
     * GET /solonapi/mafappdata/export?ids=58  或  ids=58,57,56
     */
    @Mapping(value = "/mafappdata/export", method = MethodType.GET)
    public void export(String ids) throws Exception {
        String fileName = mafAppDataService.buildExportFileName(ids);
        String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        Context ctx = Context.current();
        ctx.contentType("application/octet-stream");
        ctx.headerSet("Content-Disposition",
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encoded);
        mafAppDataService.writeExport(ids, ctx.outputStream());
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
