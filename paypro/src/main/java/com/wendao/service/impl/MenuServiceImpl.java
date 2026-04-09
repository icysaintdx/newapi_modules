package com.wendao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wendao.entity.AdminRole;
import com.wendao.entity.Menu;
import com.wendao.entity.RoleMenu;
import com.wendao.mapper.AdminRoleMapper;
import com.wendao.mapper.MenuMapper;
import com.wendao.mapper.RoleMenuMapper;
import com.wendao.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl implements MenuService {

    @Autowired
    private MenuMapper menuMapper;

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    @Override
    public IPage<Menu> list(int page, int size, String keyword) {
        Page<Menu> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Menu> queryWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Menu::getName, keyword);
        }
        queryWrapper.orderByAsc(Menu::getParentId).orderByAsc(Menu::getSort);
        return menuMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public Menu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public void create(Menu menu) {
        menu.setCreateTime(new Date());
        menu.setUpdateTime(new Date());
        menu.setSort(menu.getSort() == null ? 0 : menu.getSort());
        menu.setVisible(menu.getVisible() == null ? 1 : menu.getVisible());
        menu.setIsExternal(menu.getIsExternal() == null ? 0 : menu.getIsExternal());
        menuMapper.insert(menu);
    }

    @Override
    public void update(Menu menu) {
        menu.setUpdateTime(new Date());
        menuMapper.updateById(menu);
    }

    @Override
    public void delete(Long id) {
        // 检查是否有子菜单
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getParentId, id);
        if (menuMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("该菜单下有子菜单，无法删除");
        }

        // 删除角色菜单关联
        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.eq(RoleMenu::getMenuId, id);
        roleMenuMapper.delete(roleMenuWrapper);

        // 删除菜单
        menuMapper.deleteById(id);
    }

    @Override
    public List<Menu> getAll() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Menu::getParentId).orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }

    @Override
    public List<Menu> getMenusByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        return menuMapper.selectBatchIds(menuIds);
    }

    @Override
    public List<Menu> getMenusByAdminId(Long adminId) {
        // 获取管理员的角色
        LambdaQueryWrapper<AdminRole> adminRoleWrapper = new LambdaQueryWrapper<>();
        adminRoleWrapper.eq(AdminRole::getAdminId, adminId);
        List<AdminRole> adminRoles = adminRoleMapper.selectList(adminRoleWrapper);
        List<Long> roleIds = adminRoles.stream().map(AdminRole::getRoleId).collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取角色的菜单
        LambdaQueryWrapper<RoleMenu> roleMenuWrapper = new LambdaQueryWrapper<>();
        roleMenuWrapper.in(RoleMenu::getRoleId, roleIds);
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(roleMenuWrapper);
        List<Long> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).distinct().collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        return menuMapper.selectBatchIds(menuIds);
    }

    @Override
    public List<Menu> buildMenuTree(List<Menu> menus) {
        List<Menu> tree = new ArrayList<>();
        // 先找出所有根菜单
        List<Menu> rootMenus = menus.stream().filter(menu -> menu.getParentId() == 0).collect(Collectors.toList());
        // 递归构建子菜单
        for (Menu rootMenu : rootMenus) {
            buildChildMenu(rootMenu, menus);
            tree.add(rootMenu);
        }
        return tree;
    }

    private void buildChildMenu(Menu parentMenu, List<Menu> allMenus) {
        List<Menu> childMenus = allMenus.stream().filter(menu -> menu.getParentId().equals(parentMenu.getId())).collect(Collectors.toList());
        parentMenu.setChildren(childMenus);
        for (Menu childMenu : childMenus) {
            buildChildMenu(childMenu, allMenus);
        }
    }

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> menus = getAll();
        return buildMenuTree(menus);
    }
}
