package com.wu.aiagent.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.aiagent.dto.ConversationVO;
import com.wu.aiagent.dto.CreateConversationRequest;
import com.wu.aiagent.dto.MessageVO;
import com.wu.aiagent.entity.ChatConversation;
import com.wu.aiagent.entity.ChatMessage;
import com.wu.aiagent.exception.BusinessException;
import com.wu.aiagent.mapper.ChatConversationMapper;
import com.wu.aiagent.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话与历史消息服务。
 */
@Service
@RequiredArgsConstructor
public class ChatConversationService {

    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 创建新会话。
     *
     * @param userId  用户 ID
     * @param request 创建参数
     * @return 会话
     */
    public ChatConversation createConversation(Long userId, CreateConversationRequest request) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId("chef_" + IdUtil.simpleUUID().substring(0, 12));
        conversation.setUserId(userId);
        conversation.setScene(StrUtil.blankToDefault(request.getScene(), "chef_app"));
        conversation.setTitle(StrUtil.blankToDefault(request.getTitle(), "新对话"));
        conversation.setMessageCount(0);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 确保会话存在且归属当前用户（首次发消息时调用）。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @param scene          场景
     */
    public void ensureConversation(Long userId, String conversationId, String scene) {
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, conversationId));
        if (conversation == null) {
            ChatConversation created = new ChatConversation();
            created.setConversationId(conversationId);
            created.setUserId(userId);
            created.setScene(scene);
            created.setTitle("新对话");
            created.setMessageCount(0);
            created.setCreatedAt(LocalDateTime.now());
            created.setUpdatedAt(LocalDateTime.now());
            chatConversationMapper.insert(created);
            return;
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(403, "无权访问该会话");
        }
    }

    /**
     * 更新会话统计信息。
     *
     * @param conversationId 会话 ID
     * @param messageCount   当前窗口消息数
     */
    public void updateConversationStats(String conversationId, int messageCount) {
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, conversationId));
        if (conversation == null) {
            return;
        }
        conversation.setMessageCount(messageCount);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationMapper.updateById(conversation);
    }

    /**
     * 根据首条用户消息更新会话标题。
     *
     * @param conversationId 会话 ID
     * @param userMessage    用户消息
     */
    public void updateTitleIfDefault(String conversationId, String userMessage) {
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, conversationId));
        if (conversation == null || !"新对话".equals(conversation.getTitle())) {
            return;
        }
        String title = StrUtil.sub(userMessage, 0, 20);
        if (StrUtil.isBlank(title)) {
            return;
        }
        conversation.setTitle(title);
        conversation.setUpdatedAt(LocalDateTime.now());
        chatConversationMapper.updateById(conversation);
    }

    /**
     * 校验会话归属。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @return 会话实体
     */
    public ChatConversation requireConversation(Long userId, String conversationId) {
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getConversationId, conversationId));
        if (conversation == null) {
            throw new BusinessException(404, "会话不存在");
        }
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(403, "无权访问该会话");
        }
        return conversation;
    }

    /**
     * 查询用户会话列表。
     *
     * @param userId 用户 ID
     * @return 会话列表
     */
    public List<ConversationVO> listConversations(Long userId) {
        List<ChatConversation> conversations = chatConversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getUserId, userId)
                .orderByDesc(ChatConversation::getLastMessageAt)
                .orderByDesc(ChatConversation::getCreatedAt));
        return conversations.stream()
                .map(item -> new ConversationVO(
                        item.getConversationId(),
                        item.getTitle(),
                        item.getScene(),
                        item.getMessageCount(),
                        item.getLastMessageAt()))
                .collect(Collectors.toList());
    }

    /**
     * 查询会话历史消息。
     *
     * @param userId         用户 ID
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    public List<MessageVO> listMessages(Long userId, String conversationId) {
        requireConversation(userId, conversationId);
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt));
        return messages.stream()
                .map(item -> new MessageVO(item.getRole(), item.getContent(), item.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
