package com.wendao.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.wendao.entity.Admin;
import com.wendao.entity.Menu;
import com.wendao.entity.Role;
import com.wendao.model.ResponseVO;
import com.wendao.service.AdminService;
import com.wendao.service.MenuService;
import com.wendao.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/admin/api")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @PostMapping("/login")
    public ResponseVO login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ip = getClientIp(httpRequest);
            Admin admin = adminService.login(request.getUsername(), request.getPassword(), ip);
            
            AdminVO adminVO = new AdminVO();
            adminVO.setId(admin.getId());
            adminVO.setUsername(admin.getUsername());
            adminVO.setNickname(admin.getNickname());
            adminVO.setAvatar(admin.getAvatar());
            
            return ResponseVO.successResponse(adminVO);
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseVO logout() {
        StpUtil.logout();
        return ResponseVO.successResponse();
    }

    @GetMapping("/info")
    public ResponseVO getInfo() {
        try {
            Admin admin = adminService.getCurrentAdmin();
            if (admin == null) {
                return ResponseVO.errorResponse("未登录");
            }
            
            AdminVO adminVO = new AdminVO();
            adminVO.setId(admin.getId());
            adminVO.setUsername(admin.getUsername());
            adminVO.setNickname(admin.getNickname());
            adminVO.setAvatar(admin.getAvatar());
            adminVO.setEmail(admin.getEmail());
            adminVO.setPhone(admin.getPhone());
            
            return ResponseVO.successResponse(adminVO);
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    // 密码修改
    @PostMapping("/change-password")
    public ResponseVO changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            Admin admin = adminService.getCurrentAdmin();
            adminService.changePassword(admin.getId(), request.getOldPassword(), request.getNewPassword());
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    // 角色管理
    @GetMapping("/roles")
    public ResponseVO getRoles(@RequestParam(defaultValue = "1") int page, 
                              @RequestParam(defaultValue = "10") int size, 
                              @RequestParam(required = false) String keyword) {
        try {
            return ResponseVO.successResponse(roleService.list(page, size, keyword));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/roles")
    public ResponseVO createRole(@RequestBody Role role) {
        try {
            roleService.create(role);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PutMapping("/roles/{id}")
    public ResponseVO updateRole(@PathVariable Long id, @RequestBody Role role) {
        try {
            role.setId(id);
            roleService.update(role);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @DeleteMapping("/roles/{id}")
    public ResponseVO deleteRole(@PathVariable Long id) {
        try {
            roleService.delete(id);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/roles/all")
    public ResponseVO getAllRoles() {
        try {
            return ResponseVO.successResponse(roleService.getAll());
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/roles/{id}")
    public ResponseVO getRole(@PathVariable Long id) {
        try {
            return ResponseVO.successResponse(roleService.getById(id));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/roles/{id}/menus")
    public ResponseVO assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        try {
            roleService.assignMenus(id, menuIds);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/roles/{id}/menus")
    public ResponseVO getRoleMenus(@PathVariable Long id) {
        try {
            return ResponseVO.successResponse(roleService.getMenuIdsByRoleId(id));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/admin/roles")
    public ResponseVO getAdminRoles() {
        try {
            Admin admin = adminService.getCurrentAdmin();
            return ResponseVO.successResponse(roleService.getRolesByAdminId(admin.getId()));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    // 菜单管理
    @GetMapping("/menus")
    public ResponseVO getMenus(@RequestParam(defaultValue = "1") int page, 
                              @RequestParam(defaultValue = "10") int size, 
                              @RequestParam(required = false) String keyword) {
        try {
            return ResponseVO.successResponse(menuService.list(page, size, keyword));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/menus")
    public ResponseVO createMenu(@RequestBody Menu menu) {
        try {
            menuService.create(menu);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @PutMapping("/menus/{id}")
    public ResponseVO updateMenu(@PathVariable Long id, @RequestBody Menu menu) {
        try {
            menu.setId(id);
            menuService.update(menu);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @DeleteMapping("/menus/{id}")
    public ResponseVO deleteMenu(@PathVariable Long id) {
        try {
            menuService.delete(id);
            return ResponseVO.successResponse();
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/menus/all")
    public ResponseVO getAllMenus() {
        try {
            return ResponseVO.successResponse(menuService.getAll());
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/menus/tree")
    public ResponseVO getMenuTree() {
        try {
            return ResponseVO.successResponse(menuService.getMenuTree());
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/menus/{id}")
    public ResponseVO getMenu(@PathVariable Long id) {
        try {
            return ResponseVO.successResponse(menuService.getById(id));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    @GetMapping("/menus/permission")
    public ResponseVO getPermissionMenus() {
        try {
            Admin admin = adminService.getCurrentAdmin();
            List<Menu> menus = menuService.getMenusByAdminId(admin.getId());
            return ResponseVO.successResponse(menuService.buildMenuTree(menus));
        } catch (Exception e) {
            return ResponseVO.errorResponse(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }

    @lombok.Data
    public static class AdminVO {
        private Long id;
        private String username;
        private String nickname;
        private String avatar;
        private String email;
        private String phone;
    }
}
