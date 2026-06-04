package com.wu.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * ReAct 智能体集成测试，步骤多、耗时长，默认禁用。
 */
@SpringBootTest
@ActiveProfiles({"local", "test"})
@Disabled("智能体多步调用较耗时，本地验证时手动运行")
class ChefManusTest {

    @Resource
    private ToolCallback[] allTools;

    @Resource
    @Qualifier("dashScopeChatModel")
    private ChatModel dashscopeChatModel;

    @Test
    void run_simpleCookingTask() {
        ChefManus chefManus = new ChefManus(allTools, dashscopeChatModel);
        String result = chefManus.run("帮我查一下番茄炒蛋的做法要点，并总结成 3 条建议");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isBlank());
    }
}
