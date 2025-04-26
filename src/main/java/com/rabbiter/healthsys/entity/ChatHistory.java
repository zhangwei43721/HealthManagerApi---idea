package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime; // 使用 LocalDateTime 对应数据库的 DATETIME

/**
 * <p>
 * 用户和AI聊天历史记录表
 * </p>
 *
 * @author Your Name // TODO: 改成你的名字
 * @since 2024-07-25 // TODO: 改成当前日期
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("j_chat_history") // 映射到数据库表名 j_chat_history
public class ChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String conversationId;

    private String role; // 消息发送者角色 (user, assistant)

    private String content; // 消息内容

    private LocalDateTime timestamp; // 消息发送/接收时间

    private LocalDateTime createdAt; // 记录创建时间 (TIMESTAMP 字段)

    public static ChatHistory fromChatMessage(Integer userId, String conversationId, io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage chatMessage) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setConversationId(conversationId);
        history.setRole(chatMessage.getRole()); // 角色通常是 "user" 或 "assistant"
        history.setContent(String.valueOf(chatMessage.getContent()));
        history.setTimestamp(LocalDateTime.now()); // 使用当前时间作为消息时间
        // createdAt 数据库通常会自动填充 CURRENT_TIMESTAMP
        return history;
    }

    public io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage toChatMessage() {
        return new io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage(this.role, this.content);
    }

}