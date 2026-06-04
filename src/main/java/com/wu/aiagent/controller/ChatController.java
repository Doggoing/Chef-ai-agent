package com.wu.aiagent.controller;

import com.wu.aiagent.auth.AuthInterceptor;
import com.wu.aiagent.dto.ConversationVO;
import com.wu.aiagent.dto.CreateConversationRequest;
import com.wu.aiagent.dto.MessageVO;
import com.wu.aiagent.entity.ChatConversation;
import com.wu.aiagent.service.ChatConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会话与历史消息接口。
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatConversationService chatConversationService;

    /**
     * 当前用户的会话列表。
     */
    @GetMapping("/conversations")
    public List<ConversationVO> listConversations(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId) {
        return chatConversationService.listConversations(userId);
    }

    /**
     * 创建新会话。
     */
    @PostMapping("/conversations")
    public Map<String, String> createConversation(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                                  @RequestBody(required = false) CreateConversationRequest request) {
        if (request == null) {
            request = new CreateConversationRequest();
        }
        ChatConversation conversation = chatConversationService.createConversation(userId, request);
        return Map.of("conversationId", conversation.getConversationId());
    }

    /**
     * 查询会话历史消息。
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageVO> listMessages(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                        @PathVariable String conversationId) {
        return chatConversationService.listMessages(userId, conversationId);
    }
}
