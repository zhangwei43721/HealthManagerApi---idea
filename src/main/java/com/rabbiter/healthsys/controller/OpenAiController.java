package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factor.AiService;

import com.rabbiter.healthsys.entity.ChatHistory;
import com.rabbiter.healthsys.service.IChatHistoryService;
import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor // 自动生成包含 final 字段的构造函数
@Slf4j // Lombok 注解，用于自动生成日志记录器
public class OpenAiController {

    private final AiService aiService; // 注入 AI 服务工厂
    private final IChatHistoryService chatHistoryService; // 注入聊天历史记录服务
    private final JwtConfig jwtConfig; // 注入 JWT 配置，用于 Token 解析

    // --- 从配置文件注入模型名称 ---
    @Value("${ai.model.default}") // 注入默认接口使用的模型名称
    private String defaultChatModel;

    @Value("${ai.model.chinese}") // 注入中文接口使用的模型名称 (deepseek-r1)
    private String chineseChatModel;
    // --- End of configuration injection ---

    /**
     * AI 聊天流接口 (中文注释)。
     * 通过 URL Query 参数中的 token 识别用户，处理对话流和历史记录。
     * 使用配置文件中指定的默认模型 (ai.model.default)。
     * @param token 用户认证 token (在 URL Query 参数 "token" 中)
     * @param question 用户输入的问题 (在 URL Query 参数 "question" 中)
     * @param conversationId 当前对话 ID (可选, 在 URL Query 参数 "conversationId" 中)。如果为 null/空/"new"，则开始新对话。
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    @GetMapping("/chatStream")
    public SseEmitter getChatMessageStream(
            @RequestParam String token, // <<< 从 URL Query 参数获取 Token
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        SseEmitter emitter = new SseEmitter(3600000L); // 设置 SSE 超时时间 (单位: 毫秒)

        // --- 1. 从 Token 中解析用户 ID ---
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("/chatStream: Token解析成功，但未能获取有效的用户ID。Token: {}", token);
                try { emitter.send(SseEmitter.event().name("error").data("用户信息无效，请核对。")); } catch (IOException e) { log.error("/chatStream: 发送错误事件至客户端失败", e); }
                emitter.completeWithError(new IllegalArgumentException("用户信息无效，请重新登录。"));
                return emitter;
            }
            userId = user.getId();
            log.info("/chatStream: Token 解析成功，用户 ID: {}", userId);
        } catch (Exception e) {
            log.error("/chatStream: Token 解析失败。Token: {}", token, e);
            try { emitter.send(SseEmitter.event().name("error").data("认证失败，请重新登录。")); } catch (IOException ioException) { log.error("/chatStream: 发送错误事件至客户端失败", ioException); }
            emitter.completeWithError(new IllegalArgumentException("认证信息无效或已过期，请重新登录。"));
            return emitter;
        }

        // --- 2. 处理会话 ID ---
        String currentConversationId = conversationId;
        if (currentConversationId == null || currentConversationId.trim().isEmpty() || "new".equalsIgnoreCase(currentConversationId.trim())) {
            currentConversationId = UUID.randomUUID().toString();
            log.info("/chatStream: 为用户 {} 生成了新的会话 ID: {}", userId, currentConversationId);
            try {
                emitter.send(SseEmitter.event().name("conversationId").data(currentConversationId));
                log.info("/chatStream: 已将新的会话 ID {} 发送给用户 {}", currentConversationId, userId);
            } catch (IOException e) {
                log.error("/chatStream: 向客户端发送新的会话 ID 失败。", e);
                emitter.completeWithError(e);
                return emitter;
            }
        } else {
            log.info("/chatStream: 用户 {} 继续使用现有会话 ID: {}", userId, currentConversationId);
        }

        final String finalConversationId = currentConversationId;
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);
        List<ChatMessage> historyMessages = chatHistoryService.getChatMessagesByUserIdAndConversationId(userId, finalConversationId);
        log.info("/chatStream: 为用户 {} 的会话 {} 加载了 {} 条历史消息。", userId, finalConversationId, historyMessages.size());
        ChatMessage userMessage = ChatMessage.withUser(question);
        historyMessages.add(userMessage);
        log.info("/chatStream: 本次发送给 AI 的消息总数 (包括历史): {}", historyMessages.size());
        ChatHistory userChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, userMessage);
        chatHistoryService.save(userChatHistory);
        log.info("/chatStream: 用户消息已保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);


        // --- 6. 准备调用 AI 服务的请求参数 ---
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(defaultChatModel)
                .messages(historyMessages)
                .build();
        log.info("/chatStream: 使用模型: {}", defaultChatModel);

        // --- 7. 异步处理 ---
        Executors.newSingleThreadExecutor().submit(() -> {
            SseListener sseListener = new SseListener() {
                @Override
                protected void send() {
                    try {
                        String currentData = this.getCurrData();
                        if (currentData != null && !currentData.isEmpty()) {
                            byte[] utf8Bytes = currentData.getBytes(StandardCharsets.UTF_8);
                            emitter.send(utf8Bytes);
                        }
                    } catch (IOException e) {
                        log.error("/chatStream: 通过 send() 方法向 emitter 发送 SSE 数据块时出错: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    } catch (Exception e) {
                        log.error("/chatStream: SseListener 的 send() 方法发生意外错误: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    }
                }
                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    log.info("/chatStream: AI 流连接已打开。响应状态码: {}", response.code());
                }
                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    log.info("/chatStream: AI 流连接已关闭。");
                    String assistantResponse = getOutput().toString();
                    if (!assistantResponse.trim().isEmpty()) {
                        ChatMessage assistantMessage = ChatMessage.withAssistant(assistantResponse);
                        ChatHistory assistantChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, assistantMessage);
                        chatHistoryService.save(assistantChatHistory);
                        log.info("/chatStream: AI 回复已保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        emitter.complete();
                        log.info("/chatStream: 已在保存 AI 回复后将 SseEmitter 标记为完成。");
                    } else {
                        log.warn("/chatStream: AI 回复为空或仅包含空白字符，未保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        emitter.complete();
                        log.info("/chatStream: 已将 SseEmitter 标记为完成，即使 AI 回复为空。");
                    }
                }
                @Override
                public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                    String responseBody = null; int responseCode = -1;
                    if (response != null) {
                        responseCode = response.code();
                        try { if (response.body() != null) { responseBody = response.body().string(); } } catch (IOException e) { log.error("/chatStream: 读取 AI 流失败时的响应体出错", e); }
                    }
                    log.error("/chatStream: AI 流连接失败。状态码: {}, 响应体: {}, 异常: {}", responseCode, responseBody, (t != null ? t.getMessage() : "无异常信息"), t);
                    emitter.completeWithError(t != null ? t : new RuntimeException("AI 流处理失败，响应码: " + responseCode));
                }
            };

            // --- 8. Emitter 事件回调 ---
            emitter.onCompletion(() -> {
                log.info("/chatStream: SseEmitter 已完成。");
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.info("/chatStream: 从 SseEmitter 完成回调中取消 EventSource。"); es.cancel(); }
                log.info("/chatStream: SseEmitter 完成回调处理器执行完毕。");
            });
            emitter.onTimeout(() -> {
                log.warn("/chatStream: SseEmitter 连接超时。");
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.warn("/chatStream: 从 SseEmitter 超时回调中取消 EventSource。"); es.cancel(); }
                emitter.completeWithError(new RuntimeException("SseEmitter 连接超时"));
                log.warn("/chatStream: SseEmitter 超时回调处理器执行完毕。");
            });
            emitter.onError(e -> {
                log.error("/chatStream: SseEmitter 发生错误: {}", e.getMessage(), e);
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.error("/chatStream: 因 SseEmitter 错误，取消 EventSource。"); es.cancel(); }
                log.error("/chatStream: SseEmitter 错误回调处理器执行完毕。");
            });

            // --- 9. 发起 AI 请求 ---
            try {
                log.info("/chatStream: 用户 {} 的会话 {} 正在调用 chatCompletionStream (模型: {})...", userId, finalConversationId, defaultChatModel);
                chatService.chatCompletionStream(chatCompletion, sseListener);
                log.info("/chatStream: chatCompletionStream 方法已为用户 {} 的会话 {} 返回。异步处理继续。", userId, finalConversationId);
            } catch (Exception e) {
                log.error("/chatStream: 为用户 {} 的会话 {} 调用 chatCompletionStream 时发生异常: {}", userId, finalConversationId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        log.info("/chatStream: 为用户 {} 的会话 {} 返回 SseEmitter 对象。", userId, finalConversationId);
        return emitter;
    }


    // ========================================================================
    // ========= 中文注释和中文回复聊天接口 (使用 deepseek-r1 模型) =========
    // ========================================================================

    /**
     * AI 聊天流接口 (中文注释和回复版本)。
     * 通过 URL Query 参数中的 token 识别用户，处理对话流和历史记录。
     * 使用配置文件中指定的中文模型 (ai.model.chinese)。
     * @param token 用户认证 token (在 URL Query 参数 "token" 中)
     * @param question 用户输入的问题 (在 URL Query 参数 "question" 中)
     * @param conversationId 当前对话 ID (可选, 在 URL Query 参数 "conversationId" 中)。如果为 null/空/"new"，则开始新对话。
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    @GetMapping("/chatStreamChinese")
    public SseEmitter getChatMessageStreamChinese(
            @RequestParam String token, // <<< 从 URL Query 参数获取 Token
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        SseEmitter emitter = new SseEmitter(3600000L); // 创建 SseEmitter 对象，设置超时时间

        // --- 1. 从 Token 中解析用户 ID ---
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("/chatStreamChinese: Token解析成功，但未能获取有效的用户ID。Token: {}", token);
                try { emitter.send(SseEmitter.event().name("error").data("用户信息无效，请核对。")); } catch (IOException e) { log.error("/chatStreamChinese: 发送错误事件至客户端失败", e); }
                emitter.completeWithError(new IllegalArgumentException("用户信息无效，请重新登录。"));
                return emitter;
            }
            userId = user.getId();
            log.info("/chatStreamChinese: Token 解析成功，用户 ID: {}", userId);
        } catch (Exception e) {
            log.error("/chatStreamChinese: Token 解析失败。Token: {}", token, e);
            try { emitter.send(SseEmitter.event().name("error").data("认证失败，请重新登录。")); } catch (IOException ioException) { log.error("/chatStreamChinese: 发送错误事件至客户端失败", ioException); }
            emitter.completeWithError(new IllegalArgumentException("认证信息无效或已过期，请重新登录。"));
            return emitter;
        }

        // --- 2. 处理会话 ID ---
        String currentConversationId = conversationId;
        if (currentConversationId == null || currentConversationId.trim().isEmpty() || "new".equalsIgnoreCase(currentConversationId.trim())) {
            currentConversationId = UUID.randomUUID().toString();
            log.info("/chatStreamChinese: 为用户 {} 生成了新的会话 ID: {}", userId, currentConversationId);
            try {
                emitter.send(SseEmitter.event().name("conversationId").data(currentConversationId));
                log.info("/chatStreamChinese: 已将新的会话 ID {} 发送给用户 {}", currentConversationId, userId);
            } catch (IOException e) {
                log.error("/chatStreamChinese: 向客户端发送新的会话 ID 失败。", e);
                emitter.completeWithError(e);
                return emitter;
            }
        } else {
            log.info("/chatStreamChinese: 用户 {} 继续使用现有会话 ID: {}", userId, currentConversationId);
        }

        final String finalConversationId = currentConversationId;
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);
        List<ChatMessage> historyMessages = chatHistoryService.getChatMessagesByUserIdAndConversationId(userId, finalConversationId);
        log.info("/chatStreamChinese: 为用户 {} 的会话 {} 加载了 {} 条历史消息。", userId, finalConversationId, historyMessages.size());
        ChatMessage userMessage = ChatMessage.withUser(question);
        historyMessages.add(userMessage);
        log.info("/chatStreamChinese: 本次发送给 AI 的消息总数 (包括历史): {}", historyMessages.size());
        ChatHistory userChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, userMessage);
        chatHistoryService.save(userChatHistory);
        log.info("/chatStreamChinese: 用户消息已保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);


        // --- 6. 准备调用 AI 服务的请求参数 ---
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(chineseChatModel)
                .messages(historyMessages)
                .build();
        log.info("/chatStreamChinese: 使用模型: {}", chineseChatModel);

        // --- 7. 异步处理 ---
        Executors.newSingleThreadExecutor().submit(() -> {
            SseListener sseListener = new SseListener() {
                @Override
                protected void send() {
                    try {
                        String currentData = this.getCurrData();
                        if (currentData != null && !currentData.isEmpty()) {
                            byte[] utf8Bytes = currentData.getBytes(StandardCharsets.UTF_8);
                            emitter.send(utf8Bytes);
                        }
                    } catch (IOException e) {
                        log.error("/chatStreamChinese: 通过 send() 方法向 emitter 发送 SSE 数据块时出错: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    } catch (Exception e) {
                        log.error("/chatStreamChinese: SseListener 的 send() 方法发生意外错误: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    }
                }
                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    log.info("/chatStreamChinese: AI 流连接已打开。响应状态码: {}", response.code());
                }
                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    log.info("/chatStreamChinese: AI 流连接已关闭。");
                    String assistantResponse = getOutput().toString();
                    if (!assistantResponse.trim().isEmpty()) {
                        ChatMessage assistantMessage = ChatMessage.withAssistant(assistantResponse);
                        ChatHistory assistantChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, assistantMessage);
                        chatHistoryService.save(assistantChatHistory);
                        log.info("/chatStreamChinese: AI 回复已保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        emitter.complete();
                        log.info("/chatStreamChinese: 已在保存 AI 回复后将 SseEmitter 标记为完成。");
                    } else {
                        log.warn("/chatStreamChinese: AI 回复为空或仅包含空白字符，未保存至数据库。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        emitter.complete();
                        log.info("/chatStreamChinese: 已将 SseEmitter 标记为完成，即使 AI 回复为空。");
                    }
                }
                @Override
                public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                    String responseBody = null; int responseCode = -1;
                    if (response != null) {
                        responseCode = response.code();
                        try { if (response.body() != null) { responseBody = response.body().string(); } } catch (IOException e) { log.error("/chatStreamChinese: 读取 AI 流失败时的响应体出错", e); }
                    }
                    log.error("/chatStreamChinese: AI 流连接失败。状态码: {}, 响应体: {}, 异常: {}", responseCode, responseBody, (t != null ? t.getMessage() : "无异常信息"), t);
                    emitter.completeWithError(t != null ? t : new RuntimeException("AI 流处理失败，响应码: " + responseCode));
                }
            };

            // --- 8. Emitter 事件回调 ---
            emitter.onCompletion(() -> {
                log.info("/chatStreamChinese: SseEmitter 已完成。");
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.info("/chatStreamChinese: 从 SseEmitter 完成回调中取消 EventSource。"); es.cancel(); }
                log.info("/chatStreamChinese: SseEmitter 完成回调处理器执行完毕。");
            });
            emitter.onTimeout(() -> {
                log.warn("/chatStreamChinese: SseEmitter 连接超时。");
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.warn("/chatStreamChinese: 从 SseEmitter 超时回调中取消 EventSource。"); es.cancel(); }
                emitter.completeWithError(new RuntimeException("SseEmitter 连接超时"));
                log.warn("/chatStreamChinese: SseEmitter 超时回调处理器执行完毕。");
            });
            emitter.onError(e -> {
                log.error("/chatStreamChinese: SseEmitter 发生错误: {}", e.getMessage(), e);
                EventSource es = sseListener.getEventSource();
                if (es != null) { log.error("/chatStreamChinese: 因 SseEmitter 错误，取消 EventSource。"); es.cancel(); }
                log.error("/chatStreamChinese: SseEmitter 错误回调处理器执行完毕。");
            });

            // --- 9. 发起 AI 请求 ---
            try {
                log.info("/chatStreamChinese: 用户 {} 的会话 {} 正在调用 chatCompletionStream (模型: {})...", userId, finalConversationId, chineseChatModel);
                chatService.chatCompletionStream(chatCompletion, sseListener);
                log.info("/chatStreamChinese: chatCompletionStream 方法已为用户 {} 的会话 {} 返回。异步处理继续。", userId, finalConversationId);
            } catch (Exception e) {
                log.error("/chatStreamChinese: 为用户 {} 的会话 {} 调用 chatCompletionStream 时发生异常: {}", userId, finalConversationId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        log.info("/chatStreamChinese: 为用户 {} 的会话 {} 返回 SseEmitter 对象。", userId, finalConversationId);
        return emitter;
    }


    /**
     * 获取用户所有对话历史记录。通过请求头 "X-Token" 中的 token 识别用户。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
     * @return 用户所有 ChatHistory 列表。
     */
    @GetMapping("/viewHistory")
    public List<ChatHistory> viewHistory(@RequestHeader("X-Token") String token) { // <<< 从 Header "X-Token" 获取 Token
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("viewHistory: Token解析成功，但获取用户ID失败。Token: {}", token);
                return new ArrayList<>();
            }
            userId = user.getId();
            log.info("viewHistory: Token解析成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("viewHistory: Token解析失败。Token: {}", token, e);
            return new ArrayList<>();
        }

        log.info("准备查询用户 {} 的所有聊天历史记录。", userId);
        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatHistory::getUserId, userId);
        queryWrapper.orderByDesc(ChatHistory::getConversationId);
        queryWrapper.orderByAsc(ChatHistory::getTimestamp);

        List<ChatHistory> historyList = chatHistoryService.list(queryWrapper);
        log.info("为用户 {} 查询到 {} 条历史记录。", userId, historyList.size());
        return historyList;
    }

    /**
     * 删除用户部分或全部对话历史记录。通过请求头 "X-Token" 中的 token 识别用户。 // <--- 修改描述
     * @param token 用户认证 token (在 Request Header "X-Token" 中) // <--- 修改描述
     * @param conversationId 要删除的特定对话 ID (可选, 在 URL Query 参数 "conversationId" 中)。如果未提供，则删除用户所有历史。
     * @return 包含删除结果的字符串消息 (中文)。
     */
    @GetMapping("/resetHistory")
    public String resetHistory(
            @RequestHeader("X-Token") String token, // <<< 改回从 Header "X-Token" 获取 Token
            @RequestParam(required = false) String conversationId // conversationId 仍然从 Query 参数获取
    ) {
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("resetHistory: Token解析成功，但获取用户ID失败。Token: {}", token);
                return "删除失败：无法识别有效的用户信息。";
            }
            userId = user.getId();
            log.info("resetHistory: Token解析成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("resetHistory: Token解析失败。Token: {}", token, e);
            return "删除失败：认证信息无效或已过期，请重新登录。";
        }

        int deletedCount;
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            log.info("准备为用户 {} 删除会话 {} 的历史记录。", userId, conversationId);
            deletedCount = chatHistoryService.deleteChatHistoryByUserIdAndConversationId(userId, conversationId);
            log.info("为用户 {} 删除了会话 {} 的 {} 条历史记录。", userId, conversationId, deletedCount);
            return String.format("用户 %d 的会话 %s 已清空，共删除 %d 条记录。", userId, conversationId, deletedCount);
        } else {
            log.info("准备为用户 {} 删除所有历史记录。", userId);
            deletedCount = chatHistoryService.deleteAllChatHistoryByUserId(userId);
            log.info("为用户 {} 删除了所有历史记录，共 {} 条。", userId, deletedCount);
            return String.format("用户 %d 的所有聊天历史记录已清空，共删除 %d 条记录。", userId, deletedCount);
        }
    }
}