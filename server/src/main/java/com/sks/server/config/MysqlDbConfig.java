package com.sks.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MySQL 数据源配置（用于存储 Excel 配置方案等业务数据）
 */
@Configuration
public class MysqlDbConfig {

    @Bean(value = "mysql", typed = true)
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Solon.cfg().get("mysql.url", "jdbc:mysql://localhost:3306/sks_mas_parent"));
        config.setUsername(Solon.cfg().get("mysql.username", "sks_mas_parent"));
        config.setPassword(Solon.cfg().get("mysql.password", "123456"));
        config.setDriverClassName(Solon.cfg().get("mysql.driverClassName", "com.mysql.cj.jdbc.Driver"));
        config.setMaximumPoolSize(Solon.cfg().getInt("mysql.maximumPoolSize", 5));
        config.setMinimumIdle(Solon.cfg().getInt("mysql.minimumIdle", 1));
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // 确保建表
        ensureTableExists(config);

        HikariDataSource ds = new HikariDataSource(config);
        System.out.println("[MySQL] DataSource initialized: " + Solon.cfg().get("mysql.url", ""));
        return ds;
    }

    private void ensureTableExists(HikariConfig config) {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS excel_import_scheme (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                scheme_name VARCHAR(200) NOT NULL COMMENT '配置方案名称',
                xml_config LONGTEXT NOT NULL COMMENT 'jxls XML 配置内容',
                description VARCHAR(500) DEFAULT '' COMMENT '方案描述',
                sheet_name VARCHAR(100) DEFAULT 'Sheet1' COMMENT '工作表名称',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 导入配置方案'
            """;

        try (HikariDataSource ds = new HikariDataSource(config)) {
            try (var stmt = ds.getConnection().createStatement()) {
                stmt.execute(createTableSql);
                System.out.println("[MySQL] Table 'excel_import_scheme' ensured.");
            }
        } catch (Exception e) {
            System.err.println("[MySQL] Failed to ensure table: " + e.getMessage());
        }
    }
}
