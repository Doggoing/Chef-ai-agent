package com.wu.aiagent.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wu.aiagent.entity.ChatConversation;
import com.wu.aiagent.entity.ChatMessage;
import com.wu.aiagent.mapper.ChatConversationMapper;
import com.wu.aiagent.mapper.ChatMessageMapper;
import com.wu.aiagent.service.ChatConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 PostgreSQL 的对话记忆仓储。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PostgreSqlChatMemoryRepository implements ChatMemoryRepository {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatConversationMapper chatConversationMapper;
    private final ChatConversationService chatConversationService;

    @Override
    public List<String> findConversationIds() {
        return chatConversationMapper.selectList(new LambdaQueryWrapper<ChatConversation>()
                        .select(ChatConversation::getConversationId))
                .stream()
                .map(ChatConversation::getConversationId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatMessage> entities = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt));
        return ChatMessageConverter.toSpringMessages(entities);
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        chatMessageMapper.physicalDeleteByConversationId(conversationId);
        for (Message message : messages) {
            ChatMessage entity = ChatMessageConverter.toEntity(conversationId, message);
            chatMessageMapper.insert(entity);
        }
        chatConversationService.updateConversationStats(conversationId, messages.size());
        log.debug("persist chat memory, conversationId={}, messageCount={}", conversationId, messages.size());
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        chatMessageMapper.physicalDeleteByConversationId(conversationId);
    }
}
