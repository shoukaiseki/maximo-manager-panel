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
        String[] createTableSqls = {
            """
                CREATE TABLE IF NOT EXISTS excel_import_scheme (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    scheme_name VARCHAR(200) NOT NULL COMMENT '配置方案名称',
                    xml_config LONGTEXT NOT NULL COMMENT 'jxls XML 配置内容',
                    description VARCHAR(500) DEFAULT '' COMMENT '方案描述',
                    sheet_name VARCHAR(100) DEFAULT 'Sheet1' COMMENT '工作表名称',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Excel 导入配置方案'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_project (
                    id VARCHAR(36) PRIMARY KEY COMMENT '项目ID(UUID)',
                    name VARCHAR(200) NOT NULL COMMENT '项目名称',
                    description VARCHAR(500) DEFAULT '' COMMENT '项目描述',
                    type VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '类型: global/user',
                    user_name VARCHAR(100) NOT NULL DEFAULT 'default' COMMENT '所属用户',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 项目'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_folder (
                    id VARCHAR(36) PRIMARY KEY COMMENT '文件夹ID(UUID)',
                    project_id VARCHAR(36) NOT NULL COMMENT '所属项目ID',
                    parent_id VARCHAR(36) DEFAULT NULL COMMENT '父文件夹ID',
                    name VARCHAR(200) NOT NULL COMMENT '文件夹名称',
                    sort_order INT DEFAULT 0 COMMENT '排序',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 文件夹'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_request (
                    id VARCHAR(36) PRIMARY KEY COMMENT '请求ID(UUID)',
                    project_id VARCHAR(36) NOT NULL COMMENT '所属项目ID',
                    folder_id VARCHAR(36) DEFAULT NULL COMMENT '所属文件夹ID',
                    name VARCHAR(200) NOT NULL COMMENT '请求名称',
                    method VARCHAR(10) NOT NULL DEFAULT 'GET' COMMENT 'HTTP方法',
                    url TEXT COMMENT '请求URL',
                    sort_order INT DEFAULT 0 COMMENT '排序',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 请求'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_request_param (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '参数ID',
                    request_id VARCHAR(36) NOT NULL COMMENT '所属请求ID',
                    param_key VARCHAR(200) NOT NULL COMMENT '参数名',
                    param_value TEXT COMMENT '参数值',
                    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 请求参数'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_request_header (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Header ID',
                    request_id VARCHAR(36) NOT NULL COMMENT '所属请求ID',
                    header_key VARCHAR(200) NOT NULL COMMENT 'Header名',
                    header_value TEXT COMMENT 'Header值',
                    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 请求Header'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_request_body (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Body ID',
                    request_id VARCHAR(36) NOT NULL COMMENT '所属请求ID',
                    body_type VARCHAR(20) NOT NULL DEFAULT 'none' COMMENT 'Body类型: none/form-data/urlencoded/json',
                    body_content LONGTEXT COMMENT 'Body内容(JSON文本)',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 请求Body'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_request_body_param (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Body参数ID',
                    body_id BIGINT NOT NULL COMMENT '所属Body ID',
                    param_key VARCHAR(200) NOT NULL COMMENT '参数名',
                    param_value TEXT COMMENT '参数值',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 请求Body参数(form-data/urlencoded)'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_environment (
                    id VARCHAR(36) PRIMARY KEY COMMENT '环境ID(UUID)',
                    name VARCHAR(100) NOT NULL COMMENT '环境名称',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 环境配置'
                """,
            """
                CREATE TABLE IF NOT EXISTS api_env_variable (
                    id VARCHAR(36) PRIMARY KEY COMMENT '变量ID(UUID)',
                    env_id VARCHAR(36) NOT NULL COMMENT '所属环境ID',
                    var_key VARCHAR(100) NOT NULL COMMENT '变量名',
                    var_value TEXT COMMENT '变量值',
                    var_value_type VARCHAR(20) DEFAULT 'default' COMMENT '值类型: default=直接值, system=系统预设',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 环境变量'
                """,
            """
                CREATE TABLE IF NOT EXISTS saved_query (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '查询ID',
                    app VARCHAR(50) NOT NULL COMMENT '所属应用(如 DOMAIN)',
                    queryname VARCHAR(200) NOT NULL COMMENT '查询名称',
                    whereclause TEXT COMMENT 'WHERE条件',
                    description VARCHAR(500) DEFAULT '' COMMENT '描述',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                    UNIQUE KEY uk_app_queryname (app, queryname)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保存的查询'
                """
        };

        try (HikariDataSource ds = new HikariDataSource(config)) {
            try (var conn = ds.getConnection(); var stmt = conn.createStatement()) {
                for (String sql : createTableSqls) {
                    stmt.execute(sql);
                }

                // 检查并添加 parent_id 列（兼容 MySQL 5.7+）
                try (var rs = stmt.executeQuery("SHOW COLUMNS FROM api_folder LIKE 'parent_id'")) {
                    if (!rs.next()) {
                        System.out.println("[MySQL] Adding parent_id column to api_folder...");
                        stmt.execute("ALTER TABLE api_folder ADD COLUMN parent_id VARCHAR(36) DEFAULT NULL COMMENT '父文件夹ID' AFTER project_id");
                    }
                }

                // 检查并添加 var_value_type 列
                try (var rs = stmt.executeQuery("SHOW COLUMNS FROM api_env_variable LIKE 'var_value_type'")) {
                    if (!rs.next()) {
                        System.out.println("[MySQL] Adding var_value_type column to api_env_variable...");
                        stmt.execute("ALTER TABLE api_env_variable ADD COLUMN var_value_type VARCHAR(20) DEFAULT 'default' COMMENT '值类型: default=直接值, system=系统预设' AFTER var_value");
                    }
                }

                System.out.println("[MySQL] All tables ensured (excel_import_scheme, api_project, etc.).");
            }
        } catch (Exception e) {
            System.err.println("[MySQL] Failed to ensure table: " + e.getMessage());
        }
    }
}
