package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.User;
import com.rabbiter.healthsys.entity.AiSuggestionsSpecific;
import com.rabbiter.healthsys.service.IAiSuggestionsSpecificService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 查询AI健康建议 前端控制器
 * </p>
 *
 * @author Skyforever
 * @since 2025-04-27
 */
@RestController
@RequestMapping("/ai-suggestions-specific") // 定义基础请求路径
@RequiredArgsConstructor // Lombok: 自动生成包含 final 字段的构造函数，用于注入
@Slf4j // Lombok: 自动注入日志对象 log
public class AiSuggestionsSpecificController {

    private final IAiSuggestionsSpecificService aiSuggestionsSpecificService;
    private final JwtConfig jwtConfig; // 注入 JWT 配置

    /**
     * 私有辅助方法：验证 Token 并获取用户信息
     * @param token 从请求头 X-Token 获取的令牌
     * @return 验证通过则返回 User 对象，否则返回 null
     */
    private User validateTokenAndGetUser(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("请求头 X-Token 为空或缺失。");
            return null;
        }
        try {
            User user = jwtConfig.parseToken(token, User.class);
            if (user == null || user.getId() == null) {
                log.error("Token解析成功，但未能获取有效的用户ID。Token: {}", token);
                return null;
            }
            return user;
        } catch (Exception e) {
            log.error("Token 解析失败。Token: {}", token, e);
            return null;
        }
    }

    /**
     * 分页查询当前认证用户的建议列表 (按时间降序, 需要认证)
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @param pageNo 当前页码
     * @param pageSize 每页数量
     * @return 分页结果 (包含 total 和 rows)
     */
    @GetMapping("/page")
    public Unification<Map<String, Object>> getSuggestionsByUserPage(
            @RequestHeader("X-Token") String token,
            @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        Integer userId = user.getId(); // 从认证信息中获取用户ID

        log.info("分页查询用户 {} 的AI建议列表, pageNo={}, pageSize={}", userId, pageNo, pageSize);
        Page<AiSuggestionsSpecific> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<AiSuggestionsSpecific> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSuggestionsSpecific::getUserId, userId) // 查询条件使用认证用户的 ID
                .orderByDesc(AiSuggestionsSpecific::getGeneratedAt);

        aiSuggestionsSpecificService.page(page, wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("rows", page.getRecords());

        return Unification.success(data);
    }

    /**
     * 获取当前认证用户的历史健康建议
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 历史健康建议
     */
    @GetMapping("/historical")
    public Unification<Map<String,Object>> getHistoricalSuggestion(@RequestHeader("X-Token") String token) {
        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        AiSuggestionsSpecific s = aiSuggestionsSpecificService.getLatestSuggestionByUserId(user.getId());
        if (s == null) {
            return Unification.fail("未找到历史健康建议");
        }
        Map<String,Object> res = new HashMap<>();
        res.put("suggestion", s.getSuggestionHistoricalHealth());
        res.put("generatedAt", s.getGeneratedAt());
        return Unification.success(res);
    }

    /**
     * 获取当前认证用户的当前健康建议
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 当前健康建议
     */
    @GetMapping("/current")
    public Unification<Map<String,Object>> getCurrentSuggestion(@RequestHeader("X-Token") String token) {
        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        AiSuggestionsSpecific s = aiSuggestionsSpecificService.getLatestSuggestionByUserId(user.getId());
        if (s == null) {
            return Unification.fail("未找到当前健康建议");
        }
        Map<String,Object> res = new HashMap<>();
        res.put("suggestion", s.getSuggestionCurrentHealth());
        res.put("generatedAt", s.getGeneratedAt());
        return Unification.success(res);
    }

    /**
     * 获取当前认证用户的运动信息建议
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 运动信息建议
     */
    @GetMapping("/sport")
    public Unification<Map<String,Object>> getSportSuggestion(@RequestHeader("X-Token") String token) {
        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        AiSuggestionsSpecific s = aiSuggestionsSpecificService.getLatestSuggestionByUserId(user.getId());
        if (s == null) {
            return Unification.fail("未找到运动信息建议");
        }
        Map<String,Object> res = new HashMap<>();
        res.put("suggestion", s.getSuggestionSportInfo());
        res.put("generatedAt", s.getGeneratedAt());
        return Unification.success(res);
    }

    /**
     * AI 流式获取运动信息建议
     * @param token 用户认证 Token
     * @param conversationId 会话 ID
     * @return SSE 实时流
     */
    @GetMapping("/sportStream")
    public SseEmitter getSportSuggestionStream(
            @RequestHeader("X-Token") String token,
            @RequestParam(required = false) String conversationId) {
        SseEmitter emitter = new SseEmitter(3600000L);
        User user = validateTokenAndGetUser(token);
        if (user == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("认证失败，请检查Token或重新登录。"));
            } catch (IOException e) {
                log.error("sportStream发送错误事件失败", e);
            }
            emitter.complete();
            return emitter;
        }
        return aiSuggestionsSpecificService.analyzeSportSuggestion(token, conversationId);
    }
}