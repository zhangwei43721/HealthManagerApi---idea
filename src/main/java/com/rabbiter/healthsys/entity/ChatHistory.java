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
@Data // Lombok 注解，生成 getter, setter, equals, hashCode, toString
@NoArgsConstructor // Lombok 注解，生成无参构造函数
@AllArgsConstructor // Lombok 注解，生成全参构造函数
@TableName("j_chat_history") // 映射到数据库表名 j_chat_history
public class ChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO) // 主键ID，自增长
    private Long id; // 使用 Long 对应数据库的 BIGINT

    private Integer userId; // 关联用户ID

    private String conversationId; // 唯一对话标识符

    private String role; // 消息发送者角色 (user, assistant)

    private String content; // 消息内容

    private LocalDateTime timestamp; // 消息发送/接收时间

    private LocalDateTime createdAt; // 记录创建时间 (TIMESTAMP 字段)

    // 注意：使用 Lombok 的 @Data 会自动生成 getter 和 setter，无需手动编写

    // 可以添加一个便捷的方法将 AI4J 的 ChatMessage 转换为 ChatHistory Entity
    // 需要用户ID和conversationId
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

    // 可以添加一个便捷的方法将 ChatHistory Entity 转换为 AI4J 的 ChatMessage
    public io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage toChatMessage() {
        // 确保导入 io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage
        return new io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage(this.role, this.content);
        // 注意：ChatMessage 的构造函数可能需要根据 ai4j 库的具体版本来调整，
        // 常见的是 new ChatMessage(role, content) 或者 ChatMessage.builder().role(role).content(content).build();
        // 基于你之前代码中的 ChatMessage.withUser/withAssistant 的用法，
        // 它的构造函数可能是 ChatMessage(String role, String content)。
    }

}