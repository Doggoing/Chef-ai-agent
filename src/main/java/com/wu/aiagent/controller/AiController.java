package com.wu.aiagent.controller;

import com.wu.aiagent.agent.ChefManus;
import com.wu.aiagent.app.ChefApp;
import com.wu.aiagent.auth.AuthInterceptor;
import com.wu.aiagent.service.ChatConversationService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    private static final String SCENE_CHEF_APP = "chef_app";

    @Resource
    private ChefApp chefApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    @Qualifier("dashScopeChatModel")
    private ChatModel dashscopeChatModel;

    @Resource
    private ChatConversationService chatConversationService;

    @GetMapping("/chef_app/chat/sync")
    public String doChatSync(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                             String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChat(message, chatId);
    }

    @GetMapping(value = "/chef_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatSSE(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                  String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatByStream(message, chatId);
    }

    @GetMapping(value = "/chef_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatServerSentEvent(
            @RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
            String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder().data(chunk).build());
    }

    @GetMapping(value = "/chef_app/chat/sse_emitter")
    public SseEmitter doChatSseEmitter(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                       String message, String chatId) {
        prepareConversation(userId, chatId, message);
        SseEmitter sseEmitter = new SseEmitter(180000L);
        chefApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }

    @GetMapping("/chef_app/chat/rag")
    public String doChatWithRag(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatWithRag(message, chatId);
    }

    @GetMapping("/chef_app/chat/tools")
    public String doChatWithTools(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                  String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatWithTools(message, chatId);
    }

    @GetMapping("/chef_app/chat/mcp")
    public String doChatWithMcp(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatWithMcp(message, chatId);
    }

    @GetMapping("/chef_app/chat/report")
    public ChefApp.MealPlanReport doChatWithReport(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                                   String message, String chatId) {
        prepareConversation(userId, chatId, message);
        return chefApp.doChatWithReport(message, chatId);
    }

    @GetMapping("/chef_manus/chat")
    public SseEmitter doChatWithChefManus(@RequestAttribute(AuthInterceptor.ATTR_USER_ID) Long userId,
                                          String message, String chatId) {
        if (chatId == null || chatId.isBlank()) {
            chatId = "manus_" + System.currentTimeMillis();
        }
        chatConversationService.ensureConversation(userId, chatId, "chef_manus");
        ChefManus chefManus = new ChefManus(allTools, dashscopeChatModel);
        return chefManus.runStream(message);
    }

    // 业务层保存用户对话信息，每一次会话都保存，
    private void prepareConversation(Long userId, String chatId, String message) {
        chatConversationService.ensureConversation(userId, chatId, SCENE_CHEF_APP);
        chatConversationService.updateTitleIfDefault(chatId, message);
    }
}
