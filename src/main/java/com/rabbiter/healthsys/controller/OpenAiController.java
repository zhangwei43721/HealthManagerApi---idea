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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.springframework.lang.Nullable;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

/**
 * AI聊天接口
 * 
 * @author Skyforever
 * @since 2025-05-01
 */
@RestController
@RequiredArgsConstructor // 自动生成包含 final 字段的构造函数
@Slf4j // Lombok 注解，用于自动生成日志记录器
public class OpenAiController {

    private final AiService aiService; // 注入 AI 服务工厂
    private final IChatHistoryService chatHistoryService; // 注入聊天历史记录服务
    private final JwtConfig jwtConfig; // 注入 JWT 配置，用于 Token 解析
    private final RestTemplate restTemplate; // 注入 RestTemplate 用于 HTTP 请求
    private final ObjectMapper objectMapper; // 注入 ObjectMapper 用于 JSON 处理

    // --- 从配置文件注入模型名称 ---
    @Value("${ai.model.default}") // 注入默认接口使用的模型名称
    private String defaultChatModel;

    @Value("${ai.model.chinese}") // 注入中文接口使用的模型名称 (deepseek-r1)
    private String chineseChatModel;
    
    // 注入YOLOv10图片检测相关配置
    @Value("${ai.image-detection.api-url}")
    private String imageDetectionApiUrl;
    
    @Value("${ai.image-detection.base-url}")
    private String imageDetectionBaseUrl;
    
    @Value("${ai.image-detection.success-prompt}")
    private String imageDetectionSuccessPrompt;
    
    @Value("${ai.image-detection.error-prompt}")
    private String imageDetectionErrorPrompt;
    
    @Value("${ai.image-detection.api-error-prompt}")
    private String imageDetectionApiErrorPrompt;
    
    @Value("${ai.image-detection.exception-prompt}")
    private String imageDetectionExceptionPrompt;

    /**
     * AI 聊天流接口
     * 通过 URL Query 参数中的 token 识别用户，处理对话流和历史记录。
     * 使用配置文件中指定的默认模型 (ai.model.default)。
     * @param token 用户认证 token (在 URL Query 参数 "token" 中)
     * @param file 可选的图片文件 (在 form-data 的 'file' 字段中)
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    @PostMapping(value = "/chatStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter getChatMessageStream(
            @RequestHeader("X-Token") String token,
            @RequestPart("message") String messageParam, // 重命名原始参数
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "conversationId", required = false) String conversationIdParam // 重命名原始参数
    ) {
        SseEmitter emitter = new SseEmitter(3600000L); // 设置 SSE 超时时间 (单位: 毫秒)

        String message = messageParam; // 将参数赋值给可修改的局部变量
        String conversationIdStr = conversationIdParam; // 将参数赋值给可修改的局部变量

        // --- 新增：对输入参数进行手动的UTF-8重编码 ---
        try {
            if (message != null) {
                message = new String(message.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                log.debug("/chatStream: Re-encoded message from ISO-8859-1 to UTF-8. Original length: {}, New length: {}", messageParam != null ? messageParam.length() : "null", (message != null ? message.length() : "null"));
            }
            if (conversationIdStr != null && !conversationIdStr.trim().isEmpty() && !"new".equalsIgnoreCase(conversationIdStr.trim())) {
                conversationIdStr = new String(conversationIdStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                log.debug("/chatStream: Re-encoded conversationId from ISO-8859-1 to UTF-8. Original: [{}], New: [{}]", conversationIdParam, conversationIdStr);
            }
        } catch (Exception e) {
            log.error("/chatStream: Error during manual UTF-8 re-encoding of request parts. Original message: '{}', Original conversationId: '{}'", messageParam, conversationIdParam, e);
        }
        // --- 结束手动重编码 ---

        final String finalMessage = message; // <<< 新增 final 变量以供 lambda 使用
        final String finalConversationIdForLambda = conversationIdStr; // <<< 重命名以明确用途

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
        String currentConversationId = finalConversationIdForLambda; // 使用 final 变量
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
        final Integer finalUserId = userId; // Use final variable for lambda

        // --- 新增：异步处理图片和构建消息 ---
        Executors.newSingleThreadExecutor().submit(() -> {
            String processedMessage = finalMessage; // <<< 使用 finalMessage
            String detectionResultImageUrl; // 检测结果图片 URL
            String detectionInfo; // 检测结果文字描述

            // --- 3. (如果提供了文件) 调用对象检测 API ---
            if (file != null && !file.isEmpty()) {
                log.info("/chatStream: 用户 {} 在会话 {} 中上传了图片 {} ({} bytes)，将调用对象检测 API。",
                         finalUserId, finalConversationId, file.getOriginalFilename(), file.getSize());
                try {
                    // 使用配置的API URL而不是硬编码
                    String detectionApiUrl = imageDetectionApiUrl;

                    // 准备请求头
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    // 准备请求体
                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("file", file.getResource()); // 使用 getResource() 避免文件存储问题

                    // 创建 HTTP 实体
                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                    // 发送 POST 请求
                    ResponseEntity<String> responseEntity = restTemplate.postForEntity(detectionApiUrl, requestEntity, String.class);

                    if (responseEntity.getStatusCode() == HttpStatus.OK) {
                        String responseBody = responseEntity.getBody();
                        log.info("/chatStream: 对象检测 API 成功响应: {}", responseBody);

                        // 解析 JSON 响应
                        Map<String, Object> detectionResponse = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                        if ("success".equals(detectionResponse.get("status"))) {
                            List<Map<String, Object>> detections = (List<Map<String, Object>>) detectionResponse.get("detections");
                            detectionResultImageUrl = (String) detectionResponse.get("result_image_url"); // 获取结果图片 URL

                            if (detections != null && !detections.isEmpty()) {
                                StringBuilder sb = new StringBuilder("图片中检测到以下对象：");
                                for (Map<String, Object> det : detections) {
                                    sb.append(String.format("%s (置信度: %.2f), ", det.get("class"), det.get("confidence")));
                                }
                                // 移除末尾的逗号和空格
                                if (sb.length() > "图片中检测到以下对象：".length()) {
                                    sb.setLength(sb.length() - 2);
                                }
                                sb.append("。");
                                detectionInfo = sb.toString();
                                log.info("/chatStream: 检测结果描述: {}", detectionInfo);
                            } else {
                                detectionInfo = "图片中未检测到任何对象。";
                                log.info("/chatStream: {}", detectionInfo);
                            }
                            // 使用配置的提示词模板而不是硬编码
                            String resultImageInfo = detectionResultImageUrl != null ? 
                                "处理后的图片地址：" + imageDetectionBaseUrl + detectionResultImageUrl : "";
                            processedMessage = String.format(imageDetectionSuccessPrompt, detectionInfo, resultImageInfo, finalMessage); // <<< 使用 finalMessage

                            // 可以考虑将检测结果图片 URL 通过 SSE 发送给前端
                            if (detectionResultImageUrl != null) {
                                try {
                                    // 使用配置的基础URL
                                    emitter.send(SseEmitter.event().name("detectionResultImage").data(imageDetectionBaseUrl + detectionResultImageUrl));
                                    log.info("/chatStream: 已将检测结果图片 URL 发送给客户端: {}", imageDetectionBaseUrl + detectionResultImageUrl);
                                } catch (IOException e) {
                                    log.error("/chatStream: 发送检测结果图片 URL 至客户端失败", e);
                                }
                            }

                        } else {
                            String errorMsg = (String) detectionResponse.getOrDefault("message", "检测失败，未提供具体原因。");
                            log.error("/chatStream: 对象检测 API 返回错误状态: {}", errorMsg);
                            // 使用配置的错误提示词模板
                            processedMessage = String.format(imageDetectionErrorPrompt, errorMsg, finalMessage); // <<< 使用 finalMessage
                        }
                    } else {
                        log.error("/chatStream: 调用对象检测 API 失败，状态码: {}", responseEntity.getStatusCode());
                        // 使用配置的API错误提示词模板
                        processedMessage = String.format(imageDetectionApiErrorPrompt, responseEntity.getStatusCodeValue(), finalMessage); // <<< 使用 finalMessage
                    }

                } catch (Exception e) {
                    log.error("/chatStream: 调用对象检测 API 或处理其响应时发生异常", e);
                    // 使用配置的异常处理提示词模板
                    processedMessage = String.format(imageDetectionExceptionPrompt, e.getMessage(), finalMessage); // <<< 使用 finalMessage
                }
            } else {
                log.info("/chatStream: 用户 {} 在会话 {} 中未上传图片，直接处理消息。", finalUserId, finalConversationId);
            }
            // --- 结束图片处理 ---

            // --- 4. 获取聊天历史记录 ---
            IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK); // 确认使用 DeepSeek
            List<ChatMessage> historyMessages = chatHistoryService.getChatMessagesByUserIdAndConversationId(finalUserId, finalConversationId);
            log.info("/chatStream: 为用户 {} 的会话 {} 加载了 {} 条历史消息。", finalUserId, finalConversationId, historyMessages.size());

            // --- 5. 添加当前 (可能已处理过的) 消息到历史记录并保存 ---
            ChatMessage userMessage = ChatMessage.withUser(processedMessage); // 使用处理后的消息
            historyMessages.add(userMessage);
            log.info("/chatStream: 本次发送给 AI 的消息总数 (包括历史): {}。处理后的用户消息: {}", historyMessages.size(), processedMessage);

            // 保存原始用户消息和可能的检测信息到数据库
            // 注意：这里保存的是组合后的消息，如果需要区分保存，需要调整 ChatHistory 实体和逻辑
            ChatHistory userChatHistory = ChatHistory.fromChatMessage(finalUserId, finalConversationId, userMessage);
            chatHistoryService.save(userChatHistory);
            log.info("/chatStream: 用户消息 (可能包含图片信息) 已保存至数据库。用户 ID: {}, 会话 ID: {}", finalUserId, finalConversationId);


            // --- 6. 准备调用 AI 服务的请求参数 ---
            ChatCompletion chatCompletion = ChatCompletion.builder()
                    .model(defaultChatModel) // 确认使用哪个模型
                    .messages(historyMessages)
                    .build();
            log.info("/chatStream: 使用模型: {}", defaultChatModel);

            // --- 7. 定义 SseListener (与之前类似，但 userId 和 conversationId 需要是 final 或 effectively final) ---
            SseListener sseListener = new SseListener() {
                 private final StringBuilder assistantResponseBuilder = new StringBuilder(); // 用于累积 AI 回复

                @Override
                protected void send() {
                    try {
                        String currentData = this.getCurrData();
                        if (currentData != null && !currentData.isEmpty()) {
                            assistantResponseBuilder.append(currentData); // 累积数据块
                            byte[] utf8Bytes = currentData.getBytes(StandardCharsets.UTF_8);
                            emitter.send(utf8Bytes);
                        }
                    } catch (IOException e) {
                        log.error("/chatStream: 通过 send() 方法向 emitter 发送 SSE 数据块时出错: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    } catch (Exception e) {
                        log.error("/chatStream: SseListener 的 send() 方法发生意外错误: {}", e.getMessage(), e);
                        emitter.completeWithError(e);
                    }
                }
                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    log.info("/chatStream: AI 流连接已打开。用户 ID: {}, 会话 ID: {}, 响应状态码: {}", finalUserId, finalConversationId, response.code());
                }
                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    log.info("/chatStream: AI 流连接已关闭。用户 ID: {}, 会话 ID: {}", finalUserId, finalConversationId);
                    
                    // 获取输出并确保使用UTF-8编码
                    String rawOutput = this.getOutput().toString();
                    
                    // 重新以UTF-8编码处理响应文本
                    String assistantResponse;
                    try {
                        // 转换为UTF-8字节然后再转回字符串，保持与send()方法相同的编码处理
                        byte[] utf8Bytes = rawOutput.getBytes(StandardCharsets.UTF_8);
                        assistantResponse = new String(utf8Bytes, StandardCharsets.UTF_8).trim();
                        log.info("/chatStream: 处理后的AI回复使用UTF-8编码。用户 ID: {}, 会话 ID: {}", finalUserId, finalConversationId);
                    } catch (Exception e) {
                        log.error("/chatStream: 处理AI回复UTF-8编码时出错: {}", e.getMessage(), e);
                        assistantResponse = rawOutput.trim(); // 回退到原始处理
                    }
                    
                    if (!assistantResponse.isEmpty()) {
                        ChatMessage assistantMessage = ChatMessage.withAssistant(assistantResponse);
                        ChatHistory assistantChatHistory = ChatHistory.fromChatMessage(finalUserId, finalConversationId, assistantMessage);
                        chatHistoryService.save(assistantChatHistory);
                        log.info("/chatStream: AI 回复已保存至数据库。用户 ID: {}, 会话 ID: {}, 回复长度: {}", 
                                finalUserId, finalConversationId, assistantResponse.length());
                    } else {
                        log.warn("/chatStream: AI 回复为空或仅包含空白字符，未保存至数据库。用户 ID: {}, 会话 ID: {}", 
                                finalUserId, finalConversationId);
                    }
                    emitter.complete();
                    log.info("/chatStream: 已在 AI 流关闭后将 SseEmitter 标记为完成。用户 ID: {}, 会话 ID: {}", 
                            finalUserId, finalConversationId);
                }
                @Override
                public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                    String responseBody = null; int responseCode = -1;
                    if (response != null) {
                        responseCode = response.code();
                        try { if (response.body() != null) { responseBody = response.body().string(); } } catch (IOException e) { log.error("/chatStream: 读取 AI 流失败时的响应体出错", e); }
                    }
                    log.error("/chatStream: AI 流连接失败。用户 ID: {}, 会话 ID: {}, 状态码: {}, 响应体: {}, 异常: {}",
                              finalUserId, finalConversationId, responseCode, responseBody, (t != null ? t.getMessage() : "无异常信息"), t);
                    emitter.completeWithError(t != null ? t : new RuntimeException("AI 流处理失败，响应码: " + responseCode));
                }
            };

            // --- 8. Emitter 事件回调 (移除对 cancelEventSource 的直接调用，依赖 SseListener 和 Emitter 自身的处理) ---
            emitter.onCompletion(() -> log.info("/chatStreamLifecycle: SseEmitter 完成。用户 ID: {}, 会话 ID: {}", finalUserId, finalConversationId));
            emitter.onTimeout(() -> {
                log.warn("/chatStreamLifecycle: SseEmitter 超时。用户 ID: {}, 会话 ID: {}", finalUserId, finalConversationId);
                EventSource es = sseListener.getEventSource(); // 尝试获取 EventSource
                 if (es != null) {
                    log.warn("/chatStreamLifecycle: 因 SseEmitter 超时，尝试取消 EventSource。");
                    es.cancel();
                 }
                emitter.completeWithError(new RuntimeException("请求处理超时"));
            });
            emitter.onError(e -> {
                log.error("/chatStreamLifecycle: SseEmitter 发生错误。用户 ID: {}, 会话 ID: {}. 错误: {}", finalUserId, finalConversationId, e.getMessage(), e);
                EventSource es = sseListener.getEventSource(); // 尝试获取 EventSource
                 if (es != null) {
                     log.error("/chatStreamLifecycle: 因 SseEmitter 错误，尝试取消 EventSource。");
                     es.cancel();
                 }
            });


            // --- 9. 发起 AI 请求 ---
            try {
                log.info("/chatStream: 用户 {} 的会话 {} 正在调用 chatCompletionStream (模型: {})...", finalUserId, finalConversationId, defaultChatModel);
                chatService.chatCompletionStream(chatCompletion, sseListener);
                log.info("/chatStream: chatCompletionStream 方法已为用户 {} 的会话 {} 返回。异步处理继续。", finalUserId, finalConversationId);
            } catch (Exception e) {
                log.error("/chatStream: 为用户 {} 的会话 {} 调用 chatCompletionStream 时发生异常: {}", finalUserId, finalConversationId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });
        // --- 结束异步处理 ---

        log.info("/chatStream: 为用户 {} 的会话 {} 返回 SseEmitter 对象。", userId, finalConversationIdForLambda); // <<< 使用 finalConversationIdForLambda
        return emitter;
    }


    /**
     * AI 聊天流接口
     * 只用于AiSuggestionsSpecificController.java生成报告时使用
     * 通过 URL Query 参数中的 token 识别用户，处理对话流和历史记录。
     * 使用配置文件中指定的中文模型 (ai.model.chinese)。
     * @param token 用户认证 token (在 URL Query 参数 "token" 中)
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    public SseEmitter getChatMessageStreamChinese(
            @RequestHeader("X-Token") String token,
            @RequestParam String questionParam, // 重命名原始参数
            @RequestParam(required = false) String conversationIdParam, // 重命名原始参数
            @Nullable Consumer<String> onCompleteCallback) {
        SseEmitter emitter = new SseEmitter(3600000L); // 1 小时超时

        String question = questionParam;
        String conversationIdStr = conversationIdParam;

        // --- 新增：对输入参数进行手动的UTF-8重编码 ---
        try {
            if (question != null) {
                question = new String(question.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                log.debug("/chatStreamChinese: Re-encoded question from ISO-8859-1 to UTF-8. Original length: {}, New length: {}", questionParam != null ? questionParam.length() : "null", question.length());
            }
            if (conversationIdStr != null && !conversationIdStr.trim().isEmpty() && !"new".equalsIgnoreCase(conversationIdStr.trim())) {
                conversationIdStr = new String(conversationIdStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                log.debug("/chatStreamChinese: Re-encoded conversationId from ISO-8859-1 to UTF-8. Original: [{}], New: [{}]", conversationIdParam, conversationIdStr);
            }
        } catch (Exception e) {
            log.error("/chatStreamChinese: Error during manual UTF-8 re-encoding of request params. Original question: '{}', Original conversationId: '{}'", questionParam, conversationIdParam, e);
        }
        // --- 结束手动重编码 ---

        // --- 1. 用户认证 ---
        final Integer userId;
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("/chatStreamChinese: 无效的用户信息。Token: {}", token);
                completeEmitterWithError(emitter, "用户信息无效，请核对。", null);
                return emitter;
            }
            userId = user.getId();
            log.info("/chatStreamChinese: 用户认证成功。用户 ID: {}", userId);
        } catch (Exception e) {
            log.error("/chatStreamChinese: Token 解析失败。Token: {}", token, e);
            completeEmitterWithError(emitter, "认证失败，请重新登录。", e);
            return emitter;
        }
        // --- 2. 会话 ID 处理 (主要用于日志和客户端) ---
        String currentConversationId = conversationIdStr;
        if (currentConversationId == null || currentConversationId.trim().isEmpty() || "new".equalsIgnoreCase(currentConversationId.trim())) {
            currentConversationId = UUID.randomUUID().toString();
            log.info("/chatStreamChinese: 为用户 {} 生成了新的会话 ID (用于跟踪): {}", userId, currentConversationId);
            // 仍然发送给客户端，以便客户端可以跟踪
            sendSseEvent(emitter, "conversationId", currentConversationId, userId, currentConversationId);
        } else {
            log.info("/chatStreamChinese: 用户 {} 继续使用现有会话 ID (用于跟踪): {}", userId, currentConversationId);
        }
        // 使用 final 变量以便 lambda 访问
        final String finalConversationId = currentConversationId;
        // --- 3. 获取 AI 服务 ---
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);
        // --- 4. 准备 AI 请求 ---
        // 简化：假设 'question' 是完整提示，不再加载历史记录
        List<ChatMessage> messages = Collections.singletonList(ChatMessage.withUser(question));
        log.info("/chatStreamChinese: 准备发送给 AI 的消息数量: 1 (仅当前用户提示)。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(chineseChatModel)
                .messages(messages)
                .build();
        log.info("/chatStreamChinese: 使用模型: {}。用户 ID: {}, 会话 ID: {}", chineseChatModel, userId, finalConversationId);
        // --- 5. 异步处理 AI 请求与 SSE ---
        Executors.newSingleThreadExecutor().submit(() -> {
            SseListener sseListener = new SseListener() {
                @Override
                protected void send() {
                    try {
                        String currentData = this.getCurrData();
                        if (currentData != null && !currentData.isEmpty()) {
                            // 发送数据块给客户端
                            sendSseEvent(emitter, "message", currentData, userId, finalConversationId);
                        }
                    } catch (Exception e) { // 捕获所有可能的异常，包括 IOException
                        log.error("/chatStreamChinese: SseListener.send() 异常: {}。用户 ID: {}, 会话 ID: {}", e.getMessage(), userId, finalConversationId, e);
                        emitter.completeWithError(e); // 标记 Emitter 错误
                    }
                }
                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    log.info("/chatStreamChinese: AI 流连接已打开。用户 ID: {}, 会话 ID: {}, 状态码: {}", userId, finalConversationId, response.code());
                }
                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    log.info("/chatStreamChinese: AI 流连接已关闭 (onClosed)。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                    String assistantResponse = getOutput().toString().trim();
                    // --- 核心逻辑：执行回调并完成 Emitter ---
                    if (!assistantResponse.isEmpty()) {
                        // 移除数据库保存 AI 回复的逻辑
                        // 执行完成回调（如果提供）
                        if (onCompleteCallback != null) {
                            try {
                                log.info("/chatStreamChinese: 准备执行 onCompleteCallback。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                                onCompleteCallback.accept(assistantResponse);
                                log.info("/chatStreamChinese: onCompleteCallback 执行完毕。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                            } catch (Exception e) {
                                log.error("/chatStreamChinese: onCompleteCallback 执行异常。用户 ID: {}, 会话 ID: {}", userId, finalConversationId, e);
                                // 记录回调错误，但通常继续完成 Emitter
                            }
                        } else {
                            log.info("/chatStreamChinese: 未提供 onCompleteCallback。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        }

                        // 正常完成 SSE 连接
                        emitter.complete();
                        log.info("/chatStreamChinese: SseEmitter 已完成 (AI 有效回复)。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);

                    } else {
                        log.warn("/chatStreamChinese: AI 回复为空，不执行回调。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        emitter.complete(); // 即使回复为空也正常完成
                        log.info("/chatStreamChinese: SseEmitter 已完成 (AI 回复为空)。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                    }
                }
                @Override
                public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                    String errorDetails = buildErrorDetails(response);
                    log.error("/chatStreamChinese: AI 流连接失败。{} 用户 ID: {}, 会话 ID: {}, 异常: {}",
                            errorDetails, userId, finalConversationId, (t != null ? t.getMessage() : "N/A"), t);
                    // 标记 Emitter 错误
                    emitter.completeWithError(t != null ? t : new RuntimeException("AI 流处理失败。" + errorDetails));
                }
            }; // SseListener 定义结束
            // --- Emitter 生命周期回调 (日志记录和资源清理) ---
            emitter.onCompletion(() -> log.info("/chatStreamChinese: SseEmitter 完成。用户 ID: {}, 会话 ID: {}", userId, finalConversationId));
            emitter.onTimeout(() -> {
                log.warn("/chatStreamChinese: SseEmitter 超时。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                emitter.completeWithError(new RuntimeException("请求处理超时"));
            });
            emitter.onError(e -> log.error("/chatStreamChinese: SseEmitter 发生错误。用户 ID: {}, 会话 ID: {}. 错误: {}", userId, finalConversationId, e.getMessage(), e));
            // --- 6. 发起 AI 流式请求 ---
            try {
                log.info("/chatStreamChinese: 调用 chatCompletionStream 开始。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                chatService.chatCompletionStream(chatCompletion, sseListener);
                log.info("/chatStreamChinese: chatCompletionStream 调用返回 (异步进行中)。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
            } catch (Exception e) {
                log.error("/chatStreamChinese: 调用 chatCompletionStream 启动时异常: {}。用户 ID: {}, 会话 ID: {}", e.getMessage(), userId, finalConversationId, e);
                emitter.completeWithError(new RuntimeException("启动 AI 请求失败: " + e.getMessage(), e));
            }
        }); // 异步线程结束
        // --- 7. 返回 Emitter ---
        log.info("/chatStreamChinese: 返回 SseEmitter 对象。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
        return emitter;
    }

    // --- 辅助方法 ---

    /**
     * 安全地发送 SSE 事件，处理潜在的 IOException。
     */
    private void sendSseEvent(SseEmitter emitter, String eventName, String data, Integer userId, String conversationId) {
        try {
            // 确保数据以 UTF-8 编码发送
            byte[] utf8Bytes = data.getBytes(StandardCharsets.UTF_8);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .id(UUID.randomUUID().toString()) // 为每个事件生成唯一 ID
                    .data(new String(utf8Bytes, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.warn("/chatStreamChinese: 发送 SSE 事件 '{}' 失败: {}。用户 ID: {}, 会话 ID: {}", eventName, e.getMessage(), userId, conversationId);
            // 通常发生在客户端断开连接时，可能需要中断 emitter
            // emitter.completeWithError(e); // 可以考虑在这里中断
        } catch (Exception e) {
            log.error("/chatStreamChinese: 发送 SSE 事件 '{}' 时发生意外错误: {}。用户 ID: {}, 会话 ID: {}", eventName, e.getMessage(), userId, conversationId, e);
            // emitter.completeWithError(e); // 可以考虑在这里中断
        }
    }

    /**
     * 使用错误消息和可选的异常完成 SseEmitter。
     */
    private void completeEmitterWithError(SseEmitter emitter, String message, @Nullable Throwable cause) {
        try {
            // 尝试向客户端发送最后一条错误消息
            emitter.send(SseEmitter.event().name("error").data(message).id(UUID.randomUUID().toString()));
        } catch (IOException e) {
            log.warn("/chatStreamChinese: 发送最终错误事件失败: {}", e.getMessage());
        }
        emitter.completeWithError(cause != null ? cause : new RuntimeException(message));
    }

    /**
     * 从 OkHttp Response 构建错误详情字符串。
     */
    private String buildErrorDetails(@Nullable Response response) {
        if (response == null) return "";
        String details = "响应码: " + response.code();
        try {
            if (response.body() != null) {
                String body = response.body().string(); // 注意：只能调用一次
                details += "，响应体: '" + (body.length() > 200 ? body.substring(0, 200) + "..." : body) + "'"; // 限制长度
            }
        } catch (IOException e) {
            details += " (读取响应体失败: " + e.getMessage() + ")";
        }
        return details;
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
            @RequestHeader("X-Token") String token,
            @RequestParam(required = false) String conversationId
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