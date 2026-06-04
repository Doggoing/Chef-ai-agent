package com.wu.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wu.aiagent.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 消息 Mapper。
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 物理删除会话下全部消息（ChatMemory saveAll 全量替换时使用）。
     *
     * @param conversationId 会话 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM chat_message WHERE conversation_id = #{conversationId}")
    int physicalDeleteByConversationId(@Param("conversationId") String conversationId);

}
