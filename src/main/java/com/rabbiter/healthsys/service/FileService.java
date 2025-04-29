package com.rabbiter.healthsys.service;

import com.rabbiter.healthsys.common.Unification;
import com.rabbiter.healthsys.utils.R2Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class FileService {

    @Autowired
    private R2Utils r2Utils;

    public Unification<Map<String, Object>> upload(MultipartFile file) {
        try {
            String fileUrl = r2Utils.uploadFile(file);
            Map<String, Object> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("filename", file.getOriginalFilename());

            return Unification.success(data, "文件上传成功");

        } catch (IOException e) {
            log.error("文件上传到 R2 失败", e);
            return Unification.fail(500, "文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("处理文件上传时发生未知错误", e);
            return Unification.fail(500, "文件上传处理失败: " + e.getMessage());
        }
    }
} 