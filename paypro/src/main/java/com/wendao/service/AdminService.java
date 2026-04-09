package com.wendao.service;

import com.wendao.entity.Admin;

public interface AdminService {
    
    Admin login(String username, String password, String ip);
    
    Admin getCurrentAdmin();
    
    void updateLastLoginInfo(Long adminId, String ip);
    
    void changePassword(Long adminId, String oldPassword, String newPassword);
}
