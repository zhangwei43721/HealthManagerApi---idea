package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.ChatHistory;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;

import java.util.List;

/**
 * <p>
 * 用户和AI聊天历史记录 服务类
 * </p>
 *
 * @author Your Name // TODO: 改成你的名字
 * @since 2024-07-25 // TODO: 改成当前日期
 */
public interface IChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据用户ID和对话ID获取聊天历史记录，按时间排序
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @return 聊天历史记录列表 (使用 AI4J 的 ChatMessage 格式返回，方便 Controller 使用)
     */
    List<ChatMessage> getChatMessagesByUserIdAndConversationId(Integer userId, String conversationId);

    // 声明删除某个用户所有历史记录的方法
    /**
     * 删除某个用户的所有聊天历史记录
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteAllChatHistoryByUserId(Integer userId); // <<< 添加此方法声明

    int deleteChatHistoryByUserIdAndConversationId(Integer userId, String conversationId);

    // Note: deleteChatHistoryByUserIdAndConversationId 和 saveChatHistory 可以依赖 IService 的 remove 和 save 方法，
    // 不强制在接口中声明，但为了Service层职责清晰也可以保留。这里为了简单，OpenAiController 直接调用 service 的 save 方法。
    // 而删除特定对话的功能，我们仍然保留 deleteChatHistoryByUserIdAndConversationId 在 ServiceImpl 中实现，并在 Controller 中调用。
}