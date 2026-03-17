package com.rabbiter.healthsys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper; // 确保导入 ObjectMapper
import com.rabbiter.healthsys.common.UserTokenResolver;
import com.rabbiter.healthsys.controller.OpenAiController;
import com.rabbiter.healthsys.entity.*; // 导入所有 entity
import com.rabbiter.healthsys.mapper.AiSuggestionsSpecificMapper;
import com.rabbiter.healthsys.service.IAiSuggestionsSpecificService;
import com.rabbiter.healthsys.service.IBodyNotesService;
import com.rabbiter.healthsys.service.ISportInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AI 健康建议服务实现类
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@Service
public class AiSuggestionsSpecificServiceImpl extends ServiceImpl<AiSuggestionsSpecificMapper, AiSuggestionsSpecific>
        implements IAiSuggestionsSpecificService {

    private static final Logger log = LoggerFactory.getLogger(AiSuggestionsSpecificServiceImpl.class);

    // 用于基于用户ID的锁，防止并发创建/查找记录的问题
    private final ConcurrentHashMap<Integer, Lock> userLocks = new ConcurrentHashMap<>();

    @Autowired
    private OpenAiController openAiController; // 确保已注入

    @Autowired
    private UserTokenResolver userTokenResolver;

    @Autowired
    private IBodyNotesService bodyNotesService; // 确保已注入

    @Autowired
    private ISportInfoService sportInfoService; // 确保已注入

    @Value("${ai.sportSuggestion.prompt}")
    private String sportPrompt;

    @Value("${ai.bodynote.prompt}")
    private String bodynotePrompt;

    @Value("${ai.bodynote.singlePrompt}")
    private String singlePrompt;

    private final ObjectMapper objectMapper = new ObjectMapper(); // 确保已实例化

    @Override
    public AiSuggestionsSpecific getLatestSuggestionByUserId(Integer userId) {
        LambdaQueryWrapper<AiSuggestionsSpecific> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(AiSuggestionsSpecific::getUserId, userId)
                .orderByDesc(AiSuggestionsSpecific::getGeneratedAt)
                .last("LIMIT 1");
        return this.baseMapper.selectOne(queryWrapper);
    }

    /**
     * 获取或创建用户最新的 AI 建议记录。
     * 使用锁确保同一用户并发请求时操作的原子性。
     * 
     * @param userId 用户 ID
     * @return 用户最新（或新创建）的 AiSuggestionsSpecific 记录
     */
    private AiSuggestionsSpecific getOrCreateLatestSuggestionRecordForUpdate(Integer userId) {
        // 为每个用户获取或创建一个锁
        Lock userLock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        userLock.lock(); // 获取锁
        try {
            AiSuggestionsSpecific latestRecord = getLatestSuggestionByUserId(userId);

            // 如果没有找到记录，或者你想定义一个“过时”的标准（比如每天生成新记录）
            // 这里简化处理：如果没有记录，就创建新的
            if (latestRecord == null) {
                log.info("未找到用户 {} 的现有建议记录，将创建新记录。", userId);
                latestRecord = new AiSuggestionsSpecific();
                latestRecord.setUserId(userId);
                latestRecord.setGeneratedAt(LocalDateTime.now());
                // 初始化所有建议字段为 null 或 "尚未生成"
                latestRecord.setSuggestionHistoricalHealth(null);
                latestRecord.setSuggestionCurrentHealth(null);
                latestRecord.setSuggestionSportInfo(null);

                boolean saved = this.save(latestRecord); // 保存新记录
                if (!saved || latestRecord.getId() == null) {
                    log.error("为用户 {} 创建新的建议记录失败！", userId);
                    throw new RuntimeException("创建新的建议记录失败");
                }
                log.info("已为用户 {} 成功创建新的建议记录, ID: {}", userId, latestRecord.getId());
                return latestRecord; // 返回新创建的记录
            } else {
                // 如果找到了记录，直接返回最新的这条
                log.debug("找到用户 {} 的最新建议记录, ID: {}", userId, latestRecord.getId());
                return latestRecord;
            }
        } finally {
            userLock.unlock(); // 释放锁
        }
    }

    // --- 修改 generate...Report 方法 ---

    @Override
    public void generateHistoricalReport(String token) {
        final Integer userId = requireUserId(token);
        String conversationId = UUID.randomUUID().toString(); // 每次生成报告用新的对话ID

        final Integer targetRecordId = prepareSuggestionRecord(
                userId,
                "历史健康报告",
                "报告生成中...",
                AiSuggestionsSpecific::setSuggestionHistoricalHealth);

        // 准备 AI 请求数据
        List<BodyNotes> notes = bodyNotesService.getLatestBodyNotesByUserId(userId);
        String dataJson;
        try {
            dataJson = objectMapper.writeValueAsString(notes);
        } catch (Exception e) {
            log.error("序列化用户历史数据失败 (generateHistoricalReport), userId: {}", userId, e);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionHistoricalHealth, "生成失败：数据处理错误");
            throw new RuntimeException("序列化用户数据失败", e);
        }
        String prompt = bodynotePrompt.replace("{{user_data}}", dataJson);

        Consumer<String> updateCallback = buildSuggestionUpdateCallback(
                targetRecordId,
                userId,
                "历史健康报告",
                AiSuggestionsSpecific::setSuggestionHistoricalHealth);

        // 4. 调用 Controller 的方法，传递回调
        log.info("为用户 {} (记录ID: {}) 调用 AI 生成历史健康报告 (异步)...", userId, targetRecordId);
        openAiController.streamReasoningChat(token, prompt, conversationId, updateCallback);

        // 5. 返回当前状态的目标记录（其中 historical health 字段是 "报告生成中...")
    }

    @Override
    public void generateCurrentReport(String token) {
        final Integer userId = requireUserId(token);
        String conversationId = UUID.randomUUID().toString();

        final Integer targetRecordId = prepareSuggestionRecord(
                userId,
                "当前健康报告",
                "报告生成中...",
                AiSuggestionsSpecific::setSuggestionCurrentHealth);

        // 准备 AI 数据
        List<BodyNotes> notes = bodyNotesService.getLatestBodyNotesByUserId(userId);
        if (notes.isEmpty()) {
            log.warn("未找到用户 {} 的身体记录 (generateCurrentReport)", userId);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionCurrentHealth, "生成失败：无身体记录");
            return; // 返回已标记失败的状态
        }
        BodyNotes record = notes.get(0);
        String recJson;
        try {
            recJson = objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.error("序列化单条身体记录失败 (generateCurrentReport), userId: {}", userId, e);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionCurrentHealth, "生成失败：数据处理错误");
            throw new RuntimeException("序列化单条记录失败", e);
        }
        String prompt = singlePrompt.replace("{{record_data}}", recJson);

        Consumer<String> updateCallback = buildSuggestionUpdateCallback(
                targetRecordId,
                userId,
                "当前健康报告",
                AiSuggestionsSpecific::setSuggestionCurrentHealth);

        // 4. 调用 Controller
        log.info("为用户 {} (记录ID: {}) 调用 AI 生成当前健康报告 (异步)...", userId, targetRecordId);
        openAiController.streamReasoningChat(token, prompt, conversationId, updateCallback);

        // 5. 返回目标记录
    }

    @Override
    public void generateSportReport(String token) {
        final Integer userId = requireUserId(token);
        String conversationId = UUID.randomUUID().toString();

        final Integer targetRecordId = prepareSuggestionRecord(
                userId,
                "运动建议报告",
                "报告生成中...",
                AiSuggestionsSpecific::setSuggestionSportInfo);

        // 准备 AI 数据
        List<BodyNotes> notes = bodyNotesService.getLatestBodyNotesByUserId(userId);
        if (notes.isEmpty()) {
            log.warn("未找到用户 {} 的身体记录 (generateSportReport)", userId);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionSportInfo, "生成失败：无身体记录");
            return;
        }
        BodyNotes record = notes.get(0);
        String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.error("序列化身体数据失败 (for sport), userId: {}", userId, e);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionSportInfo, "生成失败：身体数据处理错误");
            throw new RuntimeException("序列化数据失败", e);
        }
        String sportJson;
        try {
            sportJson = objectMapper.writeValueAsString(sportInfoService.list());
        } catch (JsonProcessingException e) {
            log.error("序列化运动信息失败 (for sport), userId: {}", userId, e);
            updateSuggestionField(targetRecordId, AiSuggestionsSpecific::setSuggestionSportInfo, "生成失败：运动信息处理错误");
            throw new RuntimeException("序列化运动信息失败", e);
        }
        String prompt = sportPrompt.replace("{{body_data}}", bodyJson).replace("{{sport_infos}}", sportJson);

        Consumer<String> updateCallback = buildSuggestionUpdateCallback(
                targetRecordId,
                userId,
                "运动建议报告",
                AiSuggestionsSpecific::setSuggestionSportInfo);

        // 4. 调用 Controller
        log.info("为用户 {} (记录ID: {}) 调用 AI 生成运动建议报告 (异步)...", userId, targetRecordId);
        openAiController.streamReasoningChat(token, prompt, conversationId, updateCallback);

        // 5. 返回目标记录
    }

    // analyzeSportSuggestion 方法保持不变，因为它设计为流式输出而非持久化到 AiSuggestionsSpecific
    @Override
    public SseEmitter analyzeSportSuggestion(String token, String conversationId) {
        // ... (代码不变) ...
        Integer userId = requireUserId(token);
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
            log.info("analyzeSportSuggestion: 为用户 {} 生成新会话 ID: {}", userId, conversationId);
            // 注意：这里没有发送 conversationId 给客户端，如果需要，需要额外处理
        }
        List<BodyNotes> notes = bodyNotesService.getLatestBodyNotesByUserId(userId);
        if (notes.isEmpty()) {
            throw new IllegalStateException("未找到身体记录, userId: " + userId);
        }
        BodyNotes record = notes.get(0);
        String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            log.error("序列化身体数据失败 (for analyzeSportSuggestion), userId: {}", userId, e);
            throw new RuntimeException("序列化数据失败", e);
        }
        String sportJson;
        try {
            sportJson = objectMapper.writeValueAsString(sportInfoService.list());
        } catch (JsonProcessingException e) {
            log.error("序列化运动信息失败 (for analyzeSportSuggestion), userId: {}", userId, e);
            throw new RuntimeException("序列化运动信息失败", e);
        }
        String prompt = sportPrompt.replace("{{body_data}}", bodyJson).replace("{{sport_infos}}", sportJson);

        log.info("analyzeSportSuggestion: 为用户 {} 调用 AI 进行运动建议分析 (流式)...", userId);
        // 调用 Controller，但不传递回调，因为此方法目的是返回 Emitter 给前端
        return openAiController.streamReasoningChat(token, prompt, conversationId, null);
    }

    private Integer requireUserId(String token) {
        User user = userTokenResolver.parseUser(token);
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("认证失败，请重新登录。");
        }
        return user.getId();
    }

    private Integer prepareSuggestionRecord(Integer userId,
            String reportName,
            String placeholder,
            BiConsumer<AiSuggestionsSpecific, String> fieldSetter) {
        AiSuggestionsSpecific targetRecord = getOrCreateLatestSuggestionRecordForUpdate(userId);
        Integer targetRecordId = targetRecord.getId();
        log.info("用户 {} 准备生成{}, 更新记录 ID: {} 状态为生成中...", userId, reportName, targetRecordId);
        fieldSetter.accept(targetRecord, placeholder);
        if (!this.updateById(targetRecord)) {
            log.warn("用户 {} 更新记录 ID: {} 的{}为 '{}' 状态失败", userId, targetRecordId, reportName, placeholder);
        }
        return targetRecordId;
    }

    private Consumer<String> buildSuggestionUpdateCallback(Integer targetRecordId,
            Integer userId,
            String reportName,
            BiConsumer<AiSuggestionsSpecific, String> fieldSetter) {
        return finalReport -> {
            try {
                log.info("收到{}回调, 准备更新记录 ID: {}, userId: {}", reportName, targetRecordId, userId);
                AiSuggestionsSpecific recordToUpdate = this.getById(targetRecordId);
                if (recordToUpdate == null) {
                    log.error("回调中无法找到要更新的{}记录, 记录 ID: {}, userId: {}", reportName, targetRecordId, userId);
                    return;
                }
                fieldSetter.accept(recordToUpdate, finalReport);
                if (this.updateById(recordToUpdate)) {
                    log.info("成功更新{}, 记录 ID: {}, userId: {}", reportName, targetRecordId, userId);
                } else {
                    log.warn("更新{}最终结果失败 (updateById 返回 false), 记录 ID: {}, userId: {}", reportName, targetRecordId,
                            userId);
                }
            } catch (Exception e) {
                log.error("在{}回调中更新数据库时发生异常, 记录 ID: {}, userId: {}", reportName, targetRecordId, userId, e);
                try {
                    updateSuggestionField(targetRecordId, fieldSetter, "生成失败：回调处理错误");
                } catch (Exception ex) {
                    log.error("尝试更新{}状态为失败时再次出错", reportName, ex);
                }
            }
        };
    }

    private void updateSuggestionField(Integer recordId,
            BiConsumer<AiSuggestionsSpecific, String> fieldSetter,
            String value) {
        AiSuggestionsSpecific record = this.getById(recordId);
        if (record == null) {
            return;
        }
        fieldSetter.accept(record, value);
        this.updateById(record);
    }

}
