package com.wendao.controller;

import com.wendao.model.ResponseVO;
import com.wendao.service.ExtractCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提取码控制器
 */
@RestController
@RequestMapping("/api/extract")
public class ExtractCodeController {

    private static final Logger log = LoggerFactory.getLogger(ExtractCodeController.class);

    @Autowired
    private ExtractCodeService extractCodeService;

    /**
     * 使用提取码获取兑换码
     */
    @PostMapping("/use")
    public ResponseVO<String> useExtractCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            return ResponseVO.errorResponse("请输入提取码");
        }

        if (code.length() != 6) {
            return ResponseVO.errorResponse("提取码格式错误，应为6位数字");
        }

        try {
            String redemptionCode = extractCodeService.useExtractCode(code);

            if (redemptionCode == null) {
                return ResponseVO.errorResponse("提取码无效、已使用或已过期");
            }

            log.info("提取码使用成功: code={}", code);
            return ResponseVO.successResponse(redemptionCode);

        } catch (Exception e) {
            log.error("使用提取码失败: code={}, error={}", code, e.getMessage());
            return ResponseVO.errorResponse("系统错误，请稍后重试");
        }
    }
}
