package com.rabbiter.healthsys.controller;

import io.github.lnyocly.ai4j.listener.SseListener;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factor.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;import java.nio.charset.StandardCharsets;
import java.util.Arrays;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
//联网ai服务端
@RestController
public class OpenAiController {

    // 注入Ai服务
    @Autowired
    private AiService aiService;
    @GetMapping("/chatStream")
    public SseEmitter getChatMessageStream(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter();
        // 获取DEEPSEEK的聊天服务
        IChatService chatService = aiService.getChatService(PlatformType.DEEPSEEK);
        // 创建请求参数
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("deepseek-chat")
                .message(ChatMessage.withUser(question))
                .build();
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                SseListener sseListener = new SseListener() {
                    @Override
                    protected void send() {
                        try {
                            String currentData = this.getCurrData();
                            if (currentData != null && !currentData.isEmpty()) {
                                System.out.println("[Server] Original Data to send: [" + currentData + "]");
                                byte[] utf8Bytes = currentData.getBytes(StandardCharsets.UTF_8);
                                System.out.println("[Server] Raw Data UTF-8 Bytes being sent: " + Arrays.toString(utf8Bytes));
                                emitter.send(utf8Bytes);

                                System.out.println("[Server] Sent raw bytes successfully for: [" + currentData + "]");

                            } else {
                                System.out.println("[Server] Received empty or null data, skipping send.");
                            }
                        } catch (IOException e) {
                            System.err.println("[Server] IOException during send: " + e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.err.println("[Server] Unexpected error during send: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                };
                emitter.onCompletion(() -> {
                    System.out.println("完成");
                    sseListener.getEventSource().cancel();

                });
                // 发送流式数据
                chatService.chatCompletionStream(chatCompletion, sseListener);
                // 完成后关闭连接
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}


