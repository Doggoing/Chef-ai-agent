package com.wu.aiagent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分批将文档写入向量库（适配百炼 Embedding 单次 batch ≤ 10 的限制）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreDocumentWriter {

    private final RagProperties ragProperties;

    /**
     * 按批写入向量库，避免一次性 add 触发 DashScope batch size 超限。
     *
     * @param vectorStore 向量库
     * @param documents   待写入文档
     */
    public void addInBatches(VectorStore vectorStore, List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        int batchSize = Math.max(1, ragProperties.getEmbeddingBatchSize());
        int total = documents.size();
        for (int from = 0; from < total; from += batchSize) {
            int to = Math.min(from + batchSize, total);
            List<Document> batch = documents.subList(from, to);
            vectorStore.add(batch);
            log.debug("RAG: 向量写入批次 [{}, {}), 本批 {} 条", from, to, batch.size());
        }
        log.info("RAG: 向量写入完成，共 {} 条，batchSize={}", total, batchSize);
    }
}
