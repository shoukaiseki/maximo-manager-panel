package com.sks.server.service;

import com.sks.server.model.ExcelImportScheme;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 配置方案管理服务
 */
@Component
public class ConfigSchemeService {

    @Inject("mysql")
    private DataSource mysqlDataSource;

    /**
     * 获取所有配置方案列表
     */
    public List<ExcelImportScheme> listSchemes() {
        String sql = "SELECT id, scheme_name, xml_config, description, sheet_name, created_at, updated_at " +
                     "FROM excel_import_scheme ORDER BY updated_at DESC";
        List<ExcelImportScheme> list = new ArrayList<>();

        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapScheme(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("查询配置方案列表失败", e);
        }
        return list;
    }

    /**
     * 根据 ID 获取配置方案
     */
    public ExcelImportScheme getScheme(Long id) {
        String sql = "SELECT id, scheme_name, xml_config, description, sheet_name, created_at, updated_at " +
                     "FROM excel_import_scheme WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapScheme(rs);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("查询配置方案失败", e);
        }
        return null;
    }

    /**
     * 保存配置方案（新增）
     */
    public ExcelImportScheme saveScheme(ExcelImportScheme scheme) {
        String sql = "INSERT INTO excel_import_scheme (scheme_name, xml_config, description, sheet_name) VALUES (?, ?, ?, ?)";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, scheme.getSchemeName());
            ps.setString(2, scheme.getXmlConfig());
            ps.setString(3, scheme.getDescription() != null ? scheme.getDescription() : "");
            ps.setString(4, scheme.getSheetName() != null ? scheme.getSheetName() : "Sheet1");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    scheme.setId(rs.getLong(1));
                }
            }
            scheme.setCreatedAt(LocalDateTime.now());
            scheme.setUpdatedAt(LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException("保存配置方案失败", e);
        }
        return scheme;
    }

    /**
     * 更新配置方案
     */
    public ExcelImportScheme updateScheme(Long id, ExcelImportScheme scheme) {
        String sql = "UPDATE excel_import_scheme SET scheme_name = ?, xml_config = ?, description = ?, sheet_name = ? WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, scheme.getSchemeName());
            ps.setString(2, scheme.getXmlConfig());
            ps.setString(3, scheme.getDescription() != null ? scheme.getDescription() : "");
            ps.setString(4, scheme.getSheetName() != null ? scheme.getSheetName() : "Sheet1");
            ps.setLong(5, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("更新配置方案失败", e);
        }
        return getScheme(id);
    }

    /**
     * 删除配置方案
     */
    public void deleteScheme(Long id) {
        String sql = "DELETE FROM excel_import_scheme WHERE id = ?";
        try (Connection conn = mysqlDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("删除配置方案失败", e);
        }
    }

    private ExcelImportScheme mapScheme(ResultSet rs) throws SQLException {
        ExcelImportScheme scheme = new ExcelImportScheme();
        scheme.setId(rs.getLong("id"));
        scheme.setSchemeName(rs.getString("scheme_name"));
        scheme.setXmlConfig(rs.getString("xml_config"));
        scheme.setDescription(rs.getString("description"));
        scheme.setSheetName(rs.getString("sheet_name"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) scheme.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) scheme.setUpdatedAt(ua.toLocalDateTime());
        return scheme;
    }
}
