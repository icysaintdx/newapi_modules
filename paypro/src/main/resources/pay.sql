/*
 Navicat Premium Data Transfer

 Source Server         : txy2
 Source Server Type    : MySQL
 Source Server Version : 50742
 Source Host           : 118.89.50.209:27887
 Source Schema         : pay

 Target Server Type    : MySQL
 Target Server Version : 50742
 File Encoding         : 65001

 Date: 27/02/2026 09:32:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_auto_pass_pay
-- ----------------------------
DROP TABLE IF EXISTS `t_auto_pass_pay`;
CREATE TABLE `t_auto_pass_pay`  (
                                    `id` bigint(255) NOT NULL,
                                    `order_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                    `message_id` bigint(11) NULL DEFAULT NULL COMMENT '消息id',
                                    `message_create_time` datetime(0) NULL DEFAULT NULL COMMENT '消息创建时间',
                                    `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
                                    `message_desc` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '消息备注',
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
                            `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
                            `create_time` datetime NULL DEFAULT NULL,
                            `custom` bit(1) NULL DEFAULT NULL,
                            `device` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `mobile` bit(1) NULL DEFAULT NULL,
                            `money` decimal(19, 2) NULL DEFAULT NULL,
                            `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `pay_num` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `pay_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `state` int(11) NULL DEFAULT NULL,
                            `update_time` datetime NULL DEFAULT NULL,
                            `user_id` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
                            `product_id` bigint(10) NULL DEFAULT NULL,
                            `pay_qr_num` int(10) NULL DEFAULT NULL,
                            `order_source` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'PRODUCT' COMMENT '订单来源,PRODUCT来自产品表，OTHER其他',
                            `notify_url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '通知地址',
                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_pay_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `t_pay_chat_message`;
CREATE TABLE `t_pay_chat_message`  (
                                       `id` bigint(255) NOT NULL,
                                       `seq` bigint(20) NULL DEFAULT NULL,
                                       `time` datetime(0) NULL DEFAULT NULL,
                                       `talker` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       `talker_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       `is_chat_room` tinyint(1) NULL DEFAULT NULL,
                                       `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       `sender_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       `is_self` tinyint(1) NULL DEFAULT NULL,
                                       `type` int(10) NULL DEFAULT NULL,
                                       `sub_type` int(10) NULL DEFAULT NULL,
                                       `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
                                       `contents` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
                                       `platform_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       `process_status` tinyint(1) NULL DEFAULT NULL COMMENT '处理状态',
                                       `create_time` datetime(0) NULL DEFAULT NULL,
                                       `order_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
                                       PRIMARY KEY (`id`) USING BTREE,
                                       KEY `idx_process_status` (`process_status`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_product
-- ----------------------------
DROP TABLE IF EXISTS `t_product`;
CREATE TABLE `t_product`  (
                              `id` int(11) NOT NULL AUTO_INCREMENT,
                              `create_time` datetime(6) NULL DEFAULT NULL,
                              `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述',
                              `money` decimal(19, 2) NULL DEFAULT NULL COMMENT '金额',
                              `product_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '产品名称',
                              `update_time` datetime(6) NULL DEFAULT NULL,
                              `item_info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '具体的物品配置JSON格式{gold:1,diamound:1,itemList:[]} 金币 钻石 物品配置',
                              `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '产品类型：GAME CODE',
                              `extend` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '拓展信息',
                              `del` int(11) NULL DEFAULT 0,
                              PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;


-- 后台管理员表
CREATE TABLE IF NOT EXISTS `t_admin` (
                                         `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码（加密）',
    `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员表';

-- 插入默认管理员账号（用户名: admin, 密码: admin123，使用MD5加密）
INSERT INTO `t_admin` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '超级管理员', 1);


-- ==========================================
-- PayPro 后台管理系统 - 角色权限相关表结构
-- ==========================================

-- 1. 角色表
CREATE TABLE IF NOT EXISTS `t_role` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(50) NOT NULL COMMENT '角色名称',
    `code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
    `status` tinyint(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_system` tinyint(1) DEFAULT 0 COMMENT '是否系统内置：0-否，1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 2. 菜单表
CREATE TABLE IF NOT EXISTS `t_menu` (
                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(50) NOT NULL COMMENT '菜单名称',
    `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
    `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
    `icon` varchar(100) DEFAULT NULL COMMENT '菜单图标',
    `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID，0表示顶级菜单',
    `sort` int(11) DEFAULT 0 COMMENT '排序号',
    `visible` tinyint(1) DEFAULT 1 COMMENT '是否可见：0-隐藏，1-显示',
    `is_external` tinyint(1) DEFAULT 0 COMMENT '是否外链：0-否，1-是',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_sort` (`sort`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 3. 角色菜单关联表
CREATE TABLE IF NOT EXISTS `t_role_menu` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_menu_id` (`menu_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 4. 管理员角色关联表
CREATE TABLE IF NOT EXISTS `t_admin_role` (
                                              `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_id` bigint(20) NOT NULL COMMENT '管理员ID',
    `role_id` bigint(20) NOT NULL COMMENT '角色ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role` (`admin_id`, `role_id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_role_id` (`role_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员角色关联表';


-- ==========================================
-- 初始化数据
-- ==========================================

-- 1. 插入默认角色
INSERT INTO `t_role` (`id`, `name`, `code`, `description`, `status`, `is_system`) VALUES
                                                                                      (1, '超级管理员', 'SUPER_ADMIN', '拥有系统所有权限', 1, 1),
                                                                                      (2, '管理员', 'ADMIN', '拥有大部分管理权限', 1, 1),
                                                                                      (3, '运营人员', 'OPERATOR', '负责日常运营工作', 1, 0),
                                                                                      (4, '客服人员', 'SERVICE', '负责客户服务工作', 1, 0);

-- 2. 插入菜单数据
INSERT INTO `t_menu` (`id`, `name`, `path`, `component`, `icon`, `parent_id`, `sort`, `visible`, `is_external`) VALUES
                                                                                                                    (1, '控制台', '/admin/index.html', NULL, 'ri-home-line', 0, 1, 1, 0),
                                                                                                                    (2, '订单管理', '/admin/orders.html', NULL, 'ri-file-list-3-line', 0, 2, 1, 0),
                                                                                                                    (3, '商品管理', '/admin/products.html', NULL, 'ri-shopping-bag-line', 0, 3, 1, 0),
                                                                                                                    (4, '角色管理', '/admin/role.html', NULL, 'ri-shield-line', 0, 4, 1, 0),
                                                                                                                    (5, '菜单管理', '/admin/menu.html', NULL, 'ri-menu-line', 0, 5, 1, 0);

-- 3. 插入角色菜单关联关系
-- 超级管理员拥有所有菜单权限
INSERT INTO `t_role_menu` (`role_id`, `menu_id`) VALUES
                                                     (1, 1), (1, 2), (1, 3), (1, 4), (1, 5);

-- 管理员拥有除菜单管理外的所有权限
INSERT INTO `t_role_menu` (`role_id`, `menu_id`) VALUES
                                                     (2, 1), (2, 2), (2, 3), (2, 4);

-- 运营人员拥有订单管理和商品管理权限
INSERT INTO `t_role_menu` (`role_id`, `menu_id`) VALUES
                                                     (3, 1), (3, 2), (3, 3);

-- 客服人员只有订单管理权限
INSERT INTO `t_role_menu` (`role_id`, `menu_id`) VALUES
                                                     (4, 1), (4, 2);

-- 4. 为默认管理员分配超级管理员角色
INSERT INTO `t_admin_role` (`admin_id`, `role_id`) VALUES
    (1, 1);