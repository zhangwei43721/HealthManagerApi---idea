package com.rabbiter.healthsys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.healthsys.entity.ChatHistory;

import java.util.List;

/**
 * <p>
 * 用户和AI聊天历史记录 服务类
 * </p>
 *
 * @author skyforever
 * @since 2025-04-27
 */
public interface IChatHistoryService extends IService<ChatHistory> {

    /**
     * 根据用户ID和对话ID获取聊天历史记录，按时间排序
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @return 聊天历史记录列表
     */
    List<ChatHistory> getChatHistoryByUserIdAndConversationId(Integer userId, String conversationId);

    /**
     * 删除某个用户的所有聊天历史记录
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int deleteAllChatHistoryByUserId(Integer userId);

    /**
     * 删除某个用户特定对话的聊天历史记录
     * @param userId 用户ID
     * @param conversationId 对话ID
     * @return 删除的记录数
     */
    int deleteChatHistoryByUserIdAndConversationId(Integer userId, String conversationId);
}
