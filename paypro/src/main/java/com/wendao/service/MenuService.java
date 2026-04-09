package com.wendao.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wendao.entity.Menu;

import java.util.List;

public interface MenuService {

    IPage<Menu> list(int page, int size, String keyword);

    Menu getById(Long id);

    void create(Menu menu);

    void update(Menu menu);

    void delete(Long id);

    List<Menu> getAll();

    List<Menu> getMenusByRoleId(Long roleId);

    List<Menu> getMenusByAdminId(Long adminId);

    List<Menu> buildMenuTree(List<Menu> menus);

    List<Menu> getMenuTree();
}
