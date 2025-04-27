package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.healthsys.entity.AiSuggestionsSpecific;
import com.rabbiter.healthsys.mapper.AiSuggestionsSpecificMapper;
import com.rabbiter.healthsys.service.IAiSuggestionsSpecificService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.rabbiter.healthsys.controller.OpenAiController;

/**
 * <p>
 * 存储AI针对不同页面生成的健康建议表 (分列存储) 服务实现类
 * </p>
 *
 * @author Skyforever
 * @since 2025-04-27
 */
@Service // 标记为 Spring 的 Service 组件
public class AiSuggestionsSpecificServiceImpl extends ServiceImpl<AiSuggestionsSpecificMapper, AiSuggestionsSpecific> implements IAiSuggestionsSpecificService {

    // ServiceImpl<M, T> 自动注入了 baseMapper (即 AiSuggestionsSpecificMapper)

    @Autowired
    private OpenAiController openAiController;

    @Override
    public AiSuggestionsSpecific getLatestSuggestionByUserId(Integer userId) {
        // 使用 LambdaQueryWrapper 构造查询条件
        LambdaQueryWrapper<AiSuggestionsSpecific> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(AiSuggestionsSpecific::getUserId, userId) // 条件：userId 匹配
                .orderByDesc(AiSuggestionsSpecific::getGeneratedAt) // 按生成时间降序排序
                .last("LIMIT 1"); // 取最新的一条记录

        return this.baseMapper.selectOne(queryWrapper); // 或者使用 this.getOne(queryWrapper);
        // 注意：如果有多条记录时间完全相同，数据库可能会随机返回一条，LIMIT 1 确保只返回一条。
    }

    /**
     * 内部调用 OpenAiController 的中文聊天流接口
     */
    public SseEmitter chatStreamChinese(String token, String question, String conversationId) {
        return openAiController.getChatMessageStreamChinese(token, question, conversationId);
    }

    // 你可以在这里实现其他在 IAiSuggestionsSpecificService 接口中声明的自定义方法
    // 例如：
    // public List<AiSuggestionsSpecific> getSuggestionsForUserInDateRange(Integer userId, LocalDateTime start, LocalDateTime end) {
    //     // ... 实现逻辑 ...
    // }
}