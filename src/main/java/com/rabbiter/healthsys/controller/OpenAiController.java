package com.rabbiter.healthsys.controller;

// 导入 AI4J 库的类，使用 io.github.lnyocly.ai4j 包名
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factor.AiService;

// 导入我们新创建的 Service 和 Entity
import com.rabbiter.healthsys.entity.ChatHistory;
import com.rabbiter.healthsys.service.IChatHistoryService;
import com.rabbiter.healthsys.config.JwtConfig; // 导入 JwtConfig
import com.rabbiter.healthsys.entity.User; // 导入 User Entity

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.Response;

import org.jetbrains.annotations.NotNull; // 保持原有的NotNull导入
import org.springframework.web.bind.annotation.*; // 导入 @RequestHeader
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

//联网ai服务端
@RestController
@RequiredArgsConstructor
@Slf4j
// 可以给 AI 相关的接口设置一个统一的前缀，例如 /api/ai 或 /ai
// @RequestMapping("/api/ai") // 示例：添加一个统一的请求路径前缀
public class OpenAiController {

    private final AiService aiService;
    private final IChatHistoryService chatHistoryService;
    private final JwtConfig jwtConfig; // 注入 JwtConfig 用于解析 Token

    /**
     * AI 聊天流接口。通过用户 token 识别用户，处理对话流和历史记录。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
     * @param question 用户输入的问题
     * @param conversationId 当前对话 ID (可选)。如果为 null/空/"new"，则开始新对话。
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    @GetMapping("/chatStream")
    public SseEmitter getChatMessageStream(
            @RequestHeader("X-Token") String token, // 从 Header 获取 Token
            @RequestParam String question,
            @RequestParam(required = false) String conversationId
    ) {
        SseEmitter emitter = new SseEmitter(3600000L); // 设置超时时间

        // --- 1. 从 Token 中解析用户 ID ---
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class); // 解析 Token 获取 User 对象
            if (user == null || user.getId() == null) {
                // Token 有效，但 User 对象或其 ID 为 null (异常情况)
                log.error("Token解析成功，但获取用户ID失败。Token: {}", token);
                // 发送错误信息到前端并通过 emitter 完成连接
                try { emitter.send(SseEmitter.event().name("error").data("无效的用户信息。")); } catch (IOException e) { log.error("发送错误事件失败", e); }
                emitter.completeWithError(new IllegalArgumentException("无效的用户信息，请重新登录。"));
                return emitter;
            }
            userId = user.getId(); // 获取用户 ID
            log.info("Token解析成功，用户ID: {}", userId);
        } catch (Exception e) {
            // Token 解析失败 (无效、过期、签名错误等)
            log.error("Token解析失败。Token: {}", token, e);
            // 发送错误信息到前端并通过 emitter 完成连接
            try { emitter.send(SseEmitter.event().name("error").data("认证失败，请重新登录。")); } catch (IOException ioException) { log.error("发送错误事件失败", ioException); }
            emitter.completeWithError(new IllegalArgumentException("无效或过期的认证信息，请重新登录。"));
            return emitter;
        }

        // --- 2. 处理 Conversation ID ---
        String currentConversationId = conversationId;
        if (currentConversationId == null || currentConversationId.trim().isEmpty() || "new".equalsIgnoreCase(currentConversationId.trim())) {
            // 如果前端没有提供对话ID或要求新建，则生成一个新的
            currentConversationId = UUID.randomUUID().toString();
            log.info("生成新的对话ID: {} for user {}", currentConversationId, userId);
            // 作为 SSE 的第一个事件发送回前端，通知前端新的对话 ID
            try {
                // 使用标准 SSE event 格式发送特殊事件
                emitter.send(SseEmitter.event().name("conversationId").data(currentConversationId));
                log.info("发送新的 conversationId {} 给用户 {}", currentConversationId, userId);
            } catch (IOException e) {
                log.error("发送新的 conversationId 到前端失败。", e);
                emitter.completeWithError(e); // 如果发送 ID 都失败了，连接就中断了
                return emitter;
            }
        } else {
            log.info("使用现有对话ID: {} for user {}", currentConversationId, userId);
        }

        final String finalConversationId = currentConversationId; // 用于 Lambda 表达式

        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);

        // --- 3. 从数据库加载历史对话 (使用解析出的 userId 和处理后的 conversationId) ---
        List<ChatMessage> historyMessages = chatHistoryService.getChatMessagesByUserIdAndConversationId(userId, finalConversationId);
        log.info("为用户 {} 对话 {} 加载到 {} 条历史消息。", userId, finalConversationId, historyMessages.size());

        // --- 4. 向历史中添加当前用户输入 ---
        ChatMessage userMessage = ChatMessage.withUser(question);
        historyMessages.add(userMessage);
        log.info("当前发送给AI的历史消息总数: {}", historyMessages.size());

        // --- 5. 保存用户消息到数据库 ---
        // ChatHistory.fromChatMessage 方法需要 user_id 和 conversation_id
        ChatHistory userChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, userMessage);
        chatHistoryService.save(userChatHistory); // 使用 Service 保存到数据库
        log.info("用户消息已保存到数据库。用户ID: {}, 对话ID: {}", userId, finalConversationId);


        // 6. 创建请求参数，添加完整历史消息列表
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("deepseek-chat")
                .messages(historyMessages) // 添加完整历史消息列表 (包含从DB加载的和当前用户消息)
                .build();

        Executors.newSingleThreadExecutor().submit(() -> {
            SseListener sseListener = new SseListener() {

                // --- 必须实现的 abstract 方法 ---
                // SseListener 要求实现 send() 方法，负责将数据块发送到客户端
                @Override
                protected void send() {
                    try {
                        String currentData = this.getCurrData();
                        if (currentData != null && !currentData.isEmpty()) {
                            // 恢复原始的发送逻辑：转换为 UTF-8 字节并发送
                            // 如果前端需要标准的 SSE data: 格式，这里要改回 emitter.send(SseEmitter.event().data(currentData));
                            // 根据你的描述乱码问题，这里保持发送原始字节流。
                            byte[] utf8Bytes = currentData.getBytes(StandardCharsets.UTF_8);
                            emitter.send(utf8Bytes);
                            // log.debug("[Server] Sent raw UTF-8 bytes successfully for: [" + currentData + "]");
                        } else {
                            // log.debug("[Server] Received empty or null data chunk via send(), skipping.");
                        }
                    } catch (IOException e) {
                        log.error("Error sending SSE chunk to emitter via send(): {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    } catch (Exception e) {
                        log.error("Unexpected error in SseListener send(): {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                        EventSource es = getEventSource();
                        if (es != null) { es.cancel(); }
                    }
                }

                // --- 其他覆盖方法，根据之前的错误，这些需要是 public ---

                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    log.info("AI Stream connection opened. Response code: {}", response.code());
                }

                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    log.info("AI Stream connection closed.");
                }

                // onComplete 方法需要是 public
                public void onComplete() {
                    log.info("AI Stream completed (SseListener onComplete).");
                    // 获取完整的 AI 回复
                    String assistantResponse = getOutput().toString();
                    log.info("完整的AI回复：{}", assistantResponse != null ? assistantResponse.substring(0, Math.min(assistantResponse.length(), 200)) + (assistantResponse.length() > 200 ? "..." : "") : "null");

                    if (assistantResponse != null && !assistantResponse.trim().isEmpty()) {
                        // --- 保存 AI 回复到数据库 ---
                        ChatMessage assistantMessage = ChatMessage.withAssistant(assistantResponse);
                        ChatHistory assistantChatHistory = ChatHistory.fromChatMessage(userId, finalConversationId, assistantMessage); // 使用解析出的 userId 和 finalConversationId
                        chatHistoryService.save(assistantChatHistory); // 使用 Service 保存到数据库
                        log.info("AI回复已保存到数据库。用户ID: {}, 对话ID: {}", userId, finalConversationId);
                    } else {
                        log.warn("AI回复为空或只有空白字符，未保存到数据库。");
                    }

                    // 通知 emitter 完成
                    try {
                        emitter.complete();
                        log.info("Emitter completed successfully from SseListener onComplete.");
                    } catch (Exception e) {
                        log.error("Error during emitter.complete() in onComplete: {}", e.getMessage(), e);
                    }
                }

                // onError 方法需要是 public
                public void onError(Throwable t) {
                    log.error("AI Stream error (SseListener onError): {}", t.getMessage(), t);
                    // 通知 emitter 完成并带上错误
                    try {
                        emitter.completeWithError(t);
                        log.error("Emitter completed with error from SseListener onError.");
                    } catch (Exception e) {
                        log.error("Error during emitter.completeWithError() in onError: {}", e.getMessage(), e);
                    }

                    // 取消 EventSource
                    EventSource es = getEventSource();
                    if (es != null) {
                        log.info("Cancelling EventSource from listener error.");
                        es.cancel();
                    }
                }
            };

            // --- Emitter 事件处理 ---
            emitter.onCompletion(() -> {
                log.info("Emitter completed (client disconnected or stream finished via emitter.complete()).");
                EventSource es = sseListener.getEventSource();
                if (es != null) {
                    log.info("Cancelling EventSource from emitter completion.");
                    es.cancel();
                }
                log.info("Emitter completion handler finished.");
            });

            emitter.onTimeout(() -> {
                log.warn("Emitter timed out.");
                EventSource es = sseListener.getEventSource();
                if (es != null) {
                    log.warn("Cancelling EventSource from emitter timeout.");
                    es.cancel();
                }
                emitter.completeWithError(new RuntimeException("Emitter timed out"));
                log.warn("Emitter timeout handler finished.");
            });

            emitter.onError(e -> {
                log.error("Emitter error: {}", e.getMessage(), e);
                EventSource es = sseListener.getEventSource();
                if (es != null) { es.cancel(); }
                emitter.completeWithError(e);
                log.error("Emitter error handler finished.");
            });

            // --- 执行 AI 聊天流请求 ---
            try {
                log.info("用户 {} 对话 {} 调用 chatCompletionStream 开始流式处理...", userId, finalConversationId);
                chatService.chatCompletionStream(chatCompletion, sseListener);
                log.info("chatCompletionStream 方法已返回。异步处理通过 SseListener 继续。", userId, finalConversationId);
            } catch (Exception e) {
                log.error("用户 {} 对话 {} 调用 chatCompletionStream 发生异常：{}", userId, finalConversationId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        log.info("为用户 {} 对话 {} 返回 SseEmitter。", userId, finalConversationId);
        return emitter;
    }

    /**
     * 获取用户所有对话历史记录。通过用户 token 识别用户。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
     * @return 用户所有 ChatHistory 列表。
     */
    @GetMapping("/viewHistory")
    public List<ChatHistory> viewHistory(@RequestHeader("X-Token") String token) {// 从 Header 获取 Token
        // --- 1. 从 Token 中解析用户 ID ---
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("Token解析成功，但获取用户ID失败。Token: {}", token);
                // 返回空列表并记录错误，客户端应根据列表为空判断
                return new ArrayList<>();
            }
            userId = user.getId();
            log.info("viewHistory: Token解析成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("viewHistory: Token解析失败。Token: {}", token, e);
            // 返回空列表并记录错误
            return new ArrayList<>();
        }

        log.info("查看用户 {} 的所有历史记录。", userId);
        LambdaQueryWrapper<ChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatHistory::getUserId, userId);
        queryWrapper.orderByDesc(ChatHistory::getConversationId); // 按对话ID分组排序 (新的对话在前)
        queryWrapper.orderByAsc(ChatHistory::getTimestamp); // 同一对话按时间排序

        List<ChatHistory> historyList = chatHistoryService.list(queryWrapper); // 使用 IService 的 list 方法
        log.info("为用户 {} 找到了 {} 条历史记录。", userId, historyList.size());
        return historyList;
    }

    /**
     * 删除用户部分或全部对话历史记录。通过用户 token 识别用户。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
     * @param conversationId 要删除的特定对话 ID (可选)。如果未提供，则删除用户所有历史。
     * @return 包含删除结果的字符串消息。
     */
    @GetMapping("/resetHistory") // 注意：删除操作通常使用 DELETE 方法更符合 RESTful 风格
    public String resetHistory(
            @RequestHeader("X-Token") String token, // 从 Header 获取 Token
            @RequestParam(required = false) String conversationId
    ) {
        // --- 1. 从 Token 中解析用户 ID ---
        Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("Token解析成功，但获取用户ID失败。Token: {}", token);
                return "删除失败：无效的用户信息。";
            }
            userId = user.getId();
            log.info("resetHistory: Token解析成功，用户ID: {}", userId);
        } catch (Exception e) {
            log.error("resetHistory: Token解析失败。Token: {}", token, e);
            return "删除失败：认证信息无效或过期，请重新登录。";
        }

        int deletedCount = 0;
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            // 删除特定对话
            deletedCount = chatHistoryService.deleteChatHistoryByUserIdAndConversationId(userId, conversationId);
            log.info("为用户 {} 删除对话 {} 的 {} 条历史记录。", userId, conversationId, deletedCount);
            return String.format("用户 %d 的对话 %s 已清空，共删除 %d 条记录。", userId, conversationId, deletedCount);
        } else {
            // 删除该用户的所有历史记录
            deletedCount = chatHistoryService.deleteAllChatHistoryByUserId(userId);
            log.info("为用户 {} 删除所有 {} 条历史记录。", userId, deletedCount);
            return String.format("用户 %d 的所有历史记录已清空，共删除 %d 条记录。", userId, deletedCount);
        }
    }
}