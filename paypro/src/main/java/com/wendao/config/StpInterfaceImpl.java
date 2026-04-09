package com.wendao.config;

import cn.dev33.satoken.stp.StpInterface;
import com.wendao.entity.Role;
import com.wendao.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SaToken 权限验证接口实现
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private RoleService roleService;

    /**
     * 返回指定账号id所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 这里可以根据需要实现权限列表
        return new ArrayList<>();
    }

    /**
     * 返回指定账号id所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long adminId = Long.parseLong(loginId.toString());
            List<Role> roles = roleService.getRolesByAdminId(adminId);
            return roles.stream()
                    .map(Role::getCode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
