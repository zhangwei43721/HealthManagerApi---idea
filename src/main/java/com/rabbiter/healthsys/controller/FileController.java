package com.rabbiter.healthsys.controller;

import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.config.JwtConfig;
import com.rabbiter.healthsys.entity.User;
import com.rabbiter.healthsys.service.IUserService;
import com.rabbiter.healthsys.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传控制器
 * 处理文件上传相关操作 (用户头像上传需要 Token 认证)
 * @author shuhaoran，yaoyang
 * @since 2025/4/26 16:04
 */
@RestController
@RequestMapping("/file")
@Slf4j
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数注入 final 字段
public class FileController {
    // 允许上传的图片扩展名
    private static final String ALLOWED_EXTENSIONS = ".jpg,.jpeg,.png,.gif";
    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    // 用户通过 web 访问的头像路径前缀

    // --- 依赖注入 ---
    private final IUserService userService;
    private final JwtConfig jwtConfig; // 新增：注入 JwtConfig 用于 Token 解析
    private final FileService fileService; // 新增：注入 FileService

    // --- Token 验证辅助方法 (仿照 AiSuggestionsSpecificController) ---
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
            log.info("Token 验证成功，用户ID: {}", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Token 解析失败。Token: {}", token, e);
            return null;
        }
    }

    // --- 文件处理辅助方法 (保持不变) ---

    /**
     * 获取文件扩展名（包含点），如果文件名无效或没有有效扩展名，则返回空字符串
     * 例如："image.jpg" -> ".jpg", "archive.tar.gz" -> ".gz", "myfile" -> "", ".gitignore" -> ""
     * 这里的逻辑是查找最后一个点，并确保它不是文件名的第一个或最后一个字符。
     * @param file MultipartFile 对象
     * @return 小写的文件扩展名（包含点），或空字符串
     */
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return ""; // 文件名为空或空字符串，无扩展名
        }
        int lastDotIndex = originalFilename.lastIndexOf(".");
        // 查找最后一个点，并确保它不是文件名的第一个或最后一个字符
        // 例如：".gitignore" -> lastDotIndex = 0, "myfile." -> lastDotIndex = length-1
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            return originalFilename.substring(lastDotIndex).toLowerCase();
        }
        return ""; // 没有找到有效的点，无扩展名
    }

    /**
     * 验证文件（通用逻辑）
     * 检查文件是否为空、大小是否超限、扩展名是否合法
     * @param file 要验证的文件
     * @return 如果验证失败，返回包含错误信息的 Unification 对象；如果验证成功，返回 null
     */
    private Unification<Map<String, Object>> validateFile(MultipartFile file) {
        // 检查文件是否为空参数
        if (file == null) {
            return Unification.fail(400, "参数文件为空");
        }
        // 检查文件内容是否为空 (isEmpty() 包含 size <= 0)
        if (file.isEmpty()) {
            return Unification.fail(400, "文件内容为空");
        }
        // 判断文件大小不能大于最大值
        if (file.getSize() > MAX_FILE_SIZE) {
            // 注意：错误信息应与 MAX_FILE_SIZE 保持一致
            return Unification.fail(400, "文件不能大于 " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        // 获取文件扩展名 (使用独立的方法)
        String fileExtension = getFileExtension(file);
        // 检查文件扩展名是否合法：如果获取到的扩展名非空，则必须在允许的列表中
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            log.warn("文件扩展名不合法: {}", fileExtension);
            return Unification.fail(400, "只允许上传jpg、jpeg、png、gif格式的图片");
        }
        // 验证成功
        return null;
    }


    // --- API 端点 ---

    /**
     * 新增：上传文件到 Cloudflare R2 (通用，无需认证，具体认证逻辑可在 FileService 实现)
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Unification<Map<String, Object>> uploadToR2(@RequestParam("file") MultipartFile file) {
        // 新增：调用通用文件验证方法 (大小、扩展名)
        Unification<Map<String, Object>> validationResult = validateFile(file);
        if (validationResult != null) {
            return validationResult; // 如果验证失败，直接返回失败结果
        }

        // 验证通过后，再调用 FileService 的 upload 方法
        return fileService.upload(file);
    }

    /**
     * 上传通用头像 (无需认证) -> 修改为上传到 R2
     * 调用通用验证和保存逻辑
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/avatar/upload")
    public Unification<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 调用通用文件验证方法 (大小、扩展名)
        Unification<Map<String, Object>> validationResult = validateFile(file);
        if (validationResult != null) {
            return validationResult; // 如果验证失败，直接返回失败结果
        }

        // 2. 直接调用 FileService 上传到 R2
        return fileService.upload(file);
    }

    /**
     * 用户专用的头像上传方法 (需要 Token 认证) -> 修改为上传到 R2
     * 调用通用验证和保存逻辑，并更新用户信息
     * @param file 头像文件
     * @param token 用户认证 Token (从 Header X-Token 获取)
     * @return 上传结果
     */
    @PostMapping("/user/avatar/upload")
    public Unification<Map<String, Object>> uploadUserAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Token") String token) { // 修改：接收 Token

        // 1. 验证 Token 并获取用户 ID
        User user = validateTokenAndGetUser(token);
        if (user == null) {
            // Token 无效或解析失败，返回 401 未授权
            return Unification.fail(401, "认证失败，请检查Token或重新登录。");
        }
        Integer userId = user.getId(); // 从 Token 中获取用户 ID

        // 2. 调用通用文件验证方法
        Unification<Map<String, Object>> validationResult = validateFile(file);
        if (validationResult != null) {
            return validationResult; // 如果验证失败，直接返回失败结果
        }

        // 3. 调用 FileService 上传文件到 R2
        Unification<Map<String, Object>> r2UploadResult = fileService.upload(file);

        // 4. 检查 R2 上传是否成功
        if (r2UploadResult.getCode() != 20000) {
            // R2 上传失败，直接返回 R2 的失败结果
            log.error("用户头像上传到 R2 失败, userId: {}. R2响应: {}", userId, r2UploadResult.getMessage());
            return r2UploadResult;
        }

        // 5. R2 上传成功，获取 R2 返回的 URL
        String r2FileUrl = (String) r2UploadResult.getData().get("url");
        if (r2FileUrl == null || r2FileUrl.isEmpty()) {
            log.error("R2 上传成功但未能获取文件 URL, userId: {}, R2响应数据: {}", userId, r2UploadResult.getData());
            return Unification.fail(500, "文件上传成功，但处理URL时出错");
        }
        log.info("用户头像成功上传到 R2, userId: {}, url: {}", userId, r2FileUrl);

        // 6. 更新用户头像信息到数据库 (使用 R2 的 URL)
        boolean updateResult = userService.updateUserAvatar(userId, r2FileUrl);
        if (!updateResult) {
            log.error("更新用户头像信息失败 (R2 URL), userId: {}", userId);
            // 注意：这里可以考虑是否需要删除 R2 上的文件，目前不处理
            return Unification.fail(500, "更新用户头像信息失败");
        }
        log.info("成功更新用户头像数据库记录, userId: {}", userId);

        // 7. 返回 R2 上传成功的原始结果 (包含 R2 URL)
        return r2UploadResult;
    }
}