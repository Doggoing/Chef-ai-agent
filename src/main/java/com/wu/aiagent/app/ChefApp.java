package com.wu.aiagent.app;

import com.wu.aiagent.advisor.MyLoggerAdvisor;
import com.wu.aiagent.rag.QueryRewriter;
import com.wu.aiagent.tools.AvailableToolsPrompt;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 百味智厨 AI 助手核心应用
 */
@Component
@Slf4j
public class ChefApp {

    private static final String SYSTEM_PROMPT = """
            你是「百味智厨」AI 烹饪助手，擅长菜谱推荐、食材搭配、烹饪步骤讲解和膳食建议。
            回答时请条理清晰：先给结论，再分步骤说明；涉及用量尽量给出常见家庭份量。
            若用户有忌口或过敏，主动提醒并给出替代方案。
            """;

    private final ChatClient chatClient;

    /** 基础角色提示 + 启动时根据 allTools 生成的工具说明 */
    private final String systemPromptWithTools;

    /**
     * 初始化 ChatClient：System 含工具说明、默认 Advisor，并注册全部本地工具（Function Calling）。
     *
     * @param dashscopeChatModel       百炼对话模型
     * @param messageWindowChatMemory  窗口化对话记忆（PostgreSQL 持久化）
     * @param allTools                 {@link com.wu.aiagent.tools.ToolRegistration} 注册的本地工具
     */
    public ChefApp(@Qualifier("dashScopeChatModel") ChatModel dashscopeChatModel,
                   MessageWindowChatMemory messageWindowChatMemory,
                   ToolCallback[] allTools) {
        this.systemPromptWithTools = SYSTEM_PROMPT + AvailableToolsPrompt.build(allTools);
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPromptWithTools)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build(), // 对话消息持久化
                        new MyLoggerAdvisor()
                )
                .defaultToolCallbacks(allTools)
                .build();
        log.info("ChatClient 已加载 {} 个本地工具，并写入 System 提示词", allTools.length);
    }

    /**
     * 多轮对话（会话记忆持久化到 PostgreSQL）
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chef chat content: {}", content);
        return content;
    }

    /**
     * SSE 流式多轮对话
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    public record MealPlanReport(String title, List<String> suggestions) {
    }

    /**
     * 结构化输出：膳食计划报告
     */
    public MealPlanReport doChatWithReport(String message, String chatId) {
        MealPlanReport report = chatClient
                .prompt()
                .system(systemPromptWithTools + " 请根据用户需求生成膳食计划报告，标题简洁，建议列表 3~5 条。")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(MealPlanReport.class);
        log.info("mealPlanReport: {}", report);
        return report;
    }

    @Resource
    private VectorStore chefVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * RAG 菜谱知识库问答（内存向量库）
     */
    public String doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)) // 同一个会话id
                .advisors(new MyLoggerAdvisor())
                .advisors(QuestionAnswerAdvisor.builder(chefVectorStore).build()) // 调用rag
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chef rag content: {}", content);
        return content;
    }

    /**
     * 工具调用：联网搜索、PDF 生成等（工具已在 ChatClient 默认注册，保留独立入口便于 HTTP 路由与日志区分）。
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // .advisors(new MyLoggerAdvisor())
                // .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chef tools content: {}", content);
        return content;
    }

    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * MCP 调用：如 QwenImage 菜品图生成
     */
    public String doChatWithMcp(String message, String chatId) {
        if (toolCallbackProvider == null) {
            return "MCP 服务未启用，请在 application.yml 中配置 spring.ai.mcp.client 并确保连接正常。";
        }
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider.getToolCallbacks()) // 用工具
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("chef mcp content: {}", content);
        return content;
    }

}
