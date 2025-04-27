package com.rabbiter.healthsys.controller;

import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 * 处理文件上传相关操作
 * @author shuhaoran
 * @since 2025/4/26 16:04
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    // 允许上传的图片扩展名
    // 注意：此处的校验逻辑是：如果文件有扩展名，则必须是这些扩展名之一；没有扩展名则允许上传。
    private static final String ALLOWED_EXTENSIONS = ".jpg,.jpeg,.png,.gif";
    // 最大文件大小 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    // 相对于 classpath 的头像保存目录
    private static final String AVATAR_SAVE_DIRECTORY_RELATIVE_TO_CLASSPATH = "images/avatar/";
    // 用户通过 web 访问的头像路径前缀
    // 注意：这需要 web 服务器配置将 classpath 下的 images/avatar 目录映射到 /images/avatar 这个 web 路径
    private static final String AVATAR_WEB_ACCESS_PATH_PREFIX = "/images/avatar/";
    @Value("${server.port}")
    private String serverPort;
    private final IUserService userService;
    // 构造注入
    public FileController(IUserService userService) {
        this.userService = userService;
    }

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
            return Unification.fail(400, "文件不能大于50MB");
        }
        // 获取文件扩展名 (使用独立的方法)
        String fileExtension = getFileExtension(file);
        // 检查文件扩展名是否合法：如果获取到的扩展名非空，则必须在允许的列表中
        // 这意味着文件名没有扩展名是被允许的 (fileExtension 是 "")
        // 只有当有扩展名但不在允许列表时才失败
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            log.warn("文件扩展名不合法: {}", fileExtension);
            return Unification.fail(400, "只允许上传jpg、jpeg、png、gif格式的图片");
        }
        // 验证成功
        return null;
    }

    /**
     * 保存文件到指定目录（通用逻辑）
     *
     * @param file        要保存的文件
     * @param newFileName 保存后的文件名
     * @return 保存成功后文件的访问文件名 (即 newFileName)
     * @throws IOException 如果文件保存失败或目录创建失败
     */
    private String saveFile(MultipartFile file, String newFileName) throws IOException {
        // 获取 resources 目录的绝对路径
        String classPath = ResourceUtils.getURL("classpath:").getPath();
        // 构建完整的保存目录路径
        String fullSavePath = classPath + FileController.AVATAR_SAVE_DIRECTORY_RELATIVE_TO_CLASSPATH;
        File directory = new File(fullSavePath);
        // 创建保存目录（如果不存在）
        if (!directory.exists()) {
            // 如果目录创建失败，抛出异常
            if (!directory.mkdirs()) {
                log.error("创建目录失败: {}", fullSavePath);
                throw new IOException("服务器错误：无法创建文件存储目录");
            }
            log.info("成功创建目录: {}", fullSavePath);
        }
        // 构建目标文件路径
        File targetFile = new File(directory, newFileName);
        // 保存文件
        file.transferTo(targetFile);
        log.info("文件已保存至: {}", targetFile.getAbsolutePath());
        // 返回保存后的文件名
        return newFileName;
    }


    /**
     * 上传通用头像
     * 调用通用验证和保存逻辑
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/avatar/upload")
    public Unification<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 调用通用文件验证方法
        Unification<Map<String, Object>> validationResult = validateFile(file);
        if (validationResult != null) {
            return validationResult; // 如果验证失败，直接返回失败结果
        }
        try {
            String fileExtension = getFileExtension(file);// 获取文件扩展名
            String newFileName = System.currentTimeMillis() + fileExtension;// 生成新的文件名（使用时间戳确保唯一性）
            // 调用通用文件保存方法
            String savedFileName = saveFile(file, newFileName); // savedFileName 应该与 newFileName 相同
            // 构建文件访问路径（使用 web 访问前缀和保存后的文件名）
            String fileUrl = "http://localhost:" + serverPort + AVATAR_WEB_ACCESS_PATH_PREFIX + savedFileName;
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", savedFileName);
            return Unification.success(data, "上传成功");
        } catch (IOException e) {
            log.error("文件保存失败", e);
            return Unification.fail(500, "文件保存失败：" + e.getMessage());
        }
    }

    /**
     * 用户专用的头像上传方法
     * 调用通用验证和保存逻辑，并更新用户信息
     * @param file 头像文件
     * @param userId 用户ID
     * @return 上传结果
     */
    @PostMapping("/user/avatar/upload")
    public Unification<Map<String, Object>> uploadUserAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Integer userId) {

        // 检查用户ID是否有效
        if (userId == null || userId <= 0) {
            return Unification.fail(400, "无效的用户ID");
        }
        // 调用通用文件验证方法
        Unification<Map<String, Object>> validationResult = validateFile(file);
        if (validationResult != null) {
            return validationResult; // 如果验证失败，直接返回失败结果
        }
        try {
            String fileExtension = getFileExtension(file);// 获取文件扩展名
            String newFileName = "user_" + userId + "_" + System.currentTimeMillis() + fileExtension;// 生成新的文件名（使用用户ID和时间戳确保唯一性）
            String savedFileName = saveFile(file, newFileName);// 调用通用文件保存方法
            // 构建文件访问URL（使用 web 访问前缀和保存后的文件名）
            String fileUrl = "http://localhost:" + serverPort + AVATAR_WEB_ACCESS_PATH_PREFIX + savedFileName;
            // 更新用户头像信息到数据库
            boolean updateResult = userService.updateUserAvatar(userId, fileUrl);
            if (!updateResult) {
                log.error("更新用户头像信息失败, userId: {}", userId);
                // 注意：原代码在此处失败时不删除已保存的文件，简化后保持此行为
                return Unification.fail(500, "更新用户头像信息失败");
            }
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", savedFileName);
            return Unification.success(data, "用户头像上传成功");
        } catch (IOException e) {
            log.error("用户头像保存失败", e);
            return Unification.fail(500, "用户头像保存失败：" + e.getMessage());
        }
    }
}