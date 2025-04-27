package com.rabbiter.healthsys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.User;
import com.rabbiter.healthsys.entity.AiSuggestionsSpecific;
import com.rabbiter.healthsys.service.IAiSuggestionsSpecificService;
import lombok.RequiredArgsConstructor; // 用于构造器注入
import lombok.extern.slf4j.Slf4j; // 用于日志
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI健康建议 前端控制器 (带Token认证)
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
                return null; // 无效的用户信息
            }
            return user;
        } catch (Exception e) {
            log.error("Token 解析失败。Token: {}", token, e);
            return null; // 解析失败或Token无效/过期
        }
    }


    /**
     * 新增 AI 建议记录 (需要认证)
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @param suggestion AI 建议对象 (RequestBody)
     * @return 操作结果
     */
    @PostMapping("/add")
    public Unification<?> addSuggestion(
            @RequestHeader("X-Token") String token,
            @RequestBody AiSuggestionsSpecific suggestion) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        // 将从 Token 获取的用户 ID 设置到建议对象中
        suggestion.setUserId(user.getId());

        log.info("接收到用户 {} 新增AI建议请求: {}", user.getId(), suggestion);
        boolean success = aiSuggestionsSpecificService.save(suggestion);
        if (success) {
            log.info("AI建议添加成功, ID: {}", suggestion.getId());
            return Unification.success("建议添加成功");
        } else {
            log.error("AI建议添加失败");
            return Unification.fail("建议添加失败");
        }
    }

    /**
     * 根据 ID 获取建议记录 (需要认证)
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @param id 建议记录 ID (从 PathVariable 获取)
     * @return 建议记录详情
     */
    @GetMapping("/getSuggestionById/{id}")
    public Unification<AiSuggestionsSpecific> getSuggestionById(
            @RequestHeader("X-Token") String token,
            @PathVariable Integer id) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        // 注意：这里只验证了用户已登录，并未验证该建议是否属于该用户
        // 如果需要严格的权限控制（只能看自己的），需要额外查询并比较 suggestion.getUserId() 和 user.getId()

        log.info("用户 {} 根据ID获取AI建议, ID: {}", user.getId(), id);
        AiSuggestionsSpecific suggestion = aiSuggestionsSpecificService.getById(id);
        if (suggestion != null) {
             if (!suggestion.getUserId().equals(user.getId())) {
                 log.warn("权限不足：用户 {} 尝试访问不属于自己的建议记录 ID: {}", user.getId(), id);
                 return Unification.fail("无权访问该建议记录");
             }
            return Unification.success(suggestion);
        } else {
            log.warn("未找到指定的AI建议记录, ID: {}", id);
            return Unification.fail("未找到指定的建议记录");
        }
    }

    /**
     * 获取当前认证用户的最新建议记录 (需要认证)
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 最新的建议记录
     */
    @GetMapping("/latest") // 路径不再需要 userId，因为从 token 获取
    public Unification<AiSuggestionsSpecific> getLatestSuggestion(
            @RequestHeader("X-Token") String token) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        Integer userId = user.getId(); // 从认证信息中获取用户ID

        log.info("获取用户 {} 的最新AI建议", userId);
        AiSuggestionsSpecific latestSuggestion = aiSuggestionsSpecificService.getLatestSuggestionByUserId(userId);
        if (latestSuggestion != null) {
            return Unification.success(latestSuggestion);
        } else {
            log.warn("未找到用户 {} 的AI建议记录", userId);
            return Unification.fail("未找到该用户的建议记录");
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
     * 获取当前认证用户的所有建议列表 (按时间降序, 需要认证) - 如果数据量不大可以使用
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 建议列表
     */
    @GetMapping("/list") // 路径不再需要 userId
    public Unification<List<AiSuggestionsSpecific>> getSuggestionsByUserList(
            @RequestHeader("X-Token") String token) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        Integer userId = user.getId(); // 从认证信息中获取用户ID

        log.info("查询用户 {} 的所有AI建议列表", userId);
        LambdaQueryWrapper<AiSuggestionsSpecific> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiSuggestionsSpecific::getUserId, userId) // 查询条件使用认证用户的 ID
                .orderByDesc(AiSuggestionsSpecific::getGeneratedAt);

        List<AiSuggestionsSpecific> list = aiSuggestionsSpecificService.list(wrapper);
        return Unification.success(list);
    }


    /**
     * 根据 ID 删除建议记录 (需要认证)
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @param id 建议记录 ID (从 PathVariable 获取)
     * @return 操作结果
     */
    @DeleteMapping("/deleteSuggestionById/{id}")
    public Unification<?> deleteSuggestionById(
            @RequestHeader("X-Token") String token,
            @PathVariable Integer id) {

        User user = validateTokenAndGetUser(token);
        if (user == null) {
            return Unification.fail("认证失败，请检查Token或重新登录。");
        }
        // 注意：这里同样只验证了用户已登录，并未验证该建议是否属于该用户
        // 如果需要严格控制，应先查询，确认所有权再删除

        log.info("用户 {} 根据ID删除AI建议, ID: {}", user.getId(), id);

         AiSuggestionsSpecific suggestion = aiSuggestionsSpecificService.getById(id);
         if (suggestion == null) {
             return Unification.fail("删除失败：记录不存在");
         }
         if (!suggestion.getUserId().equals(user.getId())) {
             log.warn("权限不足：用户 {} 尝试删除不属于自己的建议记录 ID: {}", user.getId(), id);
             return Unification.fail("无权删除该建议记录");
         }

        boolean success = aiSuggestionsSpecificService.removeById(id); // 执行删除
        if (success) {
            log.info("AI建议删除成功, ID: {}", id);
            return Unification.success("删除成功");
        } else {
            log.warn("AI建议删除失败或记录不存在, ID: {}", id);
            return Unification.fail("删除失败或记录不存在");
        }
    }
}