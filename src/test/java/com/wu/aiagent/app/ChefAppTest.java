package com.wu.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * 集成测试：会调用百炼大模型，需配置有效的 application-local.yml（dashscope api-key）。
 * 无 Key 或网络异常时可能失败，可在 IDE 中单独运行。
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
class ChefAppTest {

    @Resource
    private ChefApp chefApp;

    private String newChatId() {
        return "test_" + UUID.randomUUID();
    }

    @Test
    void testChat_multiRoundMemory() {
        String chatId = newChatId();
        String answer1 = chefApp.doChat("你好，我想学做番茄炒蛋", chatId);
        Assertions.assertNotNull(answer1);
        Assertions.assertFalse(answer1.isBlank());

        String answer2 = chefApp.doChat("刚才说的那道菜，需要哪些食材？", chatId);
        Assertions.assertNotNull(answer2);
        Assertions.assertFalse(answer2.isBlank());
    }

    @Test
    void doChatWithReport() {
        String chatId = newChatId();
        String message = "帮我制定一份适合上班族的一周健康晚餐计划";
        ChefApp.MealPlanReport report = chefApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(report);
        Assertions.assertNotNull(report.title());
        Assertions.assertNotNull(report.suggestions());
        Assertions.assertFalse(report.suggestions().isEmpty());
    }

    @Test
    void doChatWithRag() {
        String chatId = newChatId();
        String message = "只有鸡蛋和西红柿，能快速做什么菜？";
        String answer = chefApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isBlank());
    }

    @Test
    @Disabled("会调用联网搜索，消耗 SearchAPI 额度，需要时手动开启")
    void doChatWithTools_webSearch() {
        String chatId = newChatId();
        String message = "番茄炒蛋的家常做法步骤是什么？";
        String answer = chefApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isBlank());
    }

    @Test
    @Disabled("会调用百炼 QwenImage MCP，需要 MCP 可用且 initialized=true，需要时手动开启")
    void doChatWithMcp_generateImage() {
        String chatId = newChatId();
        String message = "请生成一张番茄炒蛋的美食示意图";
        String answer = chefApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
        Assertions.assertFalse(answer.isBlank());
    }
}
