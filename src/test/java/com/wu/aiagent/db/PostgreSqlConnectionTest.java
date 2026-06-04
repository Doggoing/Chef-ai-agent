package com.wu.aiagent.db;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL + MyBatis-Plus 连通性测试。
 * <p>
 * 前置条件：Docker 容器 postgres-pgvector 已启动，且已执行 sql/init_schema.sql。
 * <p>
 * 运行后在控制台 / 日志中搜索 {@code [DB-TEST]} 可看到汇总信息。
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class PostgreSqlConnectionTest {

    private static final String LOG_PREFIX = "[DB-TEST] ";

    // 数据库连接池对象
    @Autowired
    private DataSource dataSource;

    // 简化 JDBC 操作的工具
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // MyBatis 的核心工厂，用来创建 SqlSession
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /**
     * 汇总打印连接、库版本、扩展、业务表等信息（建议先看这个测试的输出）。
     */
    @Test
    void printDatabaseDiagnostics() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();

            String dbProduct = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
            String jdbcUrl = meta.getURL();
            String user = meta.getUserName();
            String driver = meta.getDriverName() + " " + meta.getDriverVersion();

            log.info("{}======== PostgreSQL 诊断信息 ========", LOG_PREFIX);
            log.info("{}数据库: {}", LOG_PREFIX, dbProduct);
            log.info("{}JDBC URL: {}", LOG_PREFIX, jdbcUrl);
            log.info("{}当前用户: {}", LOG_PREFIX, user);
            log.info("{}驱动: {}", LOG_PREFIX, driver);
            log.info("{}连接有效: {}", LOG_PREFIX, connection.isValid(3));

            if (dataSource instanceof HikariDataSource hikari) {
                log.info("{}连接池: {}, 活跃连接: {}, 空闲连接: {}, 总连接: {}",
                        LOG_PREFIX,
                        hikari.getPoolName(),
                        hikari.getHikariPoolMXBean().getActiveConnections(),
                        hikari.getHikariPoolMXBean().getIdleConnections(),
                        hikari.getHikariPoolMXBean().getTotalConnections());
            }

            String pgVersion = jdbcTemplate.queryForObject("SHOW server_version", String.class);
            log.info("{}server_version: {}", LOG_PREFIX, pgVersion);

            List<String> extensions = jdbcTemplate.queryForList(
                    "SELECT extname FROM pg_extension ORDER BY extname", String.class);
            log.info("{}已安装扩展: {}", LOG_PREFIX, extensions);

            List<String> tables = listPublicTables(meta);
            log.info("{}public  schema 表: {}", LOG_PREFIX, tables);

            log.info("{}MyBatis SqlSessionFactory: {}", LOG_PREFIX,
                    sqlSessionFactory != null ? "已就绪" : "未配置");
            log.info("{}======== 诊断结束 ========", LOG_PREFIX);

            // 同步打到标准输出，IDE「Run」窗口里更容易搜到
            System.out.println(LOG_PREFIX + "数据库连接成功 -> " + dbProduct);
            System.out.println(LOG_PREFIX + "JDBC URL -> " + jdbcUrl);
            System.out.println(LOG_PREFIX + "public 表 -> " + tables);
            System.out.println(LOG_PREFIX + "pgvector 扩展 -> "
                    + (extensions.contains("vector") ? "已安装" : "未安装"));
        }
    }

    @Test
    void dataSourceShouldConnect() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(connection.isValid(3));
            log.info("{}dataSourceShouldConnect 通过", LOG_PREFIX);
        }
    }

    @Test
    void selectOneShouldReturnOne() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result);
        log.info("{}selectOneShouldReturnOne 通过, SELECT 1 = {}", LOG_PREFIX, result);
    }

    @Test
    void mybatisPlusShouldBeConfigured() {
        assertNotNull(sqlSessionFactory);
        assertNotNull(sqlSessionFactory.getConfiguration());
        log.info("{}mybatisPlusShouldBeConfigured 通过, mappedStatements={}",
                LOG_PREFIX,
                sqlSessionFactory.getConfiguration().getMappedStatementNames().size());
    }

    @Test
    void pgvectorExtensionShouldExist() {
        String extName = jdbcTemplate.queryForObject(
                "SELECT extname FROM pg_extension WHERE extname = 'vector'",
                String.class
        );
        assertEquals("vector", extName);
        log.info("{}pgvectorExtensionShouldExist 通过, extname={}", LOG_PREFIX, extName);
    }

    @Test
    void businessTablesShouldExist() throws Exception {
        String[] expectedTables = {"sys_user", "chat_conversation", "chat_message"};
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String tableName : expectedTables) {
                assertTrue(tableExists(metaData, tableName),
                        "表不存在: " + tableName + "，请先在容器中执行 sql/init_schema.sql");
                log.info("{}businessTablesShouldExist 表存在: {}", LOG_PREFIX, tableName);
            }
        }
    }

    private List<String> listPublicTables(DatabaseMetaData metaData) throws Exception {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, "public", "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet rs = metaData.getTables(null, "public", tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
