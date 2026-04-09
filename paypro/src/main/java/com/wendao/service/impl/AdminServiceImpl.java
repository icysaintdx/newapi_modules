package com.wendao.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wendao.entity.Admin;
import com.wendao.mapper.AdminMapper;
import com.wendao.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public Admin login(String username, String password, String ip) {
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Admin::getUsername, username);
        Admin admin = adminMapper.selectOne(queryWrapper);
        
        if (admin == null) {
            throw new RuntimeException("用户名不存在");
        }
        
        if (admin.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }
        
        String md5Password = DigestUtil.md5Hex(password);
        if (!md5Password.equals(admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        StpUtil.login(admin.getId());
        
        updateLastLoginInfo(admin.getId(), ip);
        
        return admin;
    }

    @Override
    public Admin getCurrentAdmin() {
        Long loginId = StpUtil.getLoginIdAsLong();
        return adminMapper.selectById(loginId);
    }

    @Override
    public void updateLastLoginInfo(Long adminId, String ip) {
        Admin admin = new Admin();
        admin.setId(adminId);
        admin.setLastLoginTime(new Date());
        admin.setLastLoginIp(ip);
        adminMapper.updateById(admin);
    }

    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        // 获取管理员信息
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }

        // 验证原密码
        String md5OldPassword = DigestUtil.md5Hex(oldPassword);
        if (!md5OldPassword.equals(admin.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 检查新密码强度
        if (newPassword.length() < 6) {
            throw new RuntimeException("新密码长度至少为6位");
        }

        // 更新密码
        Admin updateAdmin = new Admin();
        updateAdmin.setId(adminId);
        updateAdmin.setPassword(DigestUtil.md5Hex(newPassword));
        updateAdmin.setUpdateTime(new Date());
        adminMapper.updateById(updateAdmin);
    }
}
