package com.wendao.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.wendao.entity.SystemConfig;
import com.wendao.model.ResponseVO;
import com.wendao.service.SystemConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 系统配置管理控制器（仅超级管理员可访问）
 */
@RestController
@RequestMapping("/admin/api/config")
@Api(tags = "系统配置管理")
@SaCheckRole("SUPER_ADMIN")
public class AdminConfigController {

    private static final Logger log = LoggerFactory.getLogger(AdminConfigController.class);

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取所有系统配置（按分组）
     */
    @GetMapping("/all")
    @ApiOperation("获取所有配置")
    public ResponseVO getAllConfigs() {
        try {
            Map<String, List<SystemConfig>> configs = systemConfigService.getAllConfigsByGroup();
            
            // 处理敏感信息，只显示是否已配置
            for (List<SystemConfig> configList : configs.values()) {
                for (SystemConfig config : configList) {
                    if (config.getIsSensitive() && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                        config.setConfigValue("******");
                    }
                }
            }
            
            return ResponseVO.successResponse(configs);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResponseVO.errorResponse("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 根据分组获取配置
     */
    @GetMapping("/group/{group}")
    @ApiOperation("根据分组获取配置")
    public ResponseVO getConfigsByGroup(@PathVariable String group) {
        try {
            List<SystemConfig> configs = systemConfigService.getConfigsByGroup(group);
            
            // 处理敏感信息
            for (SystemConfig config : configs) {
                if (config.getIsSensitive() && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                    config.setConfigValue("******");
                }
            }
            
            return ResponseVO.successResponse(configs);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResponseVO.errorResponse("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置
     */
    @PostMapping("/save")
    @ApiOperation("保存配置")
    public ResponseVO saveConfig(@RequestBody Map<String, String> configMap) {
        try {
            log.info("保存配置: {}", configMap.keySet());
            
            // 过滤掉值为 "******" 的敏感配置（表示不修改）
            Map<String, String> filteredConfigs = new HashMap<>();
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                if (!"******".equals(entry.getValue())) {
                    filteredConfigs.put(entry.getKey(), entry.getValue());
                }
            }
            
            systemConfigService.batchSaveConfigs(filteredConfigs);
            log.info("配置保存成功");
            
            return ResponseVO.successResponse("配置保存成功");
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResponseVO.errorResponse("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 上传自定义二维码图片（用于数据库配置）
     */
    @PostMapping("/upload/qrcode")
    @ApiOperation("上传自定义二维码")
    public ResponseVO uploadQrCode(@RequestParam("file") MultipartFile file,
                                    @RequestParam("type") String type) {
        try {
            if (file.isEmpty()) {
                return ResponseVO.errorResponse("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseVO.errorResponse("只能上传图片文件");
            }

            // 创建上传目录
            String uploadDir = "/app/static/qrcodes/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";
            String filename = type + "_" + UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

            // 返回访问URL
            String url = "/static/qrcodes/" + filename;
            log.info("自定义二维码上传成功: {}", url);

            return ResponseVO.successResponse(url);
        } catch (IOException e) {
            log.error("上传二维码失败", e);
            return ResponseVO.errorResponse("上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传固定金额二维码（保存到 /app/static/qr/{payType}/{amount}/{num}.png）
     */
    @PostMapping("/upload/fixed-amount-qrcode")
    @ApiOperation("上传固定金额二维码")
    public ResponseVO uploadFixedAmountQrCode(@RequestParam("file") MultipartFile file,
                                               @RequestParam("payType") String payType,
                                               @RequestParam("amount") String amount,
                                               @RequestParam("num") Integer num) {
        try {
            if (file.isEmpty()) {
                return ResponseVO.errorResponse("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseVO.errorResponse("只能上传图片文件");
            }

            // 验证金额格式（必须是两位小数）
            if (!amount.matches("\\d+\\.\\d{2}")) {
                return ResponseVO.errorResponse("金额格式错误，必须是两位小数，如 10.01");
            }

            // 创建目录结构：/app/static/qr/{payType}/{amount}/
            String uploadDir = "/app/static/qr/" + payType + "/" + amount + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 文件名：{num}.png
            String filename = num + ".png";

            // 保存文件
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

            // 返回访问URL
            String url = "/assets/qr/" + payType + "/" + amount + "/" + filename;
            log.info("固定金额二维码上传成功: {}", url);

            return ResponseVO.successResponse(url);
        } catch (IOException e) {
            log.error("上传固定金额二维码失败", e);
            return ResponseVO.errorResponse("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传固定金额二维码
     */
    @PostMapping("/upload/batch-fixed-amount-qrcode")
    @ApiOperation("批量上传固定金额二维码")
    public ResponseVO uploadBatchFixedAmountQrCode(@RequestParam("files") MultipartFile[] files,
                                                    @RequestParam("payType") String payType,
                                                    @RequestParam("amount") String amount) {
        try {
            if (files == null || files.length == 0) {
                return ResponseVO.errorResponse("请选择文件");
            }

            // 验证金额格式
            if (!amount.matches("\\d+\\.\\d{2}")) {
                return ResponseVO.errorResponse("金额格式错误，必须是两位小数，如 10.01");
            }

            // 创建目录
            String uploadDir = "/app/static/qr/" + payType + "/" + amount + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            int successCount = 0;
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file.isEmpty()) {
                    continue;
                }

                // 文件名：1.png, 2.png, 3.png...
                String filename = (i + 1) + ".png";
                Path filePath = Paths.get(uploadDir + filename);
                Files.write(filePath, file.getBytes());
                successCount++;
            }

            log.info("批量上传固定金额二维码成功: payType={}, amount={}, count={}", payType, amount, successCount);
            return ResponseVO.successResponse("成功上传 " + successCount + " 个二维码");
        } catch (IOException e) {
            log.error("批量上传固定金额二维码失败", e);
            return ResponseVO.errorResponse("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个配置值
     */
    @GetMapping("/value/{key}")
    @ApiOperation("获取单个配置值")
    public ResponseVO getConfigValue(@PathVariable String key) {
        try {
            String value = systemConfigService.getConfigValue(key);
            return ResponseVO.successResponse(value);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResponseVO.errorResponse("获取配置失败: " + e.getMessage());
        }
    }
}
