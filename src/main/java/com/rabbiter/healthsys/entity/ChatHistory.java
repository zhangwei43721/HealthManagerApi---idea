package com.rabbiter.healthsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户和AI聊天历史记录表
 * </p>
 *
 * @author skyforever
 * @since 2025-04-27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("j_chat_history")
public class ChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer userId;

    private String conversationId;

    private String role;

    private String content;

    private LocalDateTime timestamp;

    private LocalDateTime createdAt;

    /**
     * 创建用户消息历史记录
     */
    public static ChatHistory createUserMessage(Integer userId, String conversationId, String content) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setConversationId(conversationId);
        history.setRole("user");
        history.setContent(content);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }

    /**
     * 创建助手消息历史记录
     */
    public static ChatHistory createAssistantMessage(Integer userId, String conversationId, String content) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setConversationId(conversationId);
        history.setRole("assistant");
        history.setContent(content);
        history.setTimestamp(LocalDateTime.now());
        return history;
    }
}
