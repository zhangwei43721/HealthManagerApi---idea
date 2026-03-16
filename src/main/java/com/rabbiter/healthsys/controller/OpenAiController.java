package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rabbiter.healthsys.entity.ChatHistory;
import com.rabbiter.healthsys.service.IChatHistoryService;
import com.rabbiter.healthsys.common.UserTokenResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.lang.Nullable;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * AI聊天接口 - 使用 Spring AI
 *
 * @author Skyforever
 * @since 2025-05-01
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OpenAiController {

    private static final String DETECTION_RESULT_PROXY_PATH = "/image-detection/result/";
    private final Map<String, ActiveChatStream> activeChatStreams = new ConcurrentHashMap<>();

    private final OpenAiChatModel chatModel;
    private final IChatHistoryService chatHistoryService;
    private final UserTokenResolver userTokenResolver;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.model.default}")
    private String defaultChatModel;

    @Value("${ai.model.chinese}")
    private String chineseChatModel;

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
     * @param token 用户认证 token (在 URL Query 参数 "token" 中)
     * @param file 可选的图片文件 (在 form-data 的 'file' 字段中)
     * @return SSE Emitter 实时向客户端发送 AI 回复。
     */
    @PostMapping(value = "/chatStream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter getChatMessageStream(
            @RequestHeader("X-Token") String token,
            @RequestPart("message") String messageParam,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart(value = "conversationId", required = false) String conversationIdParam,
            HttpServletRequest request
    ) {
        return streamChat(token, messageParam, file, conversationIdParam, request, true, true, null, defaultChatModel);
    }

    public SseEmitter streamChat(String token,
                                 String messageParam,
                                 @Nullable MultipartFile file,
                                 @Nullable String conversationIdParam,
                                 @Nullable HttpServletRequest request,
                                 boolean loadHistory,
                                 boolean persistHistory,
                                 @Nullable Consumer<String> onCompleteCallback,
                                 String model) {
        SseEmitter emitter = new SseEmitter(3600000L);

        String message = messageParam;
        String conversationIdStr = conversationIdParam;

        final Integer userId;
        try {
            userId = parseUserId(token);
            if (userId == null) {
                log.error("/chatStream: 无效的用户信息。Token: {}", token);
                completeEmitterWithError(emitter, "用户信息无效，请核对。", null);
                return emitter;
            }
            log.info("/chatStream: 用户认证成功。用户 ID: {}", userId);
        } catch (Exception e) {
            log.error("/chatStream: Token 解析失败。Token: {}", token, e);
            completeEmitterWithError(emitter, "认证失败，请重新登录。", e);
            return emitter;
        }

        String currentConversationId = conversationIdStr;
        if (currentConversationId == null || currentConversationId.trim().isEmpty() || "new".equalsIgnoreCase(currentConversationId.trim())) {
            currentConversationId = UUID.randomUUID().toString();
            log.info("/chatStream: 为用户 {} 生成了新的会话 ID: {}", userId, currentConversationId);
            sendSseEvent(emitter, "conversationId", currentConversationId, userId, currentConversationId);
        } else {
            log.info("/chatStream: 用户 {} 继续使用现有会话 ID: {}", userId, currentConversationId);
        }

        final String finalConversationId = currentConversationId;
        final String streamKey = buildStreamKey(userId, finalConversationId);
        final String backendBaseUrl = request != null
                ? ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(request.getContextPath())
                .replaceQuery(null)
                .build()
                .toUriString()
                : null;

        cancelActiveStream(streamKey, "同一会话有新的流式请求启动");
        final ActiveChatStream activeStream = new ActiveChatStream(emitter);
        activeChatStreams.put(streamKey, activeStream);

        Schedulers.boundedElastic().schedule(() -> {
            if (activeStream.isCancelled()) {
                cleanupActiveStream(streamKey, activeStream);
                return;
            }

            try {
                String processedMessage = file != null && request != null
                        ? processUploadedImage(file, message, userId, finalConversationId, backendBaseUrl, emitter)
                        : message;

                List<Message> messages = buildStreamMessages(userId, finalConversationId, processedMessage, loadHistory, persistHistory);
                Prompt prompt = new Prompt(messages);

                StringBuilder fullResponse = new StringBuilder();

                Flux<ChatResponse> responseFlux = chatModel.stream(prompt);

                responseFlux
                    .doOnNext(chatResponse -> {
                        if (activeStream.isCancelled()) {
                            return;
                        }
                        String content = chatResponse.getResult().getOutput().getText();
                        if (content != null && !content.isEmpty()) {
                            fullResponse.append(content);
                            try {
                                byte[] utf8Bytes = content.getBytes(StandardCharsets.UTF_8);
                                emitter.send(utf8Bytes);
                            } catch (IOException e) {
                                log.error("/chatStream: 发送 SSE 数据失败: {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }
                    })
                    .doOnError(error -> {
                        log.error("/chatStream: AI 流处理失败。用户 ID: {}, 会话 ID: {}", userId, finalConversationId, error);
                        cleanupActiveStream(streamKey, activeStream);
                        emitter.completeWithError(error);
                    })
                    .doOnComplete(() -> {
                        log.info("/chatStream: AI 流连接已关闭。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
                        String responseText = fullResponse.toString();
                        handleAssistantResponse(userId, finalConversationId, persistHistory, onCompleteCallback, responseText);
                        emitter.complete();
                        cleanupActiveStream(streamKey, activeStream);
                    })
                    .subscribe();

            } catch (Exception e) {
                log.error("/chatStream: 启动 AI 请求失败: {}", e.getMessage(), e);
                cleanupActiveStream(streamKey, activeStream);
                emitter.completeWithError(new RuntimeException("启动 AI 请求失败: " + e.getMessage(), e));
            }
        });

        registerChatEmitterLifecycle(emitter, userId, finalConversationId, streamKey, activeStream);
        log.info("/chatStream: 返回 SseEmitter 对象。用户 ID: {}, 会话 ID: {}", userId, finalConversationId);
        return emitter;
    }

    public SseEmitter streamReasoningChat(String token,
                                          String messageParam,
                                          @Nullable String conversationIdParam,
                                          @Nullable Consumer<String> onCompleteCallback) {
        return streamChat(token, messageParam, null, conversationIdParam, null, false, false, onCompleteCallback, chineseChatModel);
    }

    @GetMapping("/image-detection/result/{fileName:.+}")
    public ResponseEntity<byte[]> getDetectionResultImage(@PathVariable String fileName) {
        String targetUrl = buildDetectionResultFetchUrl(fileName);
        try {
            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(targetUrl, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = responseEntity.getHeaders().getContentType();
            headers.setContentType(contentType != null ? contentType : MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(responseEntity.getBody(), headers, responseEntity.getStatusCode());
        } catch (HttpStatusCodeException e) {
            log.error("/image-detection/result: 获取结果图失败，文件: {}, 状态码: {}, 响应: {}", fileName, e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/chatStream/cancel")
    public String cancelChatStream(
            @RequestHeader("X-Token") String token,
            @RequestParam String conversationId
    ) {
        Integer userId;
        try {
            userId = parseUserId(token);
            if (userId == null) {
                return "取消失败：用户信息无效。";
            }
        } catch (Exception e) {
            log.error("/chatStream/cancel: Token 解析失败。Token: {}", token, e);
            return "取消失败：认证信息无效或已过期。";
        }

        String streamKey = buildStreamKey(userId, conversationId);
        ActiveChatStream activeStream = activeChatStreams.remove(streamKey);
        if (activeStream == null) {
            return "当前会话没有正在进行的流式响应。";
        }

        activeStream.cancel("用户主动停止生成");
        log.info("/chatStream/cancel: 已取消流式响应。用户 ID: {}, 会话 ID: {}", userId, conversationId);
        return "已停止生成。";
    }

    // --- 辅助方法 ---

    @PreDestroy
    public void shutdown() {
        activeChatStreams.values().forEach(stream -> stream.cancel("服务关闭"));
        activeChatStreams.clear();
    }

    private String buildStreamKey(Integer userId, String conversationId) {
        return userId + ":" + conversationId;
    }

    private void cancelActiveStream(String streamKey, String reason) {
        ActiveChatStream existingStream = activeChatStreams.remove(streamKey);
        if (existingStream != null) {
            existingStream.cancel(reason);
        }
    }

    private void cleanupActiveStream(String streamKey, ActiveChatStream activeStream) {
        activeChatStreams.remove(streamKey, activeStream);
    }

    private void registerChatEmitterLifecycle(SseEmitter emitter,
                                              Integer userId,
                                              String conversationId,
                                              String streamKey,
                                              ActiveChatStream activeStream) {
        emitter.onCompletion(() -> {
            cleanupActiveStream(streamKey, activeStream);
            log.info("/chatStreamLifecycle: SseEmitter 完成。用户 ID: {}, 会话 ID: {}", userId, conversationId);
        });
        emitter.onTimeout(() -> {
            log.warn("/chatStreamLifecycle: SseEmitter 超时。用户 ID: {}, 会话 ID: {}", userId, conversationId);
            activeStream.cancel("请求处理超时");
            cleanupActiveStream(streamKey, activeStream);
            emitter.completeWithError(new RuntimeException("请求处理超时"));
        });
        emitter.onError(e -> {
            log.error("/chatStreamLifecycle: SseEmitter 发生错误。用户 ID: {}, 会话 ID: {}. 错误: {}", userId, conversationId, e.getMessage(), e);
            activeStream.cancel("SseEmitter 发生错误");
            cleanupActiveStream(streamKey, activeStream);
        });
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, String data, Integer userId, String conversationId) {
        try {
            byte[] utf8Bytes = data.getBytes(StandardCharsets.UTF_8);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .id(UUID.randomUUID().toString())
                    .data(new String(utf8Bytes, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            log.warn("/chatStream: 发送 SSE 事件 '{}' 失败: {}。用户 ID: {}, 会话 ID: {}", eventName, e.getMessage(), userId, conversationId);
        } catch (Exception e) {
            log.error("/chatStream: 发送 SSE 事件 '{}' 时发生意外错误: {}。用户 ID: {}, 会话 ID: {}", eventName, e.getMessage(), userId, conversationId, e);
        }
    }

    private void completeEmitterWithError(SseEmitter emitter, String message, @Nullable Throwable cause) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message).id(UUID.randomUUID().toString()));
        } catch (IOException e) {
            log.warn("/chatStream: 发送最终错误事件失败: {}", e.getMessage());
        }
        emitter.completeWithError(cause != null ? cause : new RuntimeException(message));
    }

    private String processUploadedImage(@Nullable MultipartFile file,
                                        String originalMessage,
                                        Integer userId,
                                        String conversationId,
                                        @Nullable String backendBaseUrl,
                                        SseEmitter emitter) {
        if (file == null || file.isEmpty()) {
            log.info("/chatStream: 用户 {} 在会话 {} 中未上传图片，直接处理消息。", userId, conversationId);
            return originalMessage;
        }

        log.info("/chatStream: 用户 {} 在会话 {} 中上传了图片 {} ({} bytes)，将调用对象检测 API。",
                userId, conversationId, file.getOriginalFilename(), file.getSize());
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    imageDetectionApiUrl,
                    buildImageDetectionRequest(file),
                    String.class
            );

            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                log.error("/chatStream: 调用对象检测 API 失败，状态码: {}", responseEntity.getStatusCode());
                return String.format(imageDetectionApiErrorPrompt, responseEntity.getStatusCode().value(), originalMessage);
            }

            String responseBody = responseEntity.getBody();
            log.info("/chatStream: 对象检测 API 成功响应: {}", responseBody);
            Map<String, Object> detectionResponse = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            if (!"success".equals(detectionResponse.get("status"))) {
                String errorMsg = extractDetectionErrorMessage(detectionResponse);
                log.error("/chatStream: 对象检测 API 返回错误状态: {}", errorMsg);
                return String.format(imageDetectionErrorPrompt, errorMsg, originalMessage);
            }

            String proxiedResultImageUrl = backendBaseUrl != null
                    ? buildDetectionResultProxyUrl(backendBaseUrl, (String) detectionResponse.get("result_image_url"))
                    : null;
            String detectionInfo = buildDetectionInfo((List<Map<String, Object>>) detectionResponse.get("detections"));
            if (proxiedResultImageUrl != null) {
                sendDetectionResultImage(emitter, proxiedResultImageUrl);
            }
            String resultImageInfo = proxiedResultImageUrl != null ? "处理后的图片地址：" + proxiedResultImageUrl : "";
            return String.format(imageDetectionSuccessPrompt, detectionInfo, resultImageInfo, originalMessage);
        } catch (HttpStatusCodeException e) {
            String errorMsg = extractHttpErrorMessage(e);
            log.error("/chatStream: 调用对象检测 API 失败，状态码: {}, 响应: {}", e.getStatusCode().value(), errorMsg, e);
            return String.format(imageDetectionErrorPrompt, errorMsg, originalMessage);
        } catch (Exception e) {
            log.error("/chatStream: 调用对象检测 API 或处理其响应时发生异常", e);
            return String.format(imageDetectionExceptionPrompt, e.getMessage(), originalMessage);
        }
    }

    private HttpEntity<MultiValueMap<String, Object>> buildImageDetectionRequest(MultipartFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        return new HttpEntity<>(body, headers);
    }

    private String buildDetectionInfo(@Nullable List<Map<String, Object>> detections) {
        if (detections == null || detections.isEmpty()) {
            log.info("/chatStream: 图片中未检测到任何对象。");
            return "图片中未检测到任何对象。";
        }

        StringBuilder sb = new StringBuilder("图片中检测到以下对象：");
        for (Map<String, Object> det : detections) {
            sb.append(String.format("%s (置信度: %.2f), ", det.get("class"), det.get("confidence")));
        }
        if (sb.length() > "图片中检测到以下对象：".length()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("。");
        String detectionInfo = sb.toString();
        log.info("/chatStream: 检测结果描述: {}", detectionInfo);
        return detectionInfo;
    }

    private void sendDetectionResultImage(SseEmitter emitter, String proxiedResultImageUrl) {
        try {
            emitter.send(SseEmitter.event().name("detectionResultImage").data(proxiedResultImageUrl));
            log.info("/chatStream: 已将检测结果图片 URL 发送给客户端: {}", proxiedResultImageUrl);
        } catch (IOException e) {
            log.error("/chatStream: 发送检测结果图片 URL 至客户端失败", e);
        }
    }

    @Nullable
    private Integer parseUserId(String token) {
        return userTokenResolver.parseUserId(token);
    }

    private List<Message> buildStreamMessages(Integer userId,
                                              String conversationId,
                                              String processedMessage,
                                              boolean loadHistory,
                                              boolean persistHistory) {
        List<Message> messages = new ArrayList<>();

        if (loadHistory) {
            List<ChatHistory> historyList = chatHistoryService.getChatHistoryByUserIdAndConversationId(userId, conversationId);
            for (ChatHistory history : historyList) {
                if ("user".equals(history.getRole())) {
                    messages.add(new UserMessage(history.getContent()));
                } else if ("assistant".equals(history.getRole())) {
                    messages.add(new AssistantMessage(history.getContent()));
                }
            }
            log.info("/chatStream: 为用户 {} 的会话 {} 加载了 {} 条历史消息。", userId, conversationId, historyList.size());
        }

        UserMessage userMessage = new UserMessage(processedMessage);
        messages.add(userMessage);
        log.info("/chatStream: 本次发送给 AI 的消息总数: {}。处理后的用户消息: {}", messages.size(), processedMessage);

        if (persistHistory) {
            chatHistoryService.save(ChatHistory.createUserMessage(userId, conversationId, processedMessage));
            log.info("/chatStream: 用户消息已保存至数据库。用户 ID: {}, 会话 ID: {}", userId, conversationId);
        }
        return messages;
    }

    private void handleAssistantResponse(Integer userId,
                                         String conversationId,
                                         boolean persistHistory,
                                         @Nullable Consumer<String> onCompleteCallback,
                                         String rawOutput) {
        String assistantResponse = normalizeAssistantResponse(rawOutput);
        if (assistantResponse.isEmpty()) {
            log.warn("/chatStream: AI 回复为空。用户 ID: {}, 会话 ID: {}", userId, conversationId);
            return;
        }

        if (persistHistory) {
            chatHistoryService.save(ChatHistory.createAssistantMessage(userId, conversationId, assistantResponse));
            log.info("/chatStream: AI 回复已保存至数据库。用户 ID: {}, 会话 ID: {}, 回复长度: {}",
                    userId, conversationId, assistantResponse.length());
        }

        if (onCompleteCallback != null) {
            try {
                onCompleteCallback.accept(assistantResponse);
                log.info("/chatStream: onCompleteCallback 执行完成。用户 ID: {}, 会话 ID: {}", userId, conversationId);
            } catch (Exception e) {
                log.error("/chatStream: onCompleteCallback 执行异常。用户 ID: {}, 会话 ID: {}", userId, conversationId, e);
            }
        }
    }

    private String normalizeAssistantResponse(String rawOutput) {
        try {
            byte[] utf8Bytes = rawOutput.getBytes(StandardCharsets.UTF_8);
            return new String(utf8Bytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.error("/chatStream: 处理 AI 回复 UTF-8 编码时出错: {}", e.getMessage(), e);
            return rawOutput.trim();
        }
    }

    private String extractDetectionErrorMessage(@Nullable Map<String, Object> detectionResponse) {
        if (detectionResponse == null || detectionResponse.isEmpty()) {
            return "检测失败，未返回可解析内容。";
        }
        Object message = detectionResponse.get("message");
        if (message == null) {
            message = detectionResponse.get("detail");
        }
        return message != null ? String.valueOf(message) : "检测失败，未提供具体原因。";
    }

    private String extractHttpErrorMessage(HttpStatusCodeException e) {
        String responseBody = e.getResponseBodyAsString();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return String.format("调用对象检测服务失败 (状态码: %d)", e.getStatusCode().value());
        }
        try {
            Map<String, Object> errorBody = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            return extractDetectionErrorMessage(errorBody);
        } catch (Exception parseException) {
            log.warn("/chatStream: 解析对象检测 API 错误响应失败，原始响应体: {}", responseBody, parseException);
            return responseBody;
        }
    }

    @Nullable
    private String buildDetectionResultProxyUrl(String backendBaseUrl, @Nullable String resultImageUrl) {
        String fileName = extractDetectionResultFileName(resultImageUrl);
        if (fileName == null) {
            return null;
        }
        String normalizedBaseUrl = trimTrailingSlash(backendBaseUrl);
        return normalizedBaseUrl + DETECTION_RESULT_PROXY_PATH + fileName;
    }

    private String buildDetectionResultFetchUrl(String fileName) {
        String baseUrl = getDetectionServiceBaseUrl();
        return trimTrailingSlash(baseUrl) + "/get_result_image/" + fileName;
    }

    private String getDetectionServiceBaseUrl() {
        if (imageDetectionBaseUrl != null && !imageDetectionBaseUrl.trim().isEmpty()) {
            return imageDetectionBaseUrl.trim();
        }
        try {
            URI detectionApiUri = URI.create(imageDetectionApiUrl);
            StringBuilder baseUrl = new StringBuilder()
                    .append(detectionApiUri.getScheme())
                    .append("://")
                    .append(detectionApiUri.getHost());
            if (detectionApiUri.getPort() > -1) {
                baseUrl.append(":").append(detectionApiUri.getPort());
            }
            return baseUrl.toString();
        } catch (Exception e) {
            log.warn("解析 ai.image-detection.api-url 失败，将回退使用原始配置。api-url={}", imageDetectionApiUrl, e);
            return imageDetectionApiUrl;
        }
    }

    @Nullable
    private String extractDetectionResultFileName(@Nullable String resultImageUrl) {
        if (resultImageUrl == null || resultImageUrl.trim().isEmpty()) {
            return null;
        }
        int lastSlashIndex = resultImageUrl.lastIndexOf('/');
        return lastSlashIndex >= 0 ? resultImageUrl.substring(lastSlashIndex + 1) : resultImageUrl;
    }

    private String trimTrailingSlash(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static class ActiveChatStream {
        private final SseEmitter emitter;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private ActiveChatStream(SseEmitter emitter) {
            this.emitter = emitter;
        }

        private boolean isCancelled() {
            return cancelled.get();
        }

        private void cancel(String reason) {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取用户所有对话历史记录。通过请求头 "X-Token" 中的 token 识别用户。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
     * @return 用户所有 ChatHistory 列表。
     */
    @GetMapping("/viewHistory")
    public List<ChatHistory> viewHistory(@RequestHeader("X-Token") String token) {
        Integer userId;
        try {
            userId = parseUserId(token);
            if (userId == null) {
                log.error("viewHistory: Token解析成功，但获取用户ID失败。Token: {}", token);
                return new ArrayList<>();
            }
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
     * 删除用户部分或全部对话历史记录。通过请求头 "X-Token" 中的 token 识别用户。
     * @param token 用户认证 token (在 Request Header "X-Token" 中)
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
            userId = parseUserId(token);
            if (userId == null) {
                log.error("resetHistory: Token解析成功，但获取用户ID失败。Token: {}", token);
                return "删除失败：无法识别有效的用户信息。";
            }
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
