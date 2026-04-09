package com.wendao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wendao.entity.Role;

import java.util.List;

public interface RoleService {

    IPage<Role> list(int page, int size, String keyword);

    Role getById(Long id);

    void create(Role role);

    void update(Role role);

    void delete(Long id);

    List<Role> getAll();

    void assignMenus(Long roleId, List<Long> menuIds);

    List<Long> getMenuIdsByRoleId(Long roleId);

    List<Role> getRolesByAdminId(Long adminId);

    void assignRoles(Long adminId, List<Long> roleIds);

    List<Long> getRoleIdsByAdminId(Long adminId);
}
