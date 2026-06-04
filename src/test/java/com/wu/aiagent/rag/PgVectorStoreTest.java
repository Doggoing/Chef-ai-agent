package com.wu.aiagent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PgVector RAG 向量库测试。
 * <p>
 * 前置条件：Docker PostgreSQL（pgvector）已启动。
 * 测试 profile 下 {@code rag.skip-startup-load=true}，不会调用 embedding API。
 */
@SpringBootTest
@ActiveProfiles("test")
class PgVectorStoreTest {

    @Autowired
    private VectorStore chefVectorStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void chefVectorStoreShouldBePgVectorStore() {
        assertNotNull(chefVectorStore);
        assertInstanceOf(PgVectorStore.class, chefVectorStore);
    }

    @Test
    void vectorStoreTableShouldExist() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'vector_store'
                """,
                Integer.class
        );
        assertEquals(1, tableCount);
    }

    @Test
    void pgvectorExtensionShouldExist() {
        String extName = jdbcTemplate.queryForObject(
                "SELECT extname FROM pg_extension WHERE extname = 'vector'",
                String.class
        );
        assertEquals("vector", extName);
    }
}
