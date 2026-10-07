-- ============================================================================
--  面向快递驿站的快件收发管理系统  初始化数据脚本
--  执行顺序：先 01_schema.sql，再本脚本
--
--  演示账号（密码统一为 123456，BCrypt 加密存储）：
--    admin   / 123456   系统管理员
--    staff01 / 123456   幸福小区快递驿站 员工
--    staff02 / 123456   大学城菜鸟驿站   员工
--    user01  / 123456   普通用户（收件人）
--    user02  / 123456   普通用户（收件人）
-- ============================================================================

USE `express_station`;
SET NAMES utf8mb4;

-- ----------------------------------------------------------------------------
-- 角色
-- ----------------------------------------------------------------------------
DELETE FROM `sys_role`;
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `sort`, `status`)
VALUES (1, 'ADMIN', '系统管理员', '拥有系统全部权限，负责用户、角色、驿站等基础数据维护', 1, 1),
       (2, 'STAFF', '驿站员工', '负责快件入库登记、取件核销、寄件受理、异常件处理等日常业务', 2, 1),
       (3, 'USER', '普通用户', '收件人，可查询本人名下快件的在库与取件状态', 3, 1);

-- ----------------------------------------------------------------------------
-- 权限（菜单 + 按钮）
-- ----------------------------------------------------------------------------
DELETE FROM `sys_permission`;
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `perm_type`, `path`, `icon`, `sort`, `status`)
VALUES
-- 首页
(1, 0, 'dashboard', '首页概览', 'MENU', '/dashboard', 'Odometer', 1, 1),
-- 快件管理
(10, 0, 'parcel', '快件管理', 'MENU', '/parcel', 'Box', 2, 1),
(11, 10, 'parcel:list', '快件查询', 'MENU', '/parcel/list', 'Search', 1, 1),
(12, 10, 'parcel:in', '收件登记', 'MENU', '/parcel/in', 'Download', 2, 1),
(13, 10, 'parcel:pickup', '取件核销', 'MENU', '/parcel/pickup', 'Finished', 3, 1),
(14, 10, 'parcel:deliver', '派送操作', 'BUTTON', NULL, NULL, 4, 1),
(15, 10, 'parcel:edit', '编辑快件', 'BUTTON', NULL, NULL, 5, 1),
(16, 10, 'parcel:delete', '删除快件', 'BUTTON', NULL, NULL, 6, 1),
(17, 10, 'parcel:export', '导出台账', 'BUTTON', NULL, NULL, 7, 1),
(18, 10, 'parcel:trace', '查看轨迹', 'BUTTON', NULL, NULL, 8, 1),
-- 寄件管理
(20, 0, 'ship', '寄件管理', 'MENU', '/ship', 'Van', 3, 1),
(21, 20, 'ship:list', '寄件查询', 'MENU', '/ship/list', 'List', 1, 1),
(22, 20, 'ship:add', '寄件登记', 'BUTTON', NULL, NULL, 2, 1),
(23, 20, 'ship:edit', '编辑寄件单', 'BUTTON', NULL, NULL, 3, 1),
(24, 20, 'ship:delete', '删除寄件单', 'BUTTON', NULL, NULL, 4, 1),
(25, 20, 'ship:status', '更新寄件状态', 'BUTTON', NULL, NULL, 5, 1),
-- 异常件管理
(30, 0, 'exception', '异常件管理', 'MENU', '/exception', 'Warning', 4, 1),
(31, 30, 'exception:list', '异常件查询', 'MENU', '/exception/list', 'List', 1, 1),
(32, 30, 'exception:add', '异常件登记', 'BUTTON', NULL, NULL, 2, 1),
(33, 30, 'exception:handle', '异常件处理', 'BUTTON', NULL, NULL, 3, 1),
-- 数据统计
(40, 0, 'stats', '数据统计', 'MENU', '/stats', 'DataAnalysis', 5, 1),
(41, 40, 'stats:view', '查看统计', 'BUTTON', NULL, NULL, 1, 1),
-- 系统管理
(50, 0, 'system', '系统管理', 'MENU', '/system', 'Setting', 6, 1),
(51, 50, 'system:user:list', '用户管理', 'MENU', '/system/user', 'User', 1, 1),
(52, 50, 'system:user:add', '新增用户', 'BUTTON', NULL, NULL, 2, 1),
(53, 50, 'system:user:edit', '编辑用户', 'BUTTON', NULL, NULL, 3, 1),
(54, 50, 'system:user:delete', '删除用户', 'BUTTON', NULL, NULL, 4, 1),
(55, 50, 'system:user:reset', '重置密码', 'BUTTON', NULL, NULL, 5, 1),
(56, 50, 'system:role:list', '角色权限', 'MENU', '/system/role', 'Key', 6, 1),
(57, 50, 'system:role:assign', '分配权限', 'BUTTON', NULL, NULL, 7, 1),
(58, 50, 'system:station:list', '驿站管理', 'MENU', '/system/station', 'OfficeBuilding', 8, 1),
(59, 50, 'system:station:edit', '编辑驿站', 'BUTTON', NULL, NULL, 9, 1),
(60, 50, 'system:shelf:list', '货位管理', 'MENU', '/system/shelf', 'Grid', 10, 1),
(61, 50, 'system:shelf:edit', '编辑货位', 'BUTTON', NULL, NULL, 11, 1),
-- 个人中心
(70, 0, 'profile', '个人中心', 'MENU', '/profile', 'UserFilled', 7, 1);

-- ----------------------------------------------------------------------------
-- 角色-权限：ADMIN 全部权限
-- ----------------------------------------------------------------------------
DELETE FROM `sys_role_permission`;
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`)
SELECT 1, `id` FROM `sys_permission`;

-- STAFF：首页 + 快件 + 寄件 + 异常件 + 统计 + 个人中心（不含系统管理）
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`)
SELECT 2, `id`
FROM `sys_permission`
WHERE `id` IN (1, 10, 11, 12, 13, 14, 15, 17, 18,
               20, 21, 22, 23, 25,
               30, 31, 32, 33,
               40, 41,
               70);

-- USER：首页 + 我的快件 + 个人中心
INSERT INTO `sys_role_permission` (`role_id`, `perm_id`)
VALUES (3, 1), (3, 11), (3, 70);

-- ----------------------------------------------------------------------------
-- 用户（密码统一 123456）
-- ----------------------------------------------------------------------------
DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `gender`, `station_id`, `status`)
VALUES (1, 'admin', '$2a$10$/nw9pF5s75pR3mwGJMmFauv40lC4KIIxWen0E/q77fj47tnlPLiAK', '吴宇航', '13800000001', 1, NULL, 1),
       (2, 'staff01', '$2a$10$hVXzdkxcpbicxczrhu3xm.A3M97hNn8qeKNiyjeb18AlX15KbNMbG', '李思远', '13800000002', 1, 1, 1),
       (3, 'staff02', '$2a$10$ZhEGZGJ.Sofdm77RLb.ozus4U2qu101sa2jXXIWmoY5mGn7ODNUeW', '陈欣怡', '13800000003', 2, 2, 1),
       (4, 'user01', '$2a$10$852jWMUlBt9DN0OjUEbgcuIA4NLQ9xrU6x57eoGcfScX7EgrAj46i', '王小明', '13900000001', 1, NULL, 1),
       (5, 'user02', '$2a$10$x9RxJT64i5PWpIB456Hqn.n8ucWXvfnSY6S8aHEl4o4d.IznZaXz6', '赵丽娜', '13900000002', 2, NULL, 1);

DELETE FROM `sys_user_role`;
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (1, 1), (2, 2), (3, 2), (4, 3), (5, 3);

-- ----------------------------------------------------------------------------
-- 驿站
-- ----------------------------------------------------------------------------
DELETE FROM `station`;
INSERT INTO `station` (`id`, `station_code`, `station_name`, `address`, `contact_phone`, `manager_name`, `business_hours`, `capacity`, `status`)
VALUES (1, 'ST001', '幸福小区快递驿站', '江苏省南京市江宁区幸福路 128 号 1 栋 101', '025-88880001', '李思远', '08:00-21:00', 500, 1),
       (2, 'ST002', '大学城菜鸟驿站', '江苏省南京市江宁区大学城文苑路 9 号', '025-88880002', '陈欣怡', '07:30-22:30', 800, 1);

-- ----------------------------------------------------------------------------
-- 货架库位
-- ----------------------------------------------------------------------------
DELETE FROM `shelf`;
INSERT INTO `shelf` (`station_id`, `shelf_code`, `area`, `capacity`, `used_count`, `status`)
VALUES (1, 'A-01-01', 'A', 40, 0, 1), (1, 'A-01-02', 'A', 40, 0, 1),
       (1, 'A-01-03', 'A', 40, 0, 1), (1, 'A-01-04', 'A', 40, 0, 1),
       (1, 'B-01-01', 'B', 60, 0, 1), (1, 'B-01-02', 'B', 60, 0, 1),
       (1, 'C-01-01', 'C', 20, 0, 1),
       (2, 'A-01-01', 'A', 40, 0, 1), (2, 'A-01-02', 'A', 40, 0, 1),
       (2, 'A-01-03', 'A', 40, 0, 1), (2, 'A-01-04', 'A', 40, 0, 1),
       (2, 'B-01-01', 'B', 60, 0, 1), (2, 'B-01-02', 'B', 60, 0, 1),
       (2, 'D-01-01', 'D', 100, 0, 1);

-- ----------------------------------------------------------------------------
-- 快件（示范数据；时间基于当前时间向前推算，保证"今日入库/逾期"等统计有数据）
-- ----------------------------------------------------------------------------
DELETE FROM `parcel`;
INSERT INTO `parcel` (`id`, `waybill_no`, `station_id`, `shelf_id`, `express_company`, `parcel_type`,
                      `receiver_name`, `receiver_phone`, `pickup_code`, `weight`, `freight`, `status`,
                      `in_time`, `pickup_time`, `overdue_days`, `storage_fee`, `operator_id`, `remark`)
VALUES
-- ===== 驿站 1 =====
(1, 'SF1234567890123', 1, 1, '顺丰速运', 'NORMAL', '王小明', '13900000001', '10012034', 1.20, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, 3, 0.00, 2, '工作日 18:00 后自取'),
(2, 'SF1234567890124', 1, 1, '顺丰速运', 'DOCUMENT', '赵丽娜', '13900000002', '10015678', 0.30, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 3 HOUR), NULL, 3, 0.00, 2, NULL),
(3, 'JD9988776655441', 1, 2, '京东物流', 'LARGE', '李强', '13900000003', '10023451', 8.50, 12.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL, 3, 0.00, 2, '大件，放在 B 区'),
(4, 'ZT7788990011223', 1, 2, '中通快递', 'NORMAL', '孙倩', '13900000004', '10029876', 2.10, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 20 HOUR), NULL, 3, 0.00, 2, NULL),
(5, 'YT6655443322110', 1, 3, '圆通速递', 'FRAGILE', '周杰', '13900000005', '10031245', 3.40, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 3, 0.00, 2, '易碎品，轻拿轻放'),
(6, 'YD1122334455667', 1, 3, '韵达快递', 'SMALL', '吴敏', '13900000006', '10038765', 0.80, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, 3, 0.00, 2, NULL),
(7, 'EMS8899001122334', 1, 4, '邮政EMS', 'NORMAL', '郑浩', '13900000007', '10042319', 1.90, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, 3, 2.00, 2, '已超免费保管期'),
(8, 'JT5566778899001', 1, 4, '极兔速递', 'NORMAL', '钱多多', '13900000008', '10045678', 2.60, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 3, 6.00, 2, '多次电话未接通'),
(9, 'SF1234567890131', 1, 5, '顺丰速运', 'COLD', '冯雪', '13900000009', '10051234', 4.20, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, 1, 0.00, 2, '生鲜件，需尽快取件'),
(10, 'ZT7788990011240', 1, 5, '中通快递', 'NORMAL', '蒋涛', '13900000010', '10058765', 1.50, 0.00, 'PICKED_UP', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3, 0.00, 2, NULL),
(11, 'JD9988776655450', 1, 6, '京东物流', 'NORMAL', '韩梅', '13900000011', '10062345', 2.30, 0.00, 'PICKED_UP', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 3, 0.00, 2, NULL),
(12, 'YT6655443322180', 1, 6, '圆通速递', 'NORMAL', '杨光', '13900000012', '10067891', 1.10, 0.00, 'PICKED_UP', DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 3, 0.00, 2, '入库后第 3 天取走，未超免费保管期'),
(13, 'YD1122334455670', 1, 7, '韵达快递', 'NORMAL', '朱琳', '13900000013', '10073456', 3.00, 0.00, 'DELIVERING', DATE_SUB(NOW(), INTERVAL 10 HOUR), NULL, 3, 0.00, 2, '客户要求送货上门'),
(14, 'EMS8899001122340', 1, 7, '邮政EMS', 'FRAGILE', '秦淮', '13900000014', '10078912', 5.60, 0.00, 'EXCEPTION', DATE_SUB(NOW(), INTERVAL 7 DAY), NULL, 3, 8.00, 2, '外包装破损'),
(15, 'JT5566778899010', 1, NULL, '极兔速递', 'NORMAL', '史磊', '13900000015', '10084567', 1.70, 0.00, 'RETURNED', DATE_SUB(NOW(), INTERVAL 12 DAY), NULL, 3, 0.00, 2, '拒收退回'),
-- ===== 驿站 2 =====
(16, 'SF2234567890201', 2, 8, '顺丰速运', 'NORMAL', '唐婉', '13900000016', '20012345', 1.30, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, 3, 0.00, 3, NULL),
(17, 'JD9988776655500', 2, 8, '京东物流', 'LARGE', '沈括', '13900000017', '20018765', 9.20, 15.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, 3, 0.00, 3, NULL),
(18, 'ZT7788990011300', 2, 9, '中通快递', 'NORMAL', '姚远', '13900000018', '20023456', 2.00, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 3, 0.00, 3, NULL),
(19, 'YT6655443322200', 2, 9, '圆通速递', 'SMALL', '范冰', '13900000019', '20027891', 0.60, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 3, 0.00, 3, NULL),
(20, 'YD1122334455700', 2, 10, '韵达快递', 'NORMAL', '廖凡', '13900000020', '20033456', 2.80, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 5 DAY), NULL, 3, 4.00, 3, '超期未取'),
(21, 'EMS8899001122400', 2, 10, '邮政EMS', 'NORMAL', '贺军', '13900000021', '20038912', 1.40, 0.00, 'PICKED_UP', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 3, 0.00, 3, NULL),
(22, 'JT5566778899100', 2, 11, '极兔速递', 'NORMAL', '龚俊', '13900000022', '20044567', 2.20, 0.00, 'PICKED_UP', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 3, 0.00, 3, NULL),
(23, 'SF2234567890210', 2, 11, '顺丰速运', 'DOCUMENT', '白雪', '13900000023', '20047891', 0.40, 0.00, 'EXCEPTION', DATE_SUB(NOW(), INTERVAL 9 DAY), NULL, 3, 12.00, 3, '地址不详'),
(24, 'ZT7788990011310', 2, 12, '中通快递', 'NORMAL', '石磊', '13900000024', '20053456', 1.80, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 30 MINUTE), NULL, 3, 0.00, 3, NULL),
(25, 'JD9988776655510', 2, 12, '京东物流', 'COLD', '贺兰', '13900000025', '20057891', 3.60, 0.00, 'IN_STORE', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, 1, 0.00, 3, '生鲜件');

-- ----------------------------------------------------------------------------
-- 快件轨迹（与上面快件状态对应）
-- ----------------------------------------------------------------------------
DELETE FROM `parcel_trace`;
INSERT INTO `parcel_trace` (`parcel_id`, `waybill_no`, `operate_type`, `operate_desc`, `station_id`, `operator_id`, `operator_name`, `operate_time`)
SELECT `id`, `waybill_no`, 'IN_STORE',
       CONCAT('快件入库登记成功，取件码 ', `pickup_code`, '，存放库位 ', IFNULL((SELECT `shelf_code` FROM `shelf` s WHERE s.`id` = `parcel`.`shelf_id`), '待分配')),
       `station_id`, `operator_id`, IF(`operator_id` = 2, '李思远', '陈欣怡'), `in_time`
FROM `parcel`;

INSERT INTO `parcel_trace` (`parcel_id`, `waybill_no`, `operate_type`, `operate_desc`, `station_id`, `operator_id`, `operator_name`, `operate_time`)
SELECT `id`, `waybill_no`, 'PICKUP', '取件核销完成，快件已交付收件人', `station_id`, `operator_id`, IF(`operator_id` = 2, '李思远', '陈欣怡'), `pickup_time`
FROM `parcel`
WHERE `status` = 'PICKED_UP' AND `pickup_time` IS NOT NULL;

INSERT INTO `parcel_trace` (`parcel_id`, `waybill_no`, `operate_type`, `operate_desc`, `station_id`, `operator_id`, `operator_name`, `operate_time`)
VALUES (13, 'YD1122334455670', 'DELIVER', '员工外出派送中，预计 2 小时内送达', 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
       (14, 'EMS8899001122340', 'EXCEPTION', '到件时外包装已破损，登记为异常件待处理', 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 6 DAY)),
       (15, 'JT5566778899010', 'RETURNED', '收件人拒收，快件已退回发件网点', 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 10 DAY)),
       (23, 'SF2234567890210', 'EXCEPTION', '收件地址不详，电话联系不上，登记异常件', 2, 3, '陈欣怡', DATE_SUB(NOW(), INTERVAL 8 DAY));

-- ----------------------------------------------------------------------------
-- 取件记录
-- ----------------------------------------------------------------------------
DELETE FROM `pickup_record`;
INSERT INTO `pickup_record` (`parcel_id`, `waybill_no`, `pickup_code`, `receiver_name`, `receiver_phone`,
                             `pickup_type`, `verify_type`, `storage_fee`, `station_id`, `operator_id`, `operator_name`, `pickup_time`, `remark`)
VALUES (10, 'ZT7788990011240', '10058765', '蒋涛', '13900000010', 'SELF', 'CODE', 0.00, 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
       (11, 'JD9988776655450', '10062345', '韩梅', '13900000011', 'AGENT', 'PHONE', 0.00, 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 1 DAY), '家属代取'),
       (12, 'YT6655443322180', '10067891', '杨光', '13900000012', 'SELF', 'CODE', 4.00, 1, 2, '李思远', DATE_SUB(NOW(), INTERVAL 5 DAY), '逾期 2 天，收取保管费 4 元'),
       (21, 'EMS8899001122400', '20038912', '贺军', '13900000021', 'SELF', 'CODE', 0.00, 2, 3, '陈欣怡', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL),
       (22, 'JT5566778899100', '20044567', '龚俊', '13900000022', 'DELIVERY', 'ID_CARD', 0.00, 2, 3, '陈欣怡', DATE_SUB(NOW(), INTERVAL 3 DAY), '送货上门签收');

-- ----------------------------------------------------------------------------
-- 寄件单
-- ----------------------------------------------------------------------------
DELETE FROM `ship_order`;
INSERT INTO `ship_order` (`order_no`, `station_id`, `express_company`, `sender_name`, `sender_phone`, `sender_address`,
                          `receiver_name`, `receiver_phone`, `receiver_address`, `parcel_type`, `weight`, `freight`,
                          `insured_value`, `status`, `waybill_no`, `operator_id`, `create_time`)
VALUES ('S20260101100001', 1, '顺丰速运', '王小明', '13900000001', '江苏省南京市江宁区幸福路 128 号',
        '刘洋', '13700000001', '北京市海淀区中关村大街 1 号', 'NORMAL', 1.50, 18.00, 0.00, 'SHIPPED', 'SF3344556677889', 2, DATE_SUB(NOW(), INTERVAL 6 DAY)),
       ('S20260101100002', 1, '中通快递', '赵丽娜', '13900000002', '江苏省南京市江宁区幸福路 128 号',
        '陈晨', '13700000002', '上海市浦东新区世纪大道 100 号', 'DOCUMENT', 0.30, 12.00, 0.00, 'SHIPPED', 'ZT3344556677890', 2, DATE_SUB(NOW(), INTERVAL 5 DAY)),
       ('S20260101100003', 1, '京东物流', '李强', '13900000003', '江苏省南京市江宁区幸福路 128 号',
        '周舟', '13700000003', '广东省深圳市南山区科技园路 8 号', 'FRAGILE', 3.20, 26.00, 500.00, 'ACCEPTED', NULL, 2, DATE_SUB(NOW(), INTERVAL 2 DAY)),
       ('S20260101100004', 1, '圆通速递', '孙倩', '13900000004', '江苏省南京市江宁区幸福路 128 号',
        '吴桐', '13700000004', '浙江省杭州市西湖区文三路 66 号', 'NORMAL', 2.10, 15.00, 0.00, 'PENDING', NULL, 2, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
       ('S20260101100005', 2, '韵达快递', '唐婉', '13900000016', '江苏省南京市江宁区大学城文苑路 9 号',
        '郑一', '13700000005', '四川省成都市武侯区天府大道 1 号', 'SMALL', 0.80, 10.00, 0.00, 'SHIPPED', 'YD3344556677891', 3, DATE_SUB(NOW(), INTERVAL 4 DAY)),
       ('S20260101100006', 2, '极兔速递', '沈括', '13900000017', '江苏省南京市江宁区大学城文苑路 9 号',
        '冯二', '13700000006', '湖北省武汉市洪山区珞喻路 129 号', 'NORMAL', 4.60, 22.00, 200.00, 'CANCELLED', NULL, 3, DATE_SUB(NOW(), INTERVAL 8 DAY)),
       ('S20260101100007', 2, '邮政EMS', '姚远', '13900000018', '江苏省南京市江宁区大学城文苑路 9 号',
        '褚三', '13700000007', '陕西省西安市雁塔区长安南路 1 号', 'DOCUMENT', 0.20, 20.00, 0.00, 'ACCEPTED', NULL, 3, DATE_SUB(NOW(), INTERVAL 1 DAY)),
       ('S20260101100008', 1, '顺丰速运', '郑浩', '13900000007', '江苏省南京市江宁区幸福路 128 号',
        '卫四', '13700000008', '福建省厦门市思明区软件园二期', 'COLD', 5.00, 45.00, 1000.00, 'PENDING', NULL, 2, DATE_SUB(NOW(), INTERVAL 2 HOUR));

-- ----------------------------------------------------------------------------
-- 异常件记录
-- ----------------------------------------------------------------------------
DELETE FROM `exception_record`;
INSERT INTO `exception_record` (`parcel_id`, `waybill_no`, `exception_type`, `description`, `handle_status`,
                                `handler_id`, `handler_name`, `handle_result`, `station_id`, `create_time`, `handle_time`)
VALUES (14, 'EMS8899001122340', 'DAMAGED', '到件外包装破损，内件疑似受损，已拍照留证', 'HANDLING', 2, '李思远', '已联系发件网点协商理赔', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
       (23, 'SF2234567890210', 'ADDRESS_ERROR', '收件地址只写到小区，联系电话停机', 'PENDING', NULL, NULL, NULL, 2, DATE_SUB(NOW(), INTERVAL 8 DAY), NULL),
       (15, 'JT5566778899010', 'REFUSED', '收件人当场拒收，要求原路退回', 'RESOLVED', 2, '李思远', '已办理退回，运单状态置为已退回', 1, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 9 DAY));

-- ----------------------------------------------------------------------------
-- 按当前仍占用货位的快件重算货位占用，保证 shelf.used_count 与 parcel 表一致
--   占用口径：快件实体仍在货架上，即 IN_STORE 在库待取 / DELIVERING 派送中 / EXCEPTION 异常件
--   已取件（PICKED_UP）与已退回（RETURNED）的快件不再占用货位
-- ----------------------------------------------------------------------------
UPDATE `shelf` s
SET s.`used_count` = (SELECT COUNT(*)
                      FROM `parcel` p
                      WHERE p.`shelf_id` = s.`id`
                        AND p.`deleted` = 0
                        AND p.`status` IN ('IN_STORE', 'DELIVERING', 'EXCEPTION'));
