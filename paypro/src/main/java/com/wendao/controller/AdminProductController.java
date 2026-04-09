package com.wendao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wendao.common.utils.StringUtils;
import com.wendao.entity.Product;
import com.wendao.mapper.ProductMapper;
import com.wendao.model.ResponseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/product")
public class AdminProductController {

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/list")
    public ResponseVO list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String type) {
        
        Page<Product> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.isNotBlank(productName)) {
            queryWrapper.like(Product::getProductName, productName);
        }
        if (StringUtils.isNotBlank(type)) {
            queryWrapper.eq(Product::getType, type);
        }
        
        queryWrapper.orderByDesc(Product::getCreateTime);
        
        IPage<Product> result = productMapper.selectPage(pageParam, queryWrapper);
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("size", size);
        
        return ResponseVO.successResponse(data);
    }

    @GetMapping("/detail/{id}")
    public ResponseVO detail(@PathVariable Integer id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ResponseVO.errorResponse("商品不存在");
        }
        return ResponseVO.successResponse(product);
    }

    @PostMapping("/add")
    public ResponseVO add(@RequestBody Product product) {
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        product.setDel(0);
        
        int result = productMapper.insert(product);
        if (result > 0) {
            return ResponseVO.successResponse("添加成功");
        } else {
            return ResponseVO.errorResponse("添加失败");
        }
    }

    @PostMapping("/update")
    public ResponseVO update(@RequestBody Product product) {
        Product existProduct = productMapper.selectById(product.getId());
        if (existProduct == null) {
            return ResponseVO.errorResponse("商品不存在");
        }
        
        product.setUpdateTime(new Date());
        int result = productMapper.updateById(product);
        if (result > 0) {
            return ResponseVO.successResponse("更新成功");
        } else {
            return ResponseVO.errorResponse("更新失败");
        }
    }

    @PostMapping("/delete/{id}")
    public ResponseVO delete(@PathVariable Integer id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ResponseVO.errorResponse("商品不存在");
        }
        
        int result = productMapper.deleteById(id);
        if (result > 0) {
            return ResponseVO.successResponse("删除成功");
        } else {
            return ResponseVO.errorResponse("删除失败");
        }
    }

    @GetMapping("/all")
    public ResponseVO all() {
        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Product::getDel, 0);
        queryWrapper.orderByDesc(Product::getCreateTime);
        
        return ResponseVO.successResponse(productMapper.selectList(queryWrapper));
    }
}
