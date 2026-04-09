package com.wendao.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.AmountMapping;
import com.wendao.mapper.AmountMappingMapper;
import com.wendao.model.ResponseVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 金额映射管理控制器
 */
@RestController
@RequestMapping("/admin/api/amount-mapping")
@Api(tags = "金额映射管理")
@SaCheckRole("SUPER_ADMIN")
public class AdminAmountMappingController {

    private static final Logger log = LoggerFactory.getLogger(AdminAmountMappingController.class);

    @Autowired
    private AmountMappingMapper amountMappingMapper;

    /**
     * 获取所有金额映射列表
     */
    @GetMapping("/list")
    @ApiOperation("获取金额映射列表")
    public ResponseVO getAllMappings() {
        try {
            LambdaQueryWrapper<AmountMapping> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AmountMapping::getEnabled, true)
                   .orderByAsc(AmountMapping::getPayType, AmountMapping::getChargeAmount, AmountMapping::getPriority);

            List<AmountMapping> list = amountMappingMapper.selectList(wrapper);
            return ResponseVO.successResponse(list);
        } catch (Exception e) {
            log.error("获取金额映射列表失败", e);
            return ResponseVO.errorResponse("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 添加金额映射
     */
    @PostMapping("/add")
    @ApiOperation("添加金额映射")
    public ResponseVO addMapping(@RequestBody AmountMapping mapping) {
        try {
            if (mapping.getPayType() == null || mapping.getChargeAmount() == null || mapping.getActualAmount() == null) {
                return ResponseVO.errorResponse("请填写完整信息");
            }

            mapping.setEnabled(true);
            amountMappingMapper.insert(mapping);
            log.info("添加金额映射成功: {}", mapping);

            return ResponseVO.successResponse(mapping);
        } catch (Exception e) {
            log.error("添加金额映射失败", e);
            return ResponseVO.errorResponse("添加失败: " + e.getMessage());
        }
    }

    /**
     * 删除金额映射
     */
    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除金额映射")
    public ResponseVO deleteMapping(@PathVariable Long id) {
        try {
            AmountMapping mapping = amountMappingMapper.selectById(id);
            if (mapping == null) {
                return ResponseVO.errorResponse("映射不存在");
            }

            amountMappingMapper.deleteById(id);
            log.info("删除金额映射成功: id={}", id);

            return ResponseVO.successResponse("删除成功");
        } catch (Exception e) {
            log.error("删除金额映射失败", e);
            return ResponseVO.errorResponse("删除失败: " + e.getMessage());
        }
    }
}
