# 快件收发管理系统 —— 前后端接口契约（v1.0）

> 本文件是前端与后端共同遵守的唯一接口约定。任何一方修改接口，必须同步修改本文件。
> 后端 Base URL：`http://localhost:8080/api`（前端开发环境通过 Vite 代理 `/api` → 该地址）

---

## 1. 通用约定

### 1.1 统一响应体

所有接口（含文件下载除外）均返回如下结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

| code | 含义 |
| ---- | ---- |
| 200  | 成功 |
| 400  | 请求参数校验不通过 |
| 401  | 未登录或 Token 失效/过期 |
| 403  | 已登录但无该操作权限 |
| 404  | 资源不存在 |
| 500  | 服务器内部错误 |
| 1001 | 业务异常（如"运单号已存在""取件码错误"），`message` 为可直接展示给用户的中文提示 |

### 1.2 分页响应

`data` 固定为：

```json
{
  "total": 25,
  "pages": 2,
  "pageNum": 1,
  "pageSize": 20,
  "list": []
}
```

分页请求统一使用 query 参数：`pageNum`（默认 1）、`pageSize`（默认 10）。

### 1.3 认证

- 登录成功后返回 `token`，前端存入 localStorage（key：`es_token`）。
- 后续所有请求在请求头携带：`Authorization: Bearer <token>`。
- 后端返回 401 时，前端清除 token 并跳转登录页。

### 1.4 角色与权限

角色编码：`ADMIN`（系统管理员）、`STAFF`（驿站员工）、`USER`（普通用户）。

权限标识（`perm_code`）清单见 `sql/02_data.sql`。前端按钮级控制使用 `v-perm="'parcel:export'"` 指令或 `hasPerm('parcel:export')` 方法。

---

## 2. 认证模块 `/auth`

### 2.1 登录

`POST /api/auth/login`

请求：
```json
{ "username": "admin", "password": "123456" }
```

响应 `data`：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userInfo": {
    "id": 1,
    "username": "admin",
    "realName": "吴宇航",
    "phone": "13800000001",
    "avatar": null,
    "stationId": null,
    "stationName": null,
    "roles": ["ADMIN"],
    "permissions": ["dashboard", "parcel", "parcel:list", "..."]
  }
}
```

### 2.2 注册

`POST /api/auth/register`（无需登录）

请求：
```json
{
  "username": "zhangsan",
  "password": "123456",
  "confirmPassword": "123456",
  "realName": "张三",
  "phone": "13900000099"
}
```

说明：注册用户默认分配 `USER` 角色。`username` 唯一，`phone` 需为 11 位手机号，密码 6-20 位。

### 2.3 退出登录

`POST /api/auth/logout` → `data: null`

### 2.4 当前登录用户信息

`GET /api/auth/me` → `data` 同 `userInfo` 结构

### 2.5 当前用户菜单树

`GET /api/auth/menus` → `data`：

```json
[
  {
    "id": 10,
    "permCode": "parcel",
    "permName": "快件管理",
    "path": "/parcel",
    "icon": "Box",
    "children": [
      { "id": 11, "permCode": "parcel:list", "permName": "快件查询", "path": "/parcel/list", "icon": "Search", "children": [] }
    ]
  }
]
```

---

## 3. 快件模块 `/parcels`

### 3.1 收件登记（入库）

`POST /api/parcels/in-store`

```json
{
  "waybillNo": "SF1234567890999",
  "stationId": 1,
  "expressCompany": "顺丰速运",
  "parcelType": "NORMAL",
  "receiverName": "王小明",
  "receiverPhone": "13900000001",
  "weight": 1.5,
  "freight": 0,
  "shelfId": 1,
  "overdueDays": 3,
  "remark": "客户要求短信通知"
}
```

- `waybillNo` 后端按快递公司正则校验，重复运单号返回 `1001 该运单号已登记，请勿重复入库`。
- `receiverPhone` 11 位手机号。
- `shelfId` 可不传，后端自动分配当前驿站剩余容量最大的可用库位。
- 成功后返回新建快件对象，其中 `pickupCode` 为系统生成的 8 位取件码。

### 3.2 按取件码 / 运单号 / 手机号查询快件（取件核销页使用）

`GET /api/parcels/query?keyword={取件码或运单号或手机号}&stationId=1`

响应 `data`：快件对象数组（最多 20 条），字段见 3.4。

### 3.3 取件核销（出库）

`POST /api/parcels/pickup`

```json
{
  "waybillNo": "SF1234567890123",
  "pickupCode": "10012034",
  "receiverName": "王小明",
  "receiverPhone": "13900000001",
  "pickupType": "SELF",
  "verifyType": "CODE",
  "storageFee": 0,
  "remark": ""
}
```

- 必须同时匹配 `waybillNo` 与 `pickupCode`，否则返回 `1001 取件码不正确`。
- 快件状态必须为 `IN_STORE`，否则返回 `1001 该快件当前状态不可取件`。
- 后端在同一事务内：更新快件状态为 `PICKED_UP`、写入 `pickup_record`、写轨迹、**释放货位占用**。

### 3.4 快件对象（列表 / 详情通用）

```json
{
  "id": 1,
  "waybillNo": "SF1234567890123",
  "stationId": 1,
  "stationName": "幸福小区快递驿站",
  "shelfId": 1,
  "shelfCode": "A-01-01",
  "expressCompany": "顺丰速运",
  "parcelType": "NORMAL",
  "parcelTypeName": "普通件",
  "receiverName": "王小明",
  "receiverPhone": "13900000001",
  "pickupCode": "10012034",
  "weight": 1.2,
  "freight": 0,
  "status": "IN_STORE",
  "statusName": "在库待取",
  "inTime": "2026-10-07 17:39:47",
  "pickupTime": null,
  "overdueDays": 3,
  "storageDays": 0,
  "overdueFee": 0,
  "operatorId": 2,
  "operatorName": "李思远",
  "remark": "工作日 18:00 后自取",
  "createTime": "2026-10-07 17:39:47"
}
```

状态枚举与中文名：

| status | statusName |
| ------ | ---------- |
| IN_STORE | 在库待取 |
| PICKED_UP | 已取件 |
| DELIVERING | 派送中 |
| EXCEPTION | 异常件 |
| RETURNED | 已退回 |

快件类型枚举：

| parcelType | 中文 |
| ---------- | ---- |
| NORMAL | 普通件 |
| SMALL | 小件 |
| LARGE | 大件 |
| FRAGILE | 易碎品 |
| DOCUMENT | 文件 |
| COLD | 生鲜 |

### 3.5 分页条件查询

`GET /api/parcels/page?pageNum=1&pageSize=10&waybillNo=&pickupCode=&receiverName=&receiverPhone=&expressCompany=&status=&parcelType=&stationId=&startTime=2026-10-01&endTime=2026-10-07`

所有参数可选。`USER` 角色调用时后端强制只返回 `receiverPhone` 等于本人手机号的快件。

### 3.6 快件详情

`GET /api/parcels/{id}` → `data` 为快件对象

### 3.7 快件轨迹

`GET /api/parcels/{id}/traces` → `data`：

```json
[
  { "id": 1, "operateType": "IN_STORE", "operateDesc": "快件入库登记成功…", "operatorName": "李思远", "operateTime": "2026-10-07 17:39:47" }
]
```

### 3.8 编辑快件

`PUT /api/parcels/{id}`，可改：`receiverName`、`receiverPhone`、`parcelType`、`shelfId`、`overdueDays`、`remark`。需 `parcel:edit` 权限。

### 3.9 删除快件

`DELETE /api/parcels/{id}`（逻辑删除，同时释放货位）。需 `parcel:delete` 权限。

### 3.10 派送

`PUT /api/parcels/{id}/deliver` → 状态由 `IN_STORE` 改为 `DELIVERING`，写轨迹。需 `parcel:deliver` 权限。

### 3.11 逾期费用试算

`GET /api/parcels/{id}/overdue-fee` → `data: { "storageDays": 5, "overdueDays": 3, "overdueFee": 4.00 }`

### 3.12 导出台账

`GET /api/parcels/export?<与 3.5 相同筛选参数>` → 直接返回 `.xlsx` 文件流（`Content-Disposition: attachment`），前端用 `window.open` 或 blob 下载。需 `parcel:export` 权限。

---

## 4. 寄件模块 `/ship-orders`

### 4.1 寄件登记

`POST /api/ship-orders`

```json
{
  "stationId": 1,
  "expressCompany": "顺丰速运",
  "senderName": "王小明",
  "senderPhone": "13900000001",
  "senderAddress": "江苏省南京市江宁区幸福路 128 号",
  "receiverName": "刘洋",
  "receiverPhone": "13700000001",
  "receiverAddress": "北京市海淀区中关村大街 1 号",
  "parcelType": "NORMAL",
  "weight": 1.5,
  "freight": 18,
  "insuredValue": 0
}
```

后端自动生成 `orderNo`（`S` + `yyyyMMddHHmmss` + 2 位随机数），初始状态 `PENDING`。

### 4.2 寄件单对象

```json
{
  "id": 1,
  "orderNo": "S2026100718394712",
  "stationId": 1,
  "stationName": "幸福小区快递驿站",
  "expressCompany": "顺丰速运",
  "senderName": "王小明",
  "senderPhone": "13900000001",
  "senderAddress": "江苏省南京市江宁区幸福路 128 号",
  "receiverName": "刘洋",
  "receiverPhone": "13700000001",
  "receiverAddress": "北京市海淀区中关村大街 1 号",
  "parcelType": "NORMAL",
  "parcelTypeName": "普通件",
  "weight": 1.5,
  "freight": 18,
  "insuredValue": 0,
  "status": "PENDING",
  "statusName": "待揽收",
  "waybillNo": null,
  "operatorId": 2,
  "operatorName": "李思远",
  "createTime": "2026-10-07 18:39:47"
}
```

状态枚举：`PENDING` 待揽收、`ACCEPTED` 已揽收、`SHIPPED` 已发出、`CANCELLED` 已取消。

### 4.3 接口清单

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/ship-orders/page?pageNum&pageSize&orderNo&senderName&senderPhone&receiverName&receiverPhone&status&stationId&startTime&endTime` | 分页查询 | `ship:list` |
| GET | `/api/ship-orders/{id}` | 详情 | `ship:list` |
| POST | `/api/ship-orders` | 登记 | `ship:add` |
| PUT | `/api/ship-orders/{id}` | 编辑（仅 PENDING 可改） | `ship:edit` |
| PUT | `/api/ship-orders/{id}/status?status=ACCEPTED&waybillNo=SF123` | 更新状态并回填运单号 | `ship:status` |
| DELETE | `/api/ship-orders/{id}` | 逻辑删除 | `ship:delete` |

---

## 5. 异常件模块 `/exceptions`

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/exceptions/page?pageNum&pageSize&waybillNo&exceptionType&handleStatus&stationId` | 分页查询 | `exception:list` |
| POST | `/api/exceptions` | 登记异常（同时把快件状态置为 `EXCEPTION`，写轨迹） | `exception:add` |
| PUT | `/api/exceptions/{id}/handle` | 处理异常 | `exception:handle` |

登记请求：
```json
{ "parcelId": 14, "exceptionType": "DAMAGED", "description": "外包装破损" }
```

处理请求：
```json
{ "handleStatus": "RESOLVED", "handleResult": "已协商理赔，快件退回", "parcelStatus": "RETURNED" }
```
`parcelStatus` 可选，传入时同步更新快件状态。

异常记录对象：
```json
{
  "id": 1, "parcelId": 14, "waybillNo": "EMS8899001122340",
  "exceptionType": "DAMAGED", "exceptionTypeName": "破损",
  "description": "到件外包装破损", "handleStatus": "HANDLING", "handleStatusName": "处理中",
  "handlerName": "李思远", "handleResult": "已联系发件网点协商理赔",
  "stationName": "幸福小区快递驿站",
  "createTime": "2026-10-01 10:00:00", "handleTime": "2026-10-02 10:00:00"
}
```

异常类型：`DAMAGED` 破损、`LOST` 丢失、`ADDRESS_ERROR` 地址错误、`REFUSED` 拒收、`TIMEOUT` 长期未取、`OTHER` 其他。
处理状态：`PENDING` 待处理、`HANDLING` 处理中、`RESOLVED` 已解决。

---

## 6. 统计模块 `/stats`

### 6.1 概览

`GET /api/stats/overview?stationId=` → `data`：

```json
{
  "todayInCount": 3,
  "todayPickupCount": 1,
  "todayShipCount": 2,
  "inStoreCount": 15,
  "overdueCount": 4,
  "exceptionCount": 2,
  "deliveringCount": 1,
  "totalParcelCount": 25,
  "shelfUsage": { "total": 320, "used": 15, "rate": 4.7 }
}
```

### 6.2 近 N 日出入库趋势

`GET /api/stats/trend?days=7&stationId=` → `data`：

```json
{
  "dates": ["10-01", "10-02", "10-03"],
  "inCounts": [3, 5, 2],
  "pickupCounts": [1, 4, 3]
}
```

### 6.3 快递公司分布

`GET /api/stats/company?stationId=` → `data`：

```json
[ { "name": "顺丰速运", "value": 8 }, { "name": "中通快递", "value": 4 } ]
```

### 6.4 快件类型分布

`GET /api/stats/parcel-type?stationId=` → `data`：同 6.3 结构（`name` 为中文类型名）。

### 6.5 驿站业务量排行

`GET /api/stats/station-rank`（仅 ADMIN）→ `data`：

```json
[ { "stationName": "幸福小区快递驿站", "parcelCount": 15, "pickupCount": 3 } ]
```

---

## 7. 系统管理模块

### 7.1 用户管理 `/users`（ADMIN）

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/users/page?pageNum&pageSize&username&realName&phone&status&roleCode&stationId` | 分页 | `system:user:list` |
| POST | `/api/users` | 新增（可指定 `roleIds`、`stationId`） | `system:user:add` |
| PUT | `/api/users/{id}` | 编辑 | `system:user:edit` |
| DELETE | `/api/users/{id}` | 逻辑删除 | `system:user:delete` |
| PUT | `/api/users/{id}/status?status=0` | 启用/禁用 | `system:user:edit` |
| PUT | `/api/users/{id}/password?password=123456` | 重置密码 | `system:user:reset` |
| PUT | `/api/users/profile` | 修改本人资料 | 登录即可 |
| PUT | `/api/users/self/password` | 修改本人密码，body `{oldPassword,newPassword}` | 登录即可 |

用户对象：
```json
{
  "id": 2, "username": "staff01", "realName": "李思远", "phone": "13800000002",
  "gender": 1, "avatar": null, "stationId": 1, "stationName": "幸福小区快递驿站",
  "status": 1, "roleIds": [2], "roleNames": ["驿站员工"],
  "lastLoginTime": "2026-10-07 18:00:00", "createTime": "2026-09-01 10:00:00"
}
```

### 7.2 角色管理 `/roles`（ADMIN）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/roles/list` | 全部启用角色 |
| GET | `/api/roles/page?pageNum&pageSize` | 分页 |
| POST | `/api/roles` | 新增 |
| PUT | `/api/roles/{id}` | 编辑 |
| DELETE | `/api/roles/{id}` | 删除 |
| GET | `/api/roles/{id}/permissions` | 该角色已有权限 id 数组 |
| PUT | `/api/roles/{id}/permissions` | body `{ "permIds": [1,10,11] }` 重新分配 |

### 7.3 权限树 `/permissions`（ADMIN）

`GET /api/permissions/tree?type=MENU|ALL`（默认 `ALL`）→ `data`：

```json
[ { "id": 10, "permCode": "parcel", "permName": "快件管理", "permType": "MENU", "children": [ ... ] } ]
```

### 7.4 驿站管理 `/stations`（ADMIN）

| 方法 | 路径 | 说明 |
| ---- | ---- | ---- |
| GET | `/api/stations/list` | 全部启用驿站（所有登录用户可读，用于下拉框） |
| GET | `/api/stations/page?pageNum&pageSize&stationName&status` | 分页 |
| POST | `/api/stations` | 新增 |
| PUT | `/api/stations/{id}` | 编辑 |
| DELETE | `/api/stations/{id}` | 删除 |

驿站对象：
```json
{
  "id": 1, "stationCode": "ST001", "stationName": "幸福小区快递驿站",
  "address": "江苏省南京市江宁区幸福路 128 号 1 栋 101", "contactPhone": "025-88880001",
  "managerName": "李思远", "businessHours": "08:00-21:00", "capacity": 500,
  "shelfCount": 7, "usedCount": 10, "status": 1, "createTime": "2026-09-01 10:00:00"
}
```

### 7.5 货位管理 `/shelves`

| 方法 | 路径 | 说明 | 权限 |
| ---- | ---- | ---- | ---- |
| GET | `/api/shelves/list?stationId=1` | 该驿站全部货位 | 登录即可 |
| GET | `/api/shelves/available?stationId=1` | 仅剩余容量 > 0 的货位 | 登录即可 |
| GET | `/api/shelves/page?pageNum&pageSize&stationId&shelfCode&area` | 分页 | `system:shelf:list` |
| POST | `/api/shelves` | 新增 | `system:shelf:edit` |
| PUT | `/api/shelves/{id}` | 编辑 | `system:shelf:edit` |
| DELETE | `/api/shelves/{id}` | 删除（有在库快件时禁止删除） | `system:shelf:edit` |

货位对象：
```json
{ "id": 1, "stationId": 1, "stationName": "幸福小区快递驿站", "shelfCode": "A-01-01", "area": "A", "capacity": 40, "usedCount": 2, "freeCount": 38, "status": 1 }
```

---

## 8. 前端页面与权限对照

| 页面 | 路由 | 可见角色 | 依赖权限 |
| ---- | ---- | -------- | -------- |
| 登录 | `/login` | 全部（免登录） | — |
| 注册 | `/register` | 全部（免登录） | — |
| 首页概览 | `/dashboard` | 全部 | `dashboard` |
| 快件查询 | `/parcel/list` | 全部（USER 仅本人） | `parcel:list` |
| 收件登记 | `/parcel/in` | ADMIN / STAFF | `parcel:in` |
| 取件核销 | `/parcel/pickup` | ADMIN / STAFF | `parcel:pickup` |
| 寄件查询 | `/ship/list` | ADMIN / STAFF | `ship:list` |
| 异常件管理 | `/exception/list` | ADMIN / STAFF | `exception:list` |
| 数据统计 | `/stats` | ADMIN / STAFF | `stats:view` |
| 用户管理 | `/system/user` | ADMIN | `system:user:list` |
| 角色权限 | `/system/role` | ADMIN | `system:role:list` |
| 驿站管理 | `/system/station` | ADMIN | `system:station:list` |
| 货位管理 | `/system/shelf` | ADMIN | `system:shelf:list` |
| 个人中心 | `/profile` | 全部 | `profile` |

---

## 9. 错误码与常见提示语

| 场景 | code | message |
| ---- | ---- | ------- |
| 用户名或密码错误 | 1001 | 用户名或密码错误 |
| 账号被禁用 | 1001 | 账号已被禁用，请联系管理员 |
| 用户名已存在 | 1001 | 该用户名已被注册 |
| 运单号重复 | 1001 | 该运单号已登记，请勿重复入库 |
| 运单号格式错误 | 1001 | 运单号格式不正确，请核对快递公司 |
| 取件码错误 | 1001 | 取件码不正确，请核对 |
| 快件状态不允许取件 | 1001 | 该快件当前状态不可取件 |
| 货位已满 | 1001 | 该货位已满，请选择其他货位 |
| Token 过期 | 401 | 登录已过期，请重新登录 |
| 无权限 | 403 | 无操作权限，请联系管理员 |
