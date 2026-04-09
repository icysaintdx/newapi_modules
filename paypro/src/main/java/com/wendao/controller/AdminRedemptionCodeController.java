package com.wendao.controller;

import com.wendao.entity.RedemptionCode;
import com.wendao.model.ResponseVO;
import com.wendao.service.RedemptionCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 兑换码管理后台接口
 */
@RestController
@RequestMapping("/admin/api/redemption-code")
public class AdminRedemptionCodeController {

    @Autowired
    private RedemptionCodeService redemptionCodeService;

    /**
     * 批量导入兑换码（文件上传）
     */
    @PostMapping("/import/file")
    public ResponseVO importFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("productId") Integer productId) {

        if (file.isEmpty()) {
            return ResponseVO.errorResponse("文件不能为空");
        }

        // 检查文件类型
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
            return ResponseVO.errorResponse("只支持 .txt 文件");
        }

        try {
            // 读取文件内容
            List<String> codes = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        codes.add(line);
                    }
                }
            }

            if (codes.isEmpty()) {
                return ResponseVO.errorResponse("文件中没有有效的兑换码");
            }

            // 生成批次ID
            String batchId = UUID.randomUUID().toString();

            // 批量导入
            int successCount = redemptionCodeService.batchImportCodes(productId, codes, batchId);

            Map<String, Object> result = new HashMap<>();
            result.put("total", codes.size());
            result.put("success", successCount);
            result.put("failed", codes.size() - successCount);
            result.put("batchId", batchId);

            return ResponseVO.successResponse(result);

        } catch (Exception e) {
            return ResponseVO.errorResponse("导入失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入兑换码（文本粘贴）
     */
    @PostMapping("/import/text")
    public ResponseVO importFromText(
            @RequestBody Map<String, Object> params) {

        Integer productId = (Integer) params.get("productId");
        String text = (String) params.get("text");

        if (productId == null) {
            return ResponseVO.errorResponse("商品ID不能为空");
        }

        if (text == null || text.trim().isEmpty()) {
            return ResponseVO.errorResponse("兑换码内容不能为空");
        }

        try {
            // 按行分割
            String[] lines = text.split("\\r?\\n");
            List<String> codes = new ArrayList<>();

            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    codes.add(line);
                }
            }

            if (codes.isEmpty()) {
                return ResponseVO.errorResponse("没有有效的兑换码");
            }

            // 生成批次ID
            String batchId = UUID.randomUUID().toString();

            // 批量导入
            int successCount = redemptionCodeService.batchImportCodes(productId, codes, batchId);

            Map<String, Object> result = new HashMap<>();
            result.put("total", codes.size());
            result.put("success", successCount);
            result.put("failed", codes.size() - successCount);
            result.put("batchId", batchId);

            return ResponseVO.successResponse(result);

        } catch (Exception e) {
            return ResponseVO.errorResponse("导入失败: " + e.getMessage());
        }
    }

    /**
     * 获取商品的兑换码库存信息
     */
    @GetMapping("/stock/{productId}")
    public ResponseVO getStock(@PathVariable Integer productId) {
        try {
            int availableStock = redemptionCodeService.getAvailableStock(productId);

            Map<String, Object> result = new HashMap<>();
            result.put("productId", productId);
            result.put("availableStock", availableStock);

            return ResponseVO.successResponse(result);
        } catch (Exception e) {
            return ResponseVO.errorResponse("查询失败: " + e.getMessage());
        }
    }
}
