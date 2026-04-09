package com.wendao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wendao.entity.AdminRole;
import com.wendao.entity.Role;
import com.wendao.entity.RoleMenu;
import com.wendao.mapper.AdminRoleMapper;
import com.wendao.mapper.RoleMapper;
import com.wendao.mapper.RoleMenuMapper;
import com.wendao.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    @Override
    public IPage<Role> list(int page, int size, String keyword) {
        Page<Role> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Role::getName, keyword).or().like(Role::getCode, keyword);
        }
        return roleMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public Role getById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public void create(Role role) {
        role.setCreateTime(new Date());
        role.setUpdateTime(new Date());
        role.setStatus(1);
        role.setIsSystem(0);
        roleMapper.insert(role);
    }

    @Override
    public void update(Role role) {
        role.setUpdateTime(new Date());
        roleMapper.updateById(role);
    }

    @Override
    public void delete(Long id) {
        // 检查是否为系统角色
        Role role = roleMapper.selectById(id);
        if (role.getIsSystem() == 1) {
            throw new RuntimeException("系统角色不能删除");
        }

        // 删除角色菜单关联
        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(RoleMenu::getRoleId, id);
        roleMenuMapper.delete(roleMenuWrapper);

        // 删除管理员角色关联
        LambdaQueryWrapper<AdminRole> adminRoleWrapper = new LambdaQueryWrapper<>();
        adminRoleWrapper.eq(AdminRole::getRoleId, id);
        adminRoleMapper.delete(adminRoleWrapper);

        // 删除角色
        roleMapper.deleteById(id);
    }

    @Override
    public List<Role> getAll() {
        return roleMapper.selectList(null);
    }

    @Transactional
    @Override
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 删除原有关联
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);

        // 添加新关联
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            roleMenu.setCreateTime(new Date());
            roleMenuMapper.insert(roleMenu);
        }
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    public List<Role> getRolesByAdminId(Long adminId) {
        LambdaQueryWrapper<AdminRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRole::getAdminId, adminId);
        List<AdminRole> adminRoles = adminRoleMapper.selectList(wrapper);
        List<Long> roleIds = adminRoles.stream().map(AdminRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds);
    }

    @Transactional
    @Override
    public void assignRoles(Long adminId, List<Long> roleIds) {
        // 删除原有关联
        LambdaQueryWrapper<AdminRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRole::getAdminId, adminId);
        adminRoleMapper.delete(wrapper);

        // 添加新关联
        for (Long roleId : roleIds) {
            AdminRole adminRole = new AdminRole();
            adminRole.setAdminId(adminId);
            adminRole.setRoleId(roleId);
            adminRole.setCreateTime(new Date());
            adminRoleMapper.insert(adminRole);
        }
    }

    @Override
    public List<Long> getRoleIdsByAdminId(Long adminId) {
        LambdaQueryWrapper<AdminRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminRole::getAdminId, adminId);
        List<AdminRole> adminRoles = adminRoleMapper.selectList(wrapper);
        return adminRoles.stream().map(AdminRole::getRoleId).collect(Collectors.toList());
    }
}
