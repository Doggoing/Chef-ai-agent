package com.wu.aiagent.rag;

import com.wu.aiagent.app.ChefApp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RAG 全流程集成测试（在测试类里显式跑通整条链路）。
 * <p>
 * 流程：加载 Markdown → 关键词增强 → Embedding 写入 PgVector → 用户问题 → 检索增强 → 大模型回答
 * <p>
 * 前置条件：
 * <ul>
 *   <li>PostgreSQL（pgvector）容器已启动</li>
 *   <li>{@code application-local.yml} 中有效的百炼 api-key（profile 含 local）</li>
 * </ul>
 * <p>
 * 运行：{@code mvn test -Dtest=ChefRagPipelineTest}
 */
@Slf4j
@SpringBootTest
@ActiveProfiles({"local", "test"})
@TestPropertySource(properties = {
        "rag.skip-startup-load=true",
        "auth.enabled=false"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChefRagPipelineTest {

    @Resource
    private ChefDocumentLoader chefDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private VectorStore chefVectorStore;

    @Resource
    private ChefApp chefApp;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private VectorStoreDocumentWriter vectorStoreDocumentWriter;

    @BeforeEach
    void resetVectorStore() {
        assertInstanceOf(PgVectorStore.class, chefVectorStore);
        jdbcTemplate.execute("TRUNCATE TABLE vector_store");
        log.info("[RAG-TEST] ① 已清空 vector_store");
    }

    @Test
    @Order(1)
    void step1_loadDocumentsAndSaveToPgVector() {
        loadDocumentsIntoPgVector();
    }

    @Test
    @Order(2)
    void step2_ragQueryWithPromptEnhancement() {
        loadDocumentsIntoPgVector();

        String chatId = "rag_test_" + UUID.randomUUID().toString().substring(0, 8);
        String question = "只有鸡蛋和西红柿，能快速做什么菜？";

        log.info("[RAG-TEST] ④ 用户提问: {}", question);
        log.info("[RAG-TEST] ⑤ 执行 doChatWithRag（QueryRewriter → 向量检索 → QuestionAnswerAdvisor → 大模型）");

        String answer = chefApp.doChatWithRag(question, chatId);

        assertNotNull(answer);
        assertFalse(answer.isBlank());
        log.info("[RAG-TEST] ⑥ RAG 回答: {}", answer);
    }

    /**
     * 一条用例跑完全流程（与分步测试等价，便于 IDE 右键只跑这一个）。
     */
    @Test
    void fullRagPipeline_inOneTest() {
        loadDocumentsIntoPgVector();

        String answer = chefApp.doChatWithRag(
                "低卡路里减脂晚餐有什么推荐？",
                "rag_full_" + UUID.randomUUID().toString().substring(0, 8)
        );
        assertFalse(answer.isBlank());
        log.info("[RAG-TEST] 全流程完成，回答长度={}", answer.length());
    }

    /**
     * 加载菜谱 → 增强 → embedding 写入 PgVector。
     */
    private void loadDocumentsIntoPgVector() {
        List<Document> documents = chefDocumentLoader.loadMarkdowns();
        assertFalse(documents.isEmpty(), "应加载到菜谱 Markdown");

        List<Document> enriched = myKeywordEnricher.enrichDocuments(documents);
        assertFalse(enriched.isEmpty());

        log.info("[RAG-TEST] ② 已加载并增强 {} 条文档片段，开始分批 embedding 写入 PgVector...", enriched.size());
        vectorStoreDocumentWriter.addInBatches(chefVectorStore, enriched);

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Long.class);
        assertNotNull(count);
        assertTrue(count > 0, "vector_store 中应有向量数据");
        log.info("[RAG-TEST] ③ 向量已入库，vector_store 行数={}", count);
    }
}
