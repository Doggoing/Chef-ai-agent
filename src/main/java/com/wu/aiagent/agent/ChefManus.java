package com.wu.aiagent.agent;

import com.wu.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 百味智厨 ReAct 智能体
 */
public class ChefManus extends ToolCallAgent {

    public ChefManus(ToolCallback[] allTools, @Qualifier("dashScopeChatModel") ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("chefManus");
        String systemPrompt = """
                You are ChefManus, a professional AI cooking assistant.
                You help users with recipes, meal planning, ingredient substitution, and cooking techniques.
                Use available tools when needed to search recipes, save notes, or generate documents.
                """;
        this.setSystemPrompt(systemPrompt);
        String nextStepPrompt = """
                Based on the user's cooking request, select the most appropriate tool or combination of tools.
                For complex tasks, break down the problem and solve it step by step.
                After each tool call, summarize the result and suggest the next step.
                Use the `doTerminate` tool when the task is fully completed.
                """;
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
