package com.wu.aiagent.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class ChefDocumentLoaderTest {

    @Autowired
    private ChefDocumentLoader chefDocumentLoader;

    @Test
    void loadMarkdowns_shouldLoadCookingDocuments() {
        List<Document> documents = chefDocumentLoader.loadMarkdowns();
        Assertions.assertFalse(documents.isEmpty(), "应加载到菜谱 Markdown 文档");
        boolean hasCategory = documents.stream()
                .anyMatch(doc -> doc.getMetadata().containsKey("category"));
        Assertions.assertTrue(hasCategory, "文档应包含 category 元信息");
    }
}
