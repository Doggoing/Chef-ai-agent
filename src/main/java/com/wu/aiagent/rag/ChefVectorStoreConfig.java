package com.wu.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * RAG 向量库配置（PgVectorStore，持久化到 PostgreSQL）。
 * <p>
 * 与对话记忆（chat_message）无关，不影响 ChefApp 普通多轮对话 / SSE。
 * <p>
 * 下方保留原内存向量库 {@link org.springframework.ai.vectorstore.SimpleVectorStore} 配置，便于对比或切回。
 */
@Slf4j
@Configuration
public class ChefVectorStoreConfig {

    @Resource
    private ChefDocumentLoader chefDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private RagProperties ragProperties;

    @Resource
    private VectorStoreDocumentWriter vectorStoreDocumentWriter;

    // -------------------------------------------------------------------------
    // 原内存向量库配置（SimpleVectorStore，重启后向量丢失，已改用下方 PgVector）
    // -------------------------------------------------------------------------
    //
    // import org.springframework.ai.vectorstore.SimpleVectorStore;
    //
    // @Bean
    // VectorStore chefVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
    //     SimpleVectorStore vectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
    //     List<Document> documents = chefDocumentLoader.loadMarkdowns();
    //     List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
    //     vectorStore.add(enrichedDocuments);
    //     return vectorStore;
    // }
    //
    // -------------------------------------------------------------------------
    // 当前：PgVectorStore（PostgreSQL vector_store 表）
    // -------------------------------------------------------------------------

    /**
     * 百味智厨 RAG 向量库 Bean（供 ChefApp.doChatWithRag 使用）。
     *
     * @param jdbcTemplate            JDBC 模板
     * @param dashscopeEmbeddingModel 百炼 Embedding 模型
     * @return PgVector 向量库
     */
    @Bean
    VectorStore chefVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .vectorTableName("vector_store")
                .build();

        if (ragProperties.isSkipStartupLoad()) {
            log.info("RAG: skipStartupLoad=true，跳过启动时 Markdown 向量导入");
            return vectorStore;
        }

        if (ragProperties.isInitializeOnEmpty()) {
            long existing = vectorStoreCount(jdbcTemplate);
            if (existing > 0) {
                log.info("RAG: vector_store 已有 {} 条，跳过 Markdown 导入", existing);
                return vectorStore;
            }
        }

        List<Document> documents = chefDocumentLoader.loadMarkdowns();
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
        vectorStoreDocumentWriter.addInBatches(vectorStore, enrichedDocuments);
        return vectorStore;
    }

    private long vectorStoreCount(JdbcTemplate jdbcTemplate) {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Long.class);
            return count == null ? 0L : count;
        } catch (Exception ex) {
            log.debug("RAG: vector_store 尚未就绪，将执行首次导入");
            return 0L;
        }
    }
}
