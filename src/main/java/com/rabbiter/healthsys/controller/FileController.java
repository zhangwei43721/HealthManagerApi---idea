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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 * 处理文件上传相关操作
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    // 允许上传的图片扩展名
    private static final String ALLOWED_EXTENSIONS = ".jpg,.jpeg,.png,.gif";
    
    // 最大文件大小 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    @Value("${server.port}")
    private String serverPort;
    
    private final IUserService userService;
    
    // 构造注入
    public FileController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 上传头像
     * @param file 文件
     * @return 上传结果
     */
    @PostMapping("/avatar/upload")
    public Unification<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 检查文件是否为空
        if (file == null) {
            return Unification.fail(400, "参数文件为空");
        }
        
        // 检查文件大小
        if (file.getSize() <= 0) {
            return Unification.fail(400, "文件为空");
        }
        
        // 判断文件大小不能大于最大值
        if (file.getSize() > MAX_FILE_SIZE) {
            return Unification.fail(400, "文件不能大于50MB");
        }
        
        // 获取文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        
        // 检查文件扩展名是否合法
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            return Unification.fail(400, "只允许上传jpg、jpeg、png、gif格式的图片");
        }
        
        try {
            // 获取 resources 目录的绝对路径
            String classPath = ResourceUtils.getURL("classpath:").getPath();
            // 创建头像保存目录（如果不存在）
            String avatarPath = classPath + "images/avatar/";
            File directory = new File(avatarPath);
            
            if (!directory.exists() && !directory.mkdirs()) {
                log.error("创建目录失败: {}", avatarPath);
                return Unification.fail(500, "服务器错误：无法创建存储目录");
            }
            
            // 生成新的文件名（使用时间戳确保唯一性）
            String newFileName = System.currentTimeMillis() + fileExtension;
            File targetFile = new File(directory, newFileName);
            
            // 保存文件
            file.transferTo(targetFile);
            log.info("文件已保存: {}", targetFile.getAbsolutePath());
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            // 文件访问路径，格式为：http://localhost:端口号/images/avatar/文件名
            String fileUrl = "http://localhost:" + serverPort + "/images/avatar/" + newFileName;
            data.put("url", fileUrl);
            data.put("filename", newFileName);
            
            return Unification.success(data, "上传成功");
            
        } catch (IOException e) {
            log.error("文件保存失败", e);
            return Unification.fail(500, "文件保存失败：" + e.getMessage());
        }
    }
    
    /**
     * 用户专用的头像上传方法
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
        
        // 检查文件是否为空
        if (file == null) {
            return Unification.fail(400, "参数文件为空");
        }
        
        // 检查文件大小
        if (file.getSize() <= 0) {
            return Unification.fail(400, "文件为空");
        }
        
        // 判断文件大小不能大于最大值
        if (file.getSize() > MAX_FILE_SIZE) {
            return Unification.fail(400, "文件不能大于50MB");
        }
        
        // 获取文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        
        // 检查文件扩展名是否合法
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            return Unification.fail(400, "只允许上传jpg、jpeg、png、gif格式的图片");
        }
        
        try {
            // 获取 resources 目录的绝对路径
            String classPath = ResourceUtils.getURL("classpath:").getPath();
            // 创建头像保存目录（如果不存在）
            String avatarPath = classPath + "images/avatar/";
            File directory = new File(avatarPath);
            
            if (!directory.exists() && !directory.mkdirs()) {
                log.error("创建目录失败: {}", avatarPath);
                return Unification.fail(500, "服务器错误：无法创建存储目录");
            }
            
            // 生成新的文件名（使用用户ID和时间戳确保唯一性）
            String newFileName = "user_" + userId + "_" + System.currentTimeMillis() + fileExtension;
            File targetFile = new File(directory, newFileName);
            
            // 保存文件
            file.transferTo(targetFile);
            log.info("用户头像已保存: {}", targetFile.getAbsolutePath());
            
            // 构建文件访问URL
            String fileUrl = "http://localhost:" + serverPort + "/images/avatar/" + newFileName;
            
            // 更新用户头像信息到数据库
            boolean updateResult = userService.updateUserAvatar(userId, fileUrl);
            if (!updateResult) {
                log.error("更新用户头像信息失败, userId: {}", userId);
                return Unification.fail(500, "更新用户头像信息失败");
            }
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", newFileName);
            
            return Unification.success(data, "用户头像上传成功");
            
        } catch (IOException e) {
            log.error("用户头像保存失败", e);
            return Unification.fail(500, "用户头像保存失败：" + e.getMessage());
        }
    }
} 