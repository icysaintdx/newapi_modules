package com.wendao.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.QrCodeFile;
import com.wendao.mapper.QrCodeFileMapper;
import com.wendao.model.ResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 二维码管理控制器
 */
@RestController
@RequestMapping("/admin/api/qrcode")
@Api(tags = "二维码管理")
@SaCheckRole("SUPER_ADMIN")
public class AdminQrCodeController {

    private static final Logger log = LoggerFactory.getLogger(AdminQrCodeController.class);

    @Autowired
    private QrCodeFileMapper qrCodeFileMapper;

    /**
     * 获取所有二维码列表
     */
    @GetMapping("/list")
    @ApiOperation("获取二维码列表")
    public ResponseVO getAllQrCodes(@RequestParam(required = false) String payType) {
        try {
            LambdaQueryWrapper<QrCodeFile> wrapper = new LambdaQueryWrapper<>();
            if (payType != null && !payType.isEmpty()) {
                wrapper.eq(QrCodeFile::getPayType, payType);
            }
            wrapper.orderByAsc(QrCodeFile::getPayType, QrCodeFile::getAmount, QrCodeFile::getQrNum);

            List<QrCodeFile> list = qrCodeFileMapper.selectList(wrapper);
            return ResponseVO.successResponse(list);
        } catch (Exception e) {
            log.error("获取二维码列表失败", e);
            return ResponseVO.errorResponse("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 单个上传二维码
     */
    @PostMapping("/upload")
    @ApiOperation("上传二维码")
    public ResponseVO upload(@RequestParam("file") MultipartFile file,
                            @RequestParam("payType") String payType,
                            @RequestParam("amount") BigDecimal amount) {
        try {
            if (file.isEmpty()) {
                return ResponseVO.errorResponse("请选择文件");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseVO.errorResponse("只能上传图片文件");
            }

            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";

            // 生成下一个序号（3位数字格式）
            int nextSeq = generateNextSequence(payType, amount);
            String seqStr = String.format("%03d", nextSeq);

            // 生成语义化文件名：{平台}_{类型}_{金额}_{序号}.{扩展名}
            String semanticFileName;
            String dir = String.format("/app/static/qr/%s/", payType);

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                // 固定金额：alipay_fixed_10.00_001.jpg
                semanticFileName = String.format("%s_fixed_%.2f_%s%s",
                    payType, amount, seqStr, extension);
            } else {
                // 普通收款码：alipay_open_001.jpg
                semanticFileName = String.format("%s_open_%s%s",
                    payType, seqStr, extension);
            }

            // 确保目录存在
            new File(dir).mkdirs();
            String filePath = dir + semanticFileName;

            // 保存文件
            Files.write(Paths.get(filePath), file.getBytes());

            // 保存到数据库
            QrCodeFile qrCodeFile = new QrCodeFile();
            qrCodeFile.setPayType(payType);
            qrCodeFile.setAmount(amount);
            qrCodeFile.setQrNum(nextSeq);
            qrCodeFile.setFilePath(filePath.replace("/app", ""));
            qrCodeFile.setFileName(semanticFileName);  // 存储语义化文件名
            qrCodeFile.setEnabled(true);

            qrCodeFileMapper.insert(qrCodeFile);
            log.info("上传二维码成功: {}", semanticFileName);

            return ResponseVO.successResponse(qrCodeFile);
        } catch (Exception e) {
            log.error("上传二维码失败", e);
            return ResponseVO.errorResponse("上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传二维码
     */
    @PostMapping("/upload/batch")
    @ApiOperation("批量上传二维码")
    public ResponseVO uploadBatch(@RequestParam("files") MultipartFile[] files,
                                   @RequestParam("payType") String payType,
                                   @RequestParam(value = "qrType", defaultValue = "fixed") String qrType,
                                   @RequestParam(value = "amount", required = false) BigDecimal amount) {
        try {
            if (files == null || files.length == 0) {
                return ResponseVO.errorResponse("请选择文件");
            }

            // 获取当前最大序号
            int nextSeq = generateNextSequence(payType, amount != null ? amount : BigDecimal.ZERO);

            int successCount = 0;
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // 验证文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    continue;
                }

                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".")
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".png";

                // 生成语义化文件名
                String seqStr = String.format("%03d", nextSeq);
                String semanticFileName;
                String dir = String.format("/app/static/qr/%s/", payType);

                if ("fixed".equals(qrType) && amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    // 固定金额：alipay_fixed_10.00_001.jpg
                    semanticFileName = String.format("%s_fixed_%.2f_%s%s",
                        payType, amount, seqStr, extension);
                } else {
                    // 普通收款码：alipay_open_001.jpg
                    semanticFileName = String.format("%s_open_%s%s",
                        payType, seqStr, extension);
                }

                // 确保目录存在
                new File(dir).mkdirs();
                String filePath = dir + semanticFileName;

                // 保存文件
                Files.write(Paths.get(filePath), file.getBytes());

                // 保存到数据库
                QrCodeFile qrCodeFile = new QrCodeFile();
                qrCodeFile.setPayType(payType);
                qrCodeFile.setAmount("fixed".equals(qrType) && amount != null ? amount : BigDecimal.ZERO);
                qrCodeFile.setQrNum(nextSeq);
                qrCodeFile.setFilePath(filePath.replace("/app", ""));
                qrCodeFile.setFileName(semanticFileName);  // 存储语义化文件名
                qrCodeFile.setEnabled(true);

                qrCodeFileMapper.insert(qrCodeFile);

                nextSeq++; // 自动递增序号
                successCount++;
            }

            log.info("批量上传二维码成功: count={}", successCount);
            return ResponseVO.successResponse("成功上传 " + successCount + " 个二维码");
        } catch (IOException e) {
            log.error("批量上传二维码失败", e);
            return ResponseVO.errorResponse("上传失败: " + e.getMessage());
        }
    }

    /**
     * 更新二维码信息
     */
    @PutMapping("/update/{id}")
    @ApiOperation("更新二维码信息")
    public ResponseVO updateQrCode(@PathVariable Long id,
                                    @RequestBody QrCodeFile qrCodeFile) {
        try {
            QrCodeFile existing = qrCodeFileMapper.selectById(id);
            if (existing == null) {
                return ResponseVO.errorResponse("二维码不存在");
            }

            // 更新信息
            if (qrCodeFile.getEnabled() != null) {
                existing.setEnabled(qrCodeFile.getEnabled());
            }

            qrCodeFileMapper.updateById(existing);
            log.info("更新二维码成功: id={}", id);

            return ResponseVO.successResponse(existing);
        } catch (Exception e) {
            log.error("更新二维码失败", e);
            return ResponseVO.errorResponse("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除二维码
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除二维码")
    public ResponseVO deleteQrCode(@PathVariable Long id) {
        try {
            QrCodeFile qrCodeFile = qrCodeFileMapper.selectById(id);
            if (qrCodeFile == null) {
                return ResponseVO.errorResponse("二维码不存在");
            }

            // 删除文件
            try {
                Files.deleteIfExists(Paths.get(qrCodeFile.getFilePath()));
            } catch (IOException e) {
                log.warn("删除文件失败: {}", qrCodeFile.getFilePath(), e);
            }

            // 删除数据库记录
            qrCodeFileMapper.deleteById(id);
            log.info("删除二维码成功: id={}", id);

            return ResponseVO.successResponse("删除成功");
        } catch (Exception e) {
            log.error("删除二维码失败", e);
            return ResponseVO.errorResponse("删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成下一个可用的序号
     */
    private Integer generateNextSequence(String payType, BigDecimal amount) {
        LambdaQueryWrapper<QrCodeFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QrCodeFile::getPayType, payType)
               .eq(QrCodeFile::getAmount, amount)
               .orderByDesc(QrCodeFile::getQrNum)
               .last("LIMIT 1");

        QrCodeFile last = qrCodeFileMapper.selectOne(wrapper);
        return last == null ? 1 : last.getQrNum() + 1;
    }
}
