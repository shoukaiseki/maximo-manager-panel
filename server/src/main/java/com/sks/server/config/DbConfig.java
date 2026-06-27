package com.sks.server.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.p6spy.engine.spy.P6DataSource;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DbConfig {

    @Bean(value = "db2", typed = true)
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Solon.cfg().get("db2.url", "jdbc:db2://localhost:50000/MAXDB"));
        config.setUsername(Solon.cfg().get("db2.username", "db2admin"));
        config.setPassword(Solon.cfg().get("db2.password", "db2admin"));
        config.setDriverClassName(Solon.cfg().get("db2.driverClassName", "com.ibm.db2.jcc.DB2Driver"));
        config.setMaximumPoolSize(Solon.cfg().getInt("db2.maximumPoolSize", 10));
        config.setMinimumIdle(Solon.cfg().getInt("db2.minimumIdle", 2));
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // 设置 DB2 schema（Maximo 表通常在 maximo schema 下）
        String schema = Solon.cfg().get("db2.schema", "");
        if (!schema.isEmpty()) {
            config.setConnectionInitSql("SET SCHEMA " + schema);
            System.out.println("[DB2] Using schema: " + schema);
        }

        HikariDataSource ds = new HikariDataSource(config);
        System.out.println("[DB2] DataSource initialized: " + Solon.cfg().get("db2.url", ""));

        // 用 P6DataSource 包装，开启 SQL 日志打印
        return new P6DataSource(ds);
    }
}