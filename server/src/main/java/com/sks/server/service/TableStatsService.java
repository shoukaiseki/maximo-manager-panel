package com.sks.server.service;

import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 表数据统计服务
 * 基于 MAXOBJECT 获取所有持久化且非视图的表名，再逐个表统计行数
 */
@Component
public class TableStatsService {

    @Inject
    private DataSource dataSource;

    /**
     * 表列表 SQL（任务给定）：
     * SELECT OBJECTNAME, MAXOBJECT.DESCRIPTION, lzh.DESCRIPTION AS LZH_DESCRIPTION
     * FROM MAXOBJECT LEFT JOIN L_MAXOBJECT lzh ... WHERE PERSISTENT=1 AND NOT EXISTS(MAXVIEW)
     */
    private static final String TABLE_LIST_SQL =
            "SELECT OBJECTNAME, MAXOBJECT.DESCRIPTION, lzh.DESCRIPTION AS LZH_DESCRIPTION " +
            "FROM MAXOBJECT " +
            "LEFT JOIN L_MAXOBJECT AS lzh ON (lzh.OWNERID = MAXOBJECT.MAXOBJECTID AND lzh.LANGCODE = 'ZH') " +
            "WHERE 1=1 AND PERSISTENT = 1 " +
            "AND NOT EXISTS (SELECT 1 FROM MAXVIEW WHERE MAXVIEW.VIEWNAME = MAXOBJECT.OBJECTNAME) ";

    /**
     * 查询所有待统计的表（含英文/中文描述）
     * @param where 自定义 where 条件（不含 WHERE 关键字），为空则返回全部表
     */
    public List<Map<String, Object>> listTables(String where) {
        StringBuilder sb = new StringBuilder(TABLE_LIST_SQL);
        if (where != null && !where.trim().isEmpty()) {
            sb.append(" AND (").append(where.trim()).append(") ");
        }
        sb.append("ORDER BY OBJECTNAME");
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("OBJECTNAME", rs.getString("OBJECTNAME"));
                row.put("DESCRIPTION", rs.getString("DESCRIPTION"));
                row.put("LZH_DESCRIPTION", rs.getString("LZH_DESCRIPTION"));
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询 MAXOBJECT 表列表失败: " + e.getMessage(), e);
        }
        return rows;
    }

    /**
     * 统计所有表的行数
     * 采用「分批 UNION ALL + 并发批次」策略：将多张表合并为单条 COUNT SQL，
     * 每条只执行一次即可得到多张表的行数；批次并行执行。
     * 计数数为 -1 的表（不存在/无权限）返回 null。
     */
    private static final int COUNT_BATCH_SIZE = 16;

    private class RowCountTask implements Callable<Map<String, Long>> {
        private final List<String> batch;

        RowCountTask(List<String> batch) {
            this.batch = batch;
        }

        @Override
        public Map<String, Long> call() {
            StringBuilder sql = new StringBuilder("SELECT RTRIM(TNAME) TNAME, CNT FROM (");
            for (int i = 0, size = batch.size(); i < size; i++) {
                if (i > 0) sql.append(" UNION ALL ");
                sql.append("SELECT '").append(batch.get(i)).append("' AS TNAME, COUNT(*) AS CNT FROM ").append(batch.get(i));
            }
            sql.append(") TMP");

            Map<String, Long> counts = new HashMap<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("TNAME"), rs.getLong("CNT"));
                }
            } catch (SQLException e) {
                // 批次整体失败（任一表异常）时逐表降级重试
                for (String name : batch) {
                    counts.put(name, countTableRows(name));
                }
            }
            return counts;
        }
    }

    /**
     * 统计指定表的行数，失败（不存在/无权限）返回 -1
     */
    public long countTableRows(String tableName) {
        String sql = "SELECT COUNT(*) AS CNT FROM " + tableName.toUpperCase();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("CNT");
            }
        } catch (SQLException e) {
            return -1;
        }
        return -1;
    }

    /**
     * 统计所有表的行数（分批 UNION ALL + 并发批次）
     * 计数失败（不存在/无权限）的表行数为 null
     */
    public Map<String, Long> countAllTables(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyMap();
        }
        // 并发度取批次数量与连接池上限(Hikari maximumPoolSize=10)的较小值，避免连接耗尽
        int poolSize = Math.max(1, 10);
        int taskCount = (tableNames.size() + COUNT_BATCH_SIZE - 1) / COUNT_BATCH_SIZE;
        ExecutorService pool = null;
        try {
            pool = Executors.newFixedThreadPool(Math.min(taskCount, poolSize));
            List<Future<Map<String, Long>>> futures = new ArrayList<>();
            for (int i = 0; i < tableNames.size(); i += COUNT_BATCH_SIZE) {
                List<String> batch = tableNames.subList(i, Math.min(i + COUNT_BATCH_SIZE, tableNames.size()));
                futures.add(pool.submit(new RowCountTask(batch)));
            }
            Map<String, Long> counts = new HashMap<>();
            for (Future<Map<String, Long>> f : futures) {
                counts.putAll(f.get());
            }
            return counts;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("统计表行数失败: " + e.getMessage(), e);
        } finally {
            if (pool != null) {
                pool.shutdown();
            }
        }
    }
}