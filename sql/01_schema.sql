-- ============================================================================
--  面向快递驿站的快件收发管理系统  数据库结构脚本
--  Graduation Project - Express Station Parcel Management System
--  DBMS : MySQL 8.0
--  Charset : utf8mb4 / utf8mb4_general_ci
--  Engine  : InnoDB
--
--  共 12 张表：5 张系统权限表 + 7 张业务表
--    系统权限：sys_user / sys_role / sys_user_role / sys_permission / sys_role_permission
--    业务核心：station / shelf / parcel / parcel_trace / pickup_record / ship_order / exception_record
-- ============================================================================

DROP DATABASE IF EXISTS `express_station`;
CREATE DATABASE `express_station` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `express_station`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. sys_user  用户表（系统管理员 / 驿站员工 / 普通用户 三类角色共用）
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `real_name`       VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `phone`           VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0 未知 1 男 2 女',
    `avatar`          VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `station_id`      BIGINT                DEFAULT NULL COMMENT '所属驿站（员工/管理员）',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：1 正常 0 禁用',
    `last_login_time` DATETIME              DEFAULT NULL COMMENT '最近登录时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_phone` (`phone`),
    KEY `idx_user_station` (`station_id`),
    KEY `idx_user_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ----------------------------------------------------------------------------
-- 2. sys_role  角色表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '角色主键',
    `role_code`   VARCHAR(50) NOT NULL COMMENT '角色编码：ADMIN/STAFF/USER',
    `role_name`   VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '角色描述',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用 0 停用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- ----------------------------------------------------------------------------
-- 3. sys_user_role  用户-角色关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`
(
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_ur_role` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- ----------------------------------------------------------------------------
-- 4. sys_permission  权限（菜单 / 按钮）表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '权限主键',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父权限ID，0 表示顶级',
    `perm_code`   VARCHAR(80) NOT NULL COMMENT '权限标识，如 parcel:create',
    `perm_name`   VARCHAR(50) NOT NULL COMMENT '权限名称',
    `perm_type`   VARCHAR(10) NOT NULL DEFAULT 'MENU' COMMENT '类型：MENU 菜单 BUTTON 按钮',
    `path`        VARCHAR(120)         DEFAULT NULL COMMENT '前端路由地址',
    `icon`        VARCHAR(50)          DEFAULT NULL COMMENT '菜单图标',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序号',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 启用 0 停用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perm_code` (`perm_code`),
    KEY `idx_perm_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

-- ----------------------------------------------------------------------------
-- 5. sys_role_permission  角色-权限关联表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`
(
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `perm_id` BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_perm` (`role_id`, `perm_id`),
    KEY `idx_rp_perm` (`perm_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

-- ----------------------------------------------------------------------------
-- 6. station  驿站（网点）表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `station`;
CREATE TABLE `station`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '驿站主键',
    `station_code`  VARCHAR(32)  NOT NULL COMMENT '驿站编号',
    `station_name`  VARCHAR(80)  NOT NULL COMMENT '驿站名称',
    `address`       VARCHAR(255) NOT NULL COMMENT '驿站地址',
    `contact_phone` VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    `manager_name`  VARCHAR(50)           DEFAULT NULL COMMENT '负责人',
    `business_hours` VARCHAR(60)          DEFAULT '08:00-21:00' COMMENT '营业时间',
    `capacity`      INT          NOT NULL DEFAULT 500 COMMENT '货位总容量',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 营业 0 停用',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_station_code` (`station_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='驿站表';

-- ----------------------------------------------------------------------------
-- 7. shelf  货架库位表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `shelf`;
CREATE TABLE `shelf`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '库位主键',
    `station_id`  BIGINT      NOT NULL COMMENT '所属驿站',
    `shelf_code`  VARCHAR(32) NOT NULL COMMENT '库位编号，如 A-01-01',
    `area`        VARCHAR(20)          DEFAULT NULL COMMENT '库区，如 A/B/C',
    `capacity`    INT         NOT NULL DEFAULT 40 COMMENT '库位容量（件）',
    `used_count`  INT         NOT NULL DEFAULT 0 COMMENT '已使用数量（件）',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1 可用 0 停用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shelf_code` (`station_id`, `shelf_code`),
    KEY `idx_shelf_station` (`station_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='货架库位表';

-- ----------------------------------------------------------------------------
-- 8. parcel  快件（包裹）主表
--    status 流转：IN_STORE 在库待取 -> PICKED_UP 已取件
--                          \-> DELIVERING 派送中 -> PICKED_UP 已签收
--                          \-> EXCEPTION 异常件 -> RETURNED 已退回
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `parcel`;
CREATE TABLE `parcel`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '快件主键',
    `waybill_no`      VARCHAR(40)  NOT NULL COMMENT '快递运单号',
    `station_id`      BIGINT       NOT NULL COMMENT '所在驿站',
    `shelf_id`        BIGINT                DEFAULT NULL COMMENT '存放库位',
    `express_company` VARCHAR(40)  NOT NULL COMMENT '快递公司',
    `parcel_type`     VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '快件类型：NORMAL 普通 SMALL 小件 LARGE 大件 FRAGILE 易碎 DOCUMENT 文件 COLD 生鲜',
    `receiver_name`   VARCHAR(50)  NOT NULL COMMENT '收件人姓名',
    `receiver_phone`  VARCHAR(20)  NOT NULL COMMENT '收件人手机号',
    `pickup_code`     VARCHAR(10)  NOT NULL COMMENT '取件码（8 位数字）',
    `weight`          DECIMAL(8, 2)         DEFAULT NULL COMMENT '重量(kg)',
    `freight`         DECIMAL(8, 2)         DEFAULT NULL COMMENT '代收运费(元)',
    `status`          VARCHAR(20)  NOT NULL DEFAULT 'IN_STORE' COMMENT '状态：IN_STORE 在库 PICKED_UP 已取件 DELIVERING 派送中 EXCEPTION 异常 RETURNED 已退回',
    `in_time`         DATETIME     NOT NULL COMMENT '入库时间',
    `pickup_time`     DATETIME              DEFAULT NULL COMMENT '取件/签收时间',
    `overdue_days`    INT          NOT NULL DEFAULT 3 COMMENT '免费保管天数',
    `storage_fee`     DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '逾期保管费(元)',
    `operator_id`     BIGINT                DEFAULT NULL COMMENT '入库操作员',
    `remark`          VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_parcel_waybill` (`waybill_no`),
    KEY `idx_parcel_station_status` (`station_id`, `status`),
    KEY `idx_parcel_pickup_code` (`pickup_code`),
    KEY `idx_parcel_receiver_phone` (`receiver_phone`),
    KEY `idx_parcel_in_time` (`in_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='快件表';

-- ----------------------------------------------------------------------------
-- 9. parcel_trace  快件轨迹（操作留痕）表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `parcel_trace`;
CREATE TABLE `parcel_trace`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '轨迹主键',
    `parcel_id`     BIGINT      NOT NULL COMMENT '快件ID',
    `waybill_no`    VARCHAR(40) NOT NULL COMMENT '运单号（冗余，便于查询）',
    `operate_type`  VARCHAR(30) NOT NULL COMMENT '操作类型：IN_STORE/PICKUP/DELIVER/EXCEPTION/RETURN/TRANSFER/EDIT',
    `operate_desc`  VARCHAR(255)         DEFAULT NULL COMMENT '操作描述',
    `station_id`    BIGINT               DEFAULT NULL COMMENT '操作驿站',
    `operator_id`   BIGINT               DEFAULT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50)          DEFAULT NULL COMMENT '操作人姓名（冗余）',
    `operate_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_trace_parcel` (`parcel_id`),
    KEY `idx_trace_waybill` (`waybill_no`),
    KEY `idx_trace_time` (`operate_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='快件轨迹表';

-- ----------------------------------------------------------------------------
-- 10. pickup_record  取件/出库记录表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `pickup_record`;
CREATE TABLE `pickup_record`
(
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '取件记录主键',
    `parcel_id`      BIGINT      NOT NULL COMMENT '快件ID',
    `waybill_no`     VARCHAR(40) NOT NULL COMMENT '运单号',
    `pickup_code`    VARCHAR(10) NOT NULL COMMENT '核销取件码',
    `receiver_name`  VARCHAR(50) NOT NULL COMMENT '实际取件人',
    `receiver_phone` VARCHAR(20)          DEFAULT NULL COMMENT '取件人手机号',
    `pickup_type`    VARCHAR(20) NOT NULL DEFAULT 'SELF' COMMENT '取件方式：SELF 本人自取 AGENT 代取 DELIVERY 送货上门',
    `verify_type`    VARCHAR(20) NOT NULL DEFAULT 'CODE' COMMENT '核验方式：CODE 取件码 ID_CARD 身份证 PHONE 手机号',
    `storage_fee`    DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '实收保管费(元)',
    `station_id`     BIGINT               DEFAULT NULL COMMENT '驿站',
    `operator_id`    BIGINT               DEFAULT NULL COMMENT '核销操作员',
    `operator_name`  VARCHAR(50)          DEFAULT NULL COMMENT '核销操作员姓名',
    `pickup_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '取件时间',
    `remark`         VARCHAR(255)         DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_pickup_parcel` (`parcel_id`),
    KEY `idx_pickup_waybill` (`waybill_no`),
    KEY `idx_pickup_time` (`pickup_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='取件记录表';

-- ----------------------------------------------------------------------------
-- 11. ship_order  寄件登记表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `ship_order`;
CREATE TABLE `ship_order`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '寄件单主键',
    `order_no`         VARCHAR(40)  NOT NULL COMMENT '寄件单号（S+时间戳）',
    `station_id`       BIGINT       NOT NULL COMMENT '受理驿站',
    `express_company`  VARCHAR(40)  NOT NULL COMMENT '快递公司',
    `sender_name`      VARCHAR(50)  NOT NULL COMMENT '寄件人姓名',
    `sender_phone`     VARCHAR(20)  NOT NULL COMMENT '寄件人手机号',
    `sender_address`   VARCHAR(255) NOT NULL COMMENT '寄件人地址',
    `receiver_name`    VARCHAR(50)  NOT NULL COMMENT '收件人姓名',
    `receiver_phone`   VARCHAR(20)  NOT NULL COMMENT '收件人手机号',
    `receiver_address` VARCHAR(255) NOT NULL COMMENT '收件人地址',
    `parcel_type`      VARCHAR(20)  NOT NULL DEFAULT 'NORMAL' COMMENT '快件类型',
    `weight`           DECIMAL(8, 2)         DEFAULT NULL COMMENT '重量(kg)',
    `freight`          DECIMAL(8, 2) NOT NULL DEFAULT 0.00 COMMENT '运费(元)',
    `insured_value`    DECIMAL(10, 2)         DEFAULT 0.00 COMMENT '保价金额(元)',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING 待揽收 ACCEPTED 已揽收 SHIPPED 已发出 CANCELLED 已取消',
    `waybill_no`       VARCHAR(40)           DEFAULT NULL COMMENT '回填的运单号',
    `operator_id`      BIGINT                DEFAULT NULL COMMENT '受理员工',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ship_order_no` (`order_no`),
    KEY `idx_ship_station` (`station_id`),
    KEY `idx_ship_status` (`status`),
    KEY `idx_ship_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='寄件登记表';

-- ----------------------------------------------------------------------------
-- 12. exception_record  异常件记录表
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `exception_record`;
CREATE TABLE `exception_record`
(
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '异常记录主键',
    `parcel_id`      BIGINT      NOT NULL COMMENT '快件ID',
    `waybill_no`     VARCHAR(40) NOT NULL COMMENT '运单号',
    `exception_type` VARCHAR(30) NOT NULL COMMENT '异常类型：DAMAGED 破损 LOST 丢失 ADDRESS_ERROR 地址错误 REFUSED 拒收 TIMEOUT 长期未取 OTHER 其他',
    `description`    VARCHAR(255)         DEFAULT NULL COMMENT '异常描述',
    `handle_status`  VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING 待处理 HANDLING 处理中 RESOLVED 已解决',
    `handler_id`     BIGINT               DEFAULT NULL COMMENT '处理人ID',
    `handler_name`   VARCHAR(50)          DEFAULT NULL COMMENT '处理人姓名',
    `handle_result`  VARCHAR(255)         DEFAULT NULL COMMENT '处理结果',
    `station_id`     BIGINT               DEFAULT NULL COMMENT '驿站',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    `handle_time`    DATETIME             DEFAULT NULL COMMENT '处理完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_exception_parcel` (`parcel_id`),
    KEY `idx_exception_status` (`handle_status`),
    KEY `idx_exception_type` (`exception_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='异常件记录表';

-- ----------------------------------------------------------------------------
-- 13. notify_record  取件/逾期通知记录表
--     驿站到件后需要通知收件人取件，逾期后需要催取；本表记录每一条通知的
--     渠道、内容、发送结果与操作人，既是业务凭证也是纠纷时的追溯依据。
--     真实项目中 channel=SMS 会对接阿里云/腾讯云短信网关，本项目以写入记录
--     的方式模拟发送过程，便于答辩演示且不产生费用。
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `notify_record`;
CREATE TABLE `notify_record`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知记录主键',
    `parcel_id`      BIGINT       NOT NULL COMMENT '快件ID',
    `waybill_no`     VARCHAR(40)  NOT NULL COMMENT '运单号',
    `pickup_code`    VARCHAR(10)           DEFAULT NULL COMMENT '取件码（便于直接告知客户）',
    `notify_type`    VARCHAR(20)  NOT NULL COMMENT '通知类型：IN_STORE 到件通知 OVERDUE 逾期催取 PICKUP_DONE 取件确认 EXCEPTION 异常通知',
    `channel`        VARCHAR(20)  NOT NULL DEFAULT 'SMS' COMMENT '通知渠道：SMS 短信 APP 站内通知 PHONE 电话',
    `receiver_phone` VARCHAR(20)  NOT NULL COMMENT '接收手机号',
    `content`        VARCHAR(500) NOT NULL COMMENT '通知内容',
    `send_status`    VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS' COMMENT '发送结果：SUCCESS 成功 FAILED 失败',
    `fail_reason`    VARCHAR(255)          DEFAULT NULL COMMENT '失败原因',
    `station_id`     BIGINT                DEFAULT NULL COMMENT '所属驿站',
    `operator_id`    BIGINT                DEFAULT NULL COMMENT '操作人ID（自动发送为 NULL）',
    `operator_name`  VARCHAR(50)           DEFAULT NULL COMMENT '操作人姓名',
    `send_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_notify_parcel` (`parcel_id`),
    KEY `idx_notify_waybill` (`waybill_no`),
    KEY `idx_notify_phone` (`receiver_phone`),
    KEY `idx_notify_type` (`notify_type`),
    KEY `idx_notify_time` (`send_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='取件通知记录表';

SET FOREIGN_KEY_CHECKS = 1;
