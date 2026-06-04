package com.wu.aiagent.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 向量库配置项。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /**
     * 为 true 时：仅当 vector_store 表为空才从 Markdown 导入（省 embedding API）。
     */
    private boolean initializeOnEmpty = true;

    /**
     * 为 true 时：启动跳过菜谱向量导入（测试环境用，避免每次跑测试都调 embedding）。
     */
    private boolean skipStartupLoad = false;

    /**
     * 百炼 Embedding 单次请求最多 10 条，写入向量库时需分批 add。
     */
    private int embeddingBatchSize = 10;
}
