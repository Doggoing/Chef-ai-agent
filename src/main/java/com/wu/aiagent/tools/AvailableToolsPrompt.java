package com.wu.aiagent.tools;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 根据已注册的 {@link ToolCallback} 生成 System 提示词中的「可用工具」说明段落。
 */
public final class AvailableToolsPrompt {

    private AvailableToolsPrompt() {
    }

    /**
     * 拼接到 defaultSystem 末尾，用文字告知模型可调用的工具及使用原则。
     *
     * @param tools {@link ToolRegistration#allTools()} 注册的工具列表
     * @return 工具说明段落；无工具时返回空字符串
     */
    public static String build(ToolCallback[] tools) {
        if (tools == null || tools.length == 0) {
            return "";
        }
        StringBuilder section = new StringBuilder("""

                ## 可用工具
                以下工具已通过 Function Calling 注册，请在确实需要时再调用；简单问答、常识性烹饪问题可直接回答，不必强行调工具。
                """);
        for (ToolCallback tool : tools) {
            ToolDefinition definition = tool.getToolDefinition();
            section.append("- **")
                    .append(definition.name())
                    .append("**：")
                    .append(definition.description())
                    .append('\n');
        }
        return section.toString();
    }
}
