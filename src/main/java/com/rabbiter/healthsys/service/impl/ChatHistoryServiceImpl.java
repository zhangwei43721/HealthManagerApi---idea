package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.ChatHistory;
import com.rabbiter.healthsys.mapper.ChatHistoryMapper;
import com.rabbiter.healthsys.service.IChatHistoryService;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage; // 导入 AI4J 的 ChatMessage
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList; // 导入 ArrayList
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户和AI聊天历史记录 服务实现类
 * </p>
 *
 * @author skyforever
 * @since 2025-04-27
 */
@Service
@RequiredArgsConstructor // Lombok 注解，用于注入 final 字段
@Slf4j // Lombok 注解，用于日志
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements IChatHistoryService {
    @Override
    public List<ChatMessage> getChatMessagesByUserIdAndConversationId(Integer userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("查询聊天历史记录时，用户ID或对话ID为空。UserId: {}, ConversationId: {}", userId, conversationId);
            return new ArrayList<>();
        }

        // 使用 LambdaQueryWrapper 构建查询条件
        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatHistory::getUserId, userId); // 按用户ID过滤
        queryWrapper.eq(ChatHistory::getConversationId, conversationId); // 按对话ID过滤
        queryWrapper.orderByAsc(ChatHistory::getTimestamp); // 按时间戳升序排序，确保对话顺序正确

        // 执行查询
        List<ChatHistory> historyList = this.baseMapper.selectList(queryWrapper);
        // this.baseMapper 是 ServiceImpl 提供的 BaseMapper 实例

        // 将查询结果 ChatHistory List 转换为 AI4J 的 ChatMessage List
        List<ChatMessage> chatMessages = historyList.stream()
                .map(ChatHistory::toChatMessage) // 使用 ChatHistory 实体中的 toChatMessage 方法进行转换
                .collect(Collectors.toList());

        // *** 修正这里的变量名，应该使用参数 conversationId ***
        log.info("为用户 {} 对话 {} 查询到 {} 条历史记录。", userId, conversationId, chatMessages.size()); // <<< 修正此处

        return chatMessages;
    }

    // 实现接口中声明的方法，删除某个用户某个对话的所有历史记录
    @Override // <<< 添加 @Override 注解
    public int deleteChatHistoryByUserIdAndConversationId(Integer userId, String conversationId) {
        if (userId == null || conversationId == null || conversationId.trim().isEmpty()) {
            log.warn("删除聊天历史记录时，用户ID或对话ID为空。UserId: {}, ConversationId: {}", userId, conversationId);
            return 0; // 返回 0 表示没有删除
        }

        // 使用 LambdaQueryWrapper 构建删除条件
        LambdaQueryWrapper<ChatHistory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatHistory::getUserId, userId);
        deleteWrapper.eq(ChatHistory::getConversationId, conversationId);

        int deletedCount = this.baseMapper.delete(deleteWrapper); // 使用 baseMapper.delete 以便返回 int
        log.info("为用户 {} 对话 {} 删除 {} 条历史记录。", userId, conversationId, deletedCount);
        return deletedCount;
    }

    // 实现接口中声明的方法，删除某个用户的所有历史记录
    @Override // <<< 添加 @Override 注解
    public int deleteAllChatHistoryByUserId(Integer userId) {
        if (userId == null) {
            log.warn("删除用户所有聊天历史记录时，用户ID为空。");
            return 0;
        }
        LambdaQueryWrapper<ChatHistory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ChatHistory::getUserId, userId);

        int deletedCount = this.baseMapper.delete(deleteWrapper); // 使用 baseMapper.delete 以便返回 int
        log.info("为用户 {} 删除所有 {} 条历史记录。", userId, deletedCount);
        return deletedCount;
    }
}