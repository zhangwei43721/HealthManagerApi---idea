package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.ChatHistory;
import com.rabbiter.healthsys.mapper.ChatHistoryMapper;
import com.rabbiter.healthsys.service.IChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户和AI聊天历史记录 服务实现类
 * </p>
 *
 * @author skyforever
 * @since 2025-04-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements IChatHistoryService {

    @Override
    public List<ChatHistory> getChatHistoryByUserIdAndConversationId(Integer userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("查询聊天历史记录时，用户ID或对话ID为空。UserId: {}, ConversationId: {}", userId, conversationId);
            return new ArrayList<>();
        }

        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatHistory::getUserId, userId);
        queryWrapper.eq(ChatHistory::getConversationId, conversationId);
        queryWrapper.orderByAsc(ChatHistory::getTimestamp);

        List<ChatHistory> historyList = this.baseMapper.selectList(queryWrapper);
        log.info("为用户 {} 对话 {} 查询到 {} 条历史记录。", userId, conversationId, historyList.size());

        return historyList;
    }

    @Override
    public int deleteChatHistoryByUserIdAndConversationId(Integer userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("删除聊天历史记录时，用户ID或对话ID为空。UserId: {}, ConversationId: {}", userId, conversationId);
            return 0;
        }

        LambdaQueryWrapper<ChatHistory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatHistory::getUserId, userId);
        deleteWrapper.eq(ChatHistory::getConversationId, conversationId);

        int deletedCount = this.baseMapper.delete(deleteWrapper);
        log.info("为用户 {} 对话 {} 删除 {} 条历史记录。", userId, conversationId, deletedCount);
        return deletedCount;
    }

    @Override
    public int deleteAllChatHistoryByUserId(Integer userId) {
        if (userId == null) {
            log.warn("删除用户所有聊天历史记录时，用户ID为空。");
            return 0;
        }
        LambdaQueryWrapper<ChatHistory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatHistory::getUserId, userId);

        int deletedCount = this.baseMapper.delete(deleteWrapper);
        log.info("为用户 {} 删除所有 {} 条历史记录。", userId, deletedCount);
        return deletedCount;
    }
}
