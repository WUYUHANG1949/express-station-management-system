# 快件收发管理系统 —— 前后端接口契约（v1.1）

> 本文件是前端与后端共同遵守的唯一接口约定。任何一方修改接口，必须同步修改本文件。
> 后端 Base URL：`http://localhost:8080/api`（前端开发环境通过 Vite 代理 `/api` → 该地址）
>
> **v1.1 变更摘要**：新增第 10 节「通知模块 `/notifications`」与第 11 节「公开接口 `/public`」；
> 快件模块补充 3.13、3.14 两个逾期接口；统计模块补充 6.6 数据大屏聚合接口；
> 货位管理补充 7.5 节中的货位地图与占用重算接口；第 8 节页面清单补充 5 个新页面；
> 第 12 节新增「枚举字典汇总」。全部内容与 `sql/01_schema.sql`、`backend/src/main/java/**` 源码逐条核对。

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

权限标识（`perm_code`）清单见 `sql/02_data.sql`。v1.1 权限总数由 35 项增加到 **43 项**，
其中 ADMIN 43 项（全部）、STAFF 29 项、USER 3 项（`dashboard`、`parcel:list`、`profile`）。
前端按钮级控制使用 `v-perm="'parcel:export'"` 指令或 `hasPerm('parcel:export')` 方法。

v1.1 新增的 8 项权限：

| id | perm_code | 名称 | 类型 | 挂载父级 | path | icon |
| -- | --------- | ---- | ---- | -------- | ---- | ---- |
| 19 | `parcel:overdue` | 逾期催取 | MENU | 10 `parcel` 快件管理 | `/parcel/overdue` | `AlarmClock` |
| 102 | `parcel:print` | 打印取件小票 | BUTTON | 10 `parcel` 快件管理 | — | — |
| 80 | `notify` | 通知管理 | MENU | 顶级 | `/notify` | `Bell` |
| 81 | `notify:list` | 通知记录 | MENU | 80 `notify` | `/notify/list` | `ChatDotSquare` |
| 82 | `notify:send` | 发送通知 | BUTTON | 80 `notify` | — | — |
| 90 | `shelfmap` | 货位地图 | MENU | 顶级 | `/shelf-map` | `MapLocation` |
| 91 | `shelfmap:view` | 查看货位地图 | BUTTON | 90 `shelfmap` | — | — |
| 110 | `screen` | 数据大屏 | MENU | 顶级 | `/screen` | `Monitor` |

### 1.5 免登录接口

`/api/public/**` 下的接口在 `WebMvcConfig` 中被显式排除登录拦截，调用时**不携带 `Authorization` 头**，
后端也不会把 `UserContext` 注入登录信息。目前仅第 11 节的取件码自助查询一个接口，
该接口内部只暴露取件必要字段，不返回任何内部标识与费用信息。

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
- 入库成功后在主事务内自动写入一条 `notify_type = IN_STORE`、`channel = SMS` 的到件通知
  （通知写入失败不影响入库结果，详见第 10 节）。

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

- 必须同时匹配 `waybillNo` 与 `pickupCode`，否则返回 `1001 取件码不正确，请核对`。
- 快件状态必须为 `IN_STORE` 或 `DELIVERING`（v1.1 起支持派送中当面签收），否则返回
  `1001 该快件当前状态为「xxx」，不可取件`。
- `pickupType` 取值 `SELF` / `AGENT` / `DELIVERY`（**送货上门的枚举值为 `DELIVERY`**），
  `verifyType` 取值 `CODE` / `ID_CARD` / `PHONE`；两者留空时分别默认为 `SELF`、`CODE`，
  传入约定枚举之外的取值返回 `1001 非法的取件方式：xxx` / `1001 非法的核验方式：xxx`。
- 后端在同一事务内：更新快件状态为 `PICKED_UP`、写入 `pickup_record`、写轨迹、**释放货位占用**，
  并按约定枚举校验写入 `pickup_record` 的取件方式与核验方式。
- 核销成功后在主事务内自动写入一条 `notify_type = PICKUP_DONE`、`channel = APP` 的取件确认通知
  （通知写入失败不影响核销结果，详见第 10 节）。

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
  "overdueDayCount": 0,
  "overdueFee": 0,
  "operatorId": 2,
  "operatorName": "李思远",
  "remark": "工作日 18:00 后自取",
  "createTime": "2026-10-07 17:39:47"
}
```

其中 `stationName`、`shelfCode`、`operatorName`、`statusName`、`parcelTypeName`、`storageDays`、
`overdueDayCount`、`overdueFee` 均为服务端派生填充字段（非数据库字段）：`storageDays` 为已保管天数，
`overdueDayCount` 为已超过免费保管期的天数（未逾期为 0），`overdueFee` 按 2 元/天计算，
已取件快件返回实收的 `storageFee`。

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

### 3.13 逾期未取快件分页查询（v1.1 新增）

`GET /api/parcels/overdue/page?pageNum=1&pageSize=10&stationId=1&minDays=1`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| ---- | ---- | ---- | ------ | ---- |
| `pageNum` | long | 否 | 1 | 页码 |
| `pageSize` | long | 否 | 10 | 每页条数 |
| `stationId` | long | 否 | — | 驿站ID；不传且当前用户为员工时强制取本人所属驿站 |
| `minDays` | Integer | 否 | 1 | 至少逾期天数，小于 1 时按 1 处理 |

权限：`parcel:overdue`。响应 `data` 为分页结构，`list` 元素为 3.4 节的快件对象。

判定口径（与 `ParcelMapper.xml` 一致）：`status = 'IN_STORE'` 且
`DATEDIFF(NOW(), in_time) - overdue_days >= minDays`，即「已保管天数超过免费保管天数，且超出部分达到 minDays 天」；
结果按 `DATEDIFF(NOW(), in_time) - overdue_days` 倒序、`in_time` 升序排列。
`minDays` 用于「逾期 1 天 / 3 天 / 7 天以上」的分层催取。

数据权限：ADMIN 可指定或查看全部驿站；STAFF 强制收敛为本驿站；其他角色强制收敛为本人手机号名下的快件。

### 3.14 逾期未取件数量（v1.1 新增）

`GET /api/parcels/overdue/count?stationId=1` → `data` 为整数，供首页与侧边栏角标使用。

权限：`parcel:list`。统计口径与 3.13 完全一致（**必须使用同一判定式，避免角标数字与列表条数不一致**）：
`status = 'IN_STORE'` 且 `DATEDIFF(NOW(), in_time) - overdue_days >= 1`，即按自然日计算已保管天数，
超出免费保管天数 1 天及以上才算逾期。"恰好用满免费保管天数"的快件不计入逾期，与费用试算口径一致。

数据范围（v1.1 已与 3.13 分页接口统一）：管理员按传入的 `stationId` 统计（为空表示全部驿站）；
**驿站员工强制收敛为本驿站**；**普通用户强制收敛为本人手机号名下的快件**
（`ParcelService.countOverdue` 内部按角色决定传 `stationId` 还是 `limitPhone`），
因此不会出现"角标数字比列表里能看到的还多"的情况。

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

### 6.6 数据大屏聚合数据（v1.1 新增）

`GET /api/stats/screen?stationId=1` → `data` 为 `ScreenVO`：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `overview` | Object | 同 6.1 的概览结构 |
| `trend` | Object | 近 **14** 日出入库趋势，同 6.2 结构 |
| `company` | Array | 快递公司分布，同 6.3 结构 |
| `parcelType` | Array | 快件类型分布（`name` 为中文类型名） |
| `stationRank` | Array | 驿站业务量排行，**仅 ADMIN 有数据，员工返回空数组** |
| `recentInStore` | Array | 最近入库快件，最多 8 条（快件对象） |
| `recentPickup` | Array | 最近取件快件，最多 8 条（快件对象） |
| `topOverdue` | Array | 逾期最久的快件，最多 8 条（快件对象） |
| `notifyToday` | Integer | 今日通知发送条数 |
| `serverTime` | String | 服务器时间，大屏右上角显示 |

权限：`screen`。数据范围收敛规则与其它统计接口一致（ADMIN 可指定驿站或查看全部，员工固定为本驿站）；
大屏前端页面用**一次请求**取回全部指标，避免首屏并发发起 7 个请求。

响应示例（节选）：

```json
{
  "overview": { "todayInCount": 3, "inStoreCount": 15, "overdueCount": 4, "shelfUsage": { "total": 320, "used": 15, "rate": 4.7 } },
  "trend": { "dates": ["10-01", "10-02"], "inCounts": [3, 5], "pickupCounts": [1, 4] },
  "company": [ { "name": "顺丰速运", "value": 8 } ],
  "parcelType": [ { "name": "普通件", "value": 12 } ],
  "stationRank": [ { "stationName": "幸福小区快递驿站", "parcelCount": 15, "pickupCount": 3 } ],
  "recentInStore": [ { "id": 24, "waybillNo": "ZT7788990011310", "pickupCode": "20053456", "statusName": "在库待取" } ],
  "recentPickup": [ { "id": 21, "waybillNo": "EMS8899001122400", "statusName": "已取件" } ],
  "topOverdue": [ { "id": 8, "waybillNo": "JT5566778899001", "overdueDayCount": 3 } ],
  "notifyToday": 6,
  "serverTime": "2026-10-07 19:20:31"
}
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
| GET | `/api/shelves/map?stationId=1` | 货位地图（v1.1 新增） | `shelfmap:view` |
| PUT | `/api/shelves/recalculate?stationId=1` | 重算货位占用（v1.1 新增） | `system:shelf:edit` |

货位对象：
```json
{ "id": 1, "stationId": 1, "stationName": "幸福小区快递驿站", "shelfCode": "A-01-01", "area": "A", "capacity": 40, "usedCount": 2, "freeCount": 38, "status": 1 }
```

#### 7.5.1 货位地图（v1.1 新增）

`GET /api/shelves/map?stationId=1` → `data` 为 `ShelfMapVO` 数组：

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `shelfId` | Long | 库位ID |
| `shelfCode` | String | 库位编号，如 `A-01-01` |
| `area` | String | 库区，如 `A`/`B`/`C` |
| `capacity` | Integer | 库位容量 |
| `usedCount` | Integer | 已占用数量 |
| `freeCount` | Integer | 剩余容量，`max(capacity - usedCount, 0)` |
| `rate` | Number | 占用率（百分比，保留 1 位小数） |
| `level` | String | 占用程度：`EMPTY` 空 / `NORMAL` 正常 / `BUSY` 占用率 ≥ 80% / `FULL` 已满 |
| `status` | Integer | 库位状态：1 可用 0 停用 |
| `parcels` | Array | 该库位上的快件清单（快件对象数组），复用 3.4 结构 |

`level` 与 `rate` 的对应关系（`ShelfMapVO.computeLevel`）：容量 ≤ 0 或已用 ≤ 0 为 `EMPTY`；
已用 ≥ 容量为 `FULL`；占用率 ≥ 0.8 为 `BUSY`；其余为 `NORMAL`。
`parcels` 只包含仍占用货位的快件（`IN_STORE` / `DELIVERING` / `EXCEPTION`）。

数据范围（v1.1 已在服务层强制收敛）：管理员可按传入的 `stationId` 查询（为空表示全部驿站）；
**驿站员工一律被强制收敛到本人所属驿站**，即使显式传入其他驿站 ID 也只返回本驿站数据。
收敛逻辑见 `ShelfService.resolveStationScope`，配合前端对员工锁定驿站的操作，形成前后端双重限制。

#### 7.5.2 重算货位占用（v1.1 新增）

`PUT /api/shelves/recalculate?stationId=1` → `data` 为受影响（更新）的货位行数，用于修复异常中断造成的历史不一致。

占用口径（v1.1 统一口径，与 `ParcelService.isShelfOccupied` 一致）：快件实体是否仍在货架上，
即 `IN_STORE`、`DELIVERING`、`EXCEPTION` 占用货位，`PICKED_UP`、`RETURNED` 不占用；重算时排除逻辑删除的快件。
数据范围与 7.5.1 相同：管理员可按传入的 `stationId` 重算（为空表示全部驿站），
**驿站员工被强制收敛到本人所属驿站**（见 `ShelfService.resolveStationScope`）；
本接口还需 `system:shelf:edit` 权限（当前仅 ADMIN 拥有，故员工实际无法调用）。

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
| 逾期催取 | `/parcel/overdue` | ADMIN / STAFF | `parcel:overdue` |
| 通知记录 | `/notify/list` | ADMIN / STAFF | `notify:list` |
| 货位地图 | `/shelf-map` | ADMIN / STAFF | `shelfmap:view` |
| 数据大屏 | `/screen` | ADMIN / STAFF（全屏，脱离主布局） | `screen` |
| 取件码自助查询 | `/query` | **任何人（免登录）** | 无需权限 |
| 个人中心 | `/profile` | 全部 | `profile` |

页面总数（含 `/403`、`/404`，不含 `/login`、`/register`）由 v1.0 的 14 个增加到 **19 个**。
其中 `/screen` 与 `/query` 为顶级路由、脱离主布局全屏展示；`/query` 通过路由 `meta.public = true`
进入免登录白名单，未登录用户也可直接访问。

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
| 快件状态不允许取件 | 1001 | 该快件当前状态为「已取件」，不可取件 |
| 非法取件方式 | 1001 | 非法的取件方式：DOOR |
| 非法核验方式 | 1001 | 非法的核验方式：FACE |
| 手机号格式错误（公开查询） | 1001 | 请输入正确的 11 位手机号 |
| 通知类型非法 | 1001 | 非法的通知类型：xxx |
| 通知渠道非法 | 1001 | 非法的通知渠道：xxx |
| 通知的快件不存在 | 1001 | 快件不存在 |
| 货位已满 | 1001 | 该货位已满，请选择其他货位 |
| 货位容量小于已占用 | 1001 | 库位容量不能小于当前已占用的 N 件 |
| 货位仍有快件 | 1001 | 该货位仍存放有快件，请先转移后再删除 |
| 非员工角色查询通知记录 | 1001 | 无操作权限，请联系管理员 |
| 普通用户调用通知记录接口 | 403 | 无操作权限，请联系管理员 |
| Token 过期 | 401 | 登录已过期，请重新登录 |
| 无权限 | 403 | 无操作权限，请联系管理员 |

---

## 10. 通知模块 `/notifications`（v1.1 新增）

通知记录表 `notify_record` 记录每一条到件通知、逾期催取、取件确认与异常通知，
既是业务凭证也是纠纷追溯依据。本模块对应后端 `NotifyController`。

### 10.1 分页查询通知记录

`GET /api/notifications/page?pageNum=1&pageSize=10&waybillNo=&receiverPhone=&notifyType=&channel=&sendStatus=&stationId=&startTime=2026-10-01&endTime=2026-10-07`

| 参数 | 类型 | 必填 | 默认值 | 匹配方式 |
| ---- | ---- | ---- | ------ | -------- |
| `pageNum` | long | 否 | 1 | 页码 |
| `pageSize` | long | 否 | 10 | 每页条数 |
| `waybillNo` | String | 否 | — | 模糊匹配 `waybill_no` |
| `receiverPhone` | String | 否 | — | 模糊匹配 `receiver_phone` |
| `notifyType` | String | 否 | — | 精确匹配 `notify_type` |
| `channel` | String | 否 | — | 精确匹配 `channel` |
| `sendStatus` | String | 否 | — | 精确匹配 `send_status` |
| `stationId` | Long | 否 | — | 精确匹配 `station_id` |
| `startTime` | Date | 否 | — | `send_time >= startTime`（`yyyy-MM-dd`） |
| `endTime` | Date | 否 | — | `send_time < endTime + 1 天`（含当天） |

权限：`notify:list`。结果按 `send_time DESC, id DESC` 排序，返回 1.2 节的分页结构。

数据权限：ADMIN 不限；STAFF 在未传 `stationId` 时被强制为本驿站；既非 ADMIN 又无 STAFF 角色时，
服务层抛出 `1001 无操作权限，请联系管理员`。需要注意调用顺序：`USER` 角色不具备 `notify:list` 权限，
会先在权限切面被拦截并返回 `403 无操作权限，请联系管理员`，只有在拥有 `notify:list` 但既非 ADMIN 又非 STAFF 的
自定义角色下才会命中服务层的 `1001` 提示。

`data.list` 元素为通知记录对象（见 10.2）。

### 10.2 查询某快件的全部通知记录

`GET /api/notifications/parcel/{parcelId}` → `data` 为该快件的通知记录数组，按 `send_time DESC, id DESC` 排序。

权限：`@RequiresPermission({"notify:list", "parcel:list"})`，注解默认按 **AND** 语义校验，
即调用者需**同时**具备 `notify:list` 与 `parcel:list`（ADMIN 与 STAFF 均同时具备这两项权限）。

通知记录对象（`NotifyRecord`）：

```json
{
  "id": 1,
  "parcelId": 1,
  "waybillNo": "SF1234567890123",
  "pickupCode": "10012034",
  "notifyType": "IN_STORE",
  "notifyTypeName": "到件通知",
  "channel": "SMS",
  "channelName": "短信",
  "receiverPhone": "13900000001",
  "content": "【快件驿站】您的快件（顺丰速运 SF1234567890123）已到达幸福小区快递驿站，取件码 10012034，请凭取件码及时取件。",
  "sendStatus": "SUCCESS",
  "sendStatusName": "发送成功",
  "failReason": null,
  "stationId": 1,
  "stationName": "幸福小区快递驿站",
  "receiverName": "王小明",
  "operatorId": null,
  "operatorName": null,
  "sendTime": "2026-10-07 17:39:47"
}
```

其中 `notifyTypeName`、`channelName`、`sendStatusName` 由服务端按字典翻译，
`receiverName`、`stationName` 由关联 `parcel`、`station` 表带出，均为非数据库字段。
自动发送的记录 `operatorId` 为 `null`、`operatorName` 为「系统自动发送」。

### 10.3 发送通知

`POST /api/notifications`

```json
{
  "parcelId": 1,
  "notifyType": "IN_STORE",
  "channel": "SMS",
  "content": "您的快件已到站，请尽快取件。",
  "receiverPhone": "13900000001"
}
```

| 字段 | 必填 | 说明 |
| ---- | ---- | ---- |
| `parcelId` | 是 | 快件ID，为空报 `400 快件ID不能为空`；快件不存在报 `1001 快件不存在` |
| `notifyType` | 是 | 通知类型，为空报 `400 通知类型不能为空`；不在枚举内报 `1001 非法的通知类型：xxx` |
| `channel` | 否 | 通知渠道，默认 `SMS`；不在枚举内报 `1001 非法的通知渠道：xxx` |
| `content` | 否 | 自定义通知内容；留空由后端按类型自动生成模板文案 |
| `receiverPhone` | 否 | 接收手机号；留空取该快件的 `receiverPhone` |

权限：`notify:send`。成功响应 `message` 为「通知已发送」，`data` 为新建的通知记录对象。

自动生成的文案模板（`NotifyService.buildContent`）：

| 通知类型 | 文案 |
| -------- | ---- |
| `IN_STORE` | 【快件驿站】您的快件（{快递公司} {运单号}）已到达{驿站名称}，取件码 {取件码}，请凭取件码及时取件。 |
| `OVERDUE` | 【快件驿站】您的快件（{运单号}）已超过免费保管期 {逾期天数} 天，逾期保管费 2 元/天，请尽快凭取件码 {取件码} 到{驿站名称}取件。 |
| `PICKUP_DONE` | 【快件驿站】您的快件（{运单号}）已于 {yyyy-MM-dd HH:mm} 完成取件，感谢使用。 |
| `EXCEPTION` | 【快件驿站】您的快件（{运单号}）在驿站出现异常情况，请及时联系{驿站名称}（{驿站联系电话}）。 |

发送结果模拟规则（`NotifyService.dispatch`，即「对接真实短信网关的位置」）：
手机号为空或不匹配 `^1[3-9]\d{9}$` 时写入 `sendStatus = FAILED` 与
`failReason = 接收号码不合法或为空，短信网关拒绝发送`；否则写入 `sendStatus = SUCCESS`。

### 10.4 批量催取逾期件

`POST /api/notifications/batch-overdue?stationId=1&minDays=1`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| ---- | ---- | ---- | ------ | ---- |
| `stationId` | Long | 否 | — | 驿站ID；为空且当前用户非 ADMIN 时取本人所属驿站 |
| `minDays` | Integer | 否 | 1 | 至少逾期天数，小于 1 时按 1 处理 |

权限：`notify:send`。响应 `message` 为「本次共发送 N 条催取通知」，`data` 为**实际发送条数**（整数）。

规则：
- 候选快件与 3.13 节同口径（`status = 'IN_STORE'` 且逾期天数达到 `minDays`），单次最多处理 **50** 件；
- 通知类型固定为 `OVERDUE`、渠道固定为 `SMS`，接收手机号取快件的 `receiverPhone`；
- **同一快件当天已发送过 `OVERDUE` 通知的会被跳过**（`countTodayByParcelAndType`），因此当天重复调用不会重复骚扰客户，
  返回值也随之减少。

### 10.5 待催取的逾期件数量

`GET /api/notifications/overdue-pending?stationId=1` → `data` 为整数，供页面角标提示。

权限：`notify:list`。统计口径为「至少逾期 1 天」的在库快件数量，
实现上取 `selectOverdueList(stationId, 1, 50)` 的条数，因此**返回值上限为 50**。

---

## 11. 公开接口 `/public`（v1.1 新增，免登录）

本模块对应后端 `PublicController`（`@RequestMapping("/api/public")`），
其路径已在 `WebMvcConfig` 中排除 JWT 登录拦截，**调用时无需登录、也不要携带敏感信息**。

### 11.1 按手机号自助查询取件码

`GET /api/public/pickup-query?phone=13900000001`

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| `phone` | String | 是 | 收件人手机号，需匹配 `^1[3-9]\d{9}$`，否则报 `1001 请输入正确的 11 位手机号` |

响应 `message` 为「共查询到 N 件待取快件」，`data` 为快件对象数组。查询与脱敏规则：

1. 只返回 **`IN_STORE`（在库待取）与 `DELIVERING`（派送中）** 的快件，已取件历史与已退回件不返回；
2. 单次最多返回 **20** 条，按 `in_time DESC` 排序；
3. 返回前由 `PublicService` 主动把 `stationId`、`shelfId`、`operatorId`、`freight`、`remark`
   五个字段置为 `null`，避免通过手机号探测到驿站内部标识、货位、运费与备注等敏感信息。

安全设计说明：即使他人知道了某个手机号，也只能看到「当前确实需要来取」的快件，
无法据此探测该号码的历史收件记录；接口也不需要登录态，因此仅暴露取件必要字段。

响应示例：

```json
{
  "code": 200,
  "message": "共查询到 1 件待取快件",
  "data": [
    {
      "id": 1,
      "waybillNo": "SF1234567890123",
      "stationId": null,
      "stationName": "幸福小区快递驿站",
      "shelfId": null,
      "shelfCode": "A-01-01",
      "expressCompany": "顺丰速运",
      "parcelType": "NORMAL",
      "parcelTypeName": "普通件",
      "receiverName": "王小明",
      "receiverPhone": "13900000001",
      "pickupCode": "10012034",
      "weight": 1.20,
      "freight": null,
      "status": "IN_STORE",
      "statusName": "在库待取",
      "inTime": "2026-10-07 17:39:47",
      "pickupTime": null,
      "overdueDays": 3,
      "storageFee": 0.00,
      "operatorId": null,
      "operatorName": "李思远",
      "remark": null,
      "storageDays": 0,
      "overdueFee": 0.00
    }
  ]
}
```

字段说明：
- `overdueDays` 在公开接口中被**复用为「还可免费保管天数」**，取 `max(免费保管天数 - 已保管天数, 0)`；
- `overdueFee` 为按 2 元/天估算的应缴保管费，`storageDays` 为已保管天数；
- 该接口不返回 `overdueDayCount`（未调用 `ParcelService.fillDisplayFields`）。

---

## 12. 枚举字典汇总

以下字典与后端 `common/enums` 包及前端 `src/utils/dict.js` 严格一致，任何调整必须同步三处。

### 12.1 快件状态 `PARCEL_STATUS`

| 枚举值 | 中文名 |
| ------ | ------ |
| `IN_STORE` | 在库待取 |
| `PICKED_UP` | 已取件 |
| `DELIVERING` | 派送中 |
| `EXCEPTION` | 异常件 |
| `RETURNED` | 已退回 |

### 12.2 快件类型 `PARCEL_TYPE`

`NORMAL` 普通件、`SMALL` 小件、`LARGE` 大件、`FRAGILE` 易碎品、`DOCUMENT` 文件、`COLD` 生鲜。

### 12.3 取件方式 `pickupType`（v1.1 修正）

| 枚举值 | 中文名 |
| ------ | ------ |
| `SELF` | 本人自取 |
| `AGENT` | 他人代取 |
| `DELIVERY` | 送货上门 |

> **v1.1 修正说明**：送货上门的正确枚举值为 `DELIVERY`，早期文档与前端曾误写为 `DOOR`，
> 会导致按字典渲染时匹配不到后端返回值。后端 `ParcelService.pickup` 现已校验该枚举，
> 传入非法值返回 `1001 非法的取件方式：xxx`。

### 12.4 核验方式 `verifyType`

`CODE` 取件码核验、`ID_CARD` 身份证核验、`PHONE` 手机号核验。后端同样做枚举校验，
非法值返回 `1001 非法的核验方式：xxx`。

### 12.5 通知类型 `NOTIFY_TYPE`（v1.1 新增）

| 枚举值 | 中文名 | 含义 |
| ------ | ------ | ---- |
| `IN_STORE` | 到件通知 | 快件入库后告知收件人取件码 |
| `OVERDUE` | 逾期催取 | 超过免费保管期后提醒尽快取件 |
| `PICKUP_DONE` | 取件确认 | 核销完成后告知已取件 |
| `EXCEPTION` | 异常通知 | 出现破损、地址不详等问题时告知 |

### 12.6 通知渠道 `NOTIFY_CHANNEL`（v1.1 新增）

| 枚举值 | 中文名 |
| ------ | ------ |
| `SMS` | 短信 |
| `APP` | 站内通知 |
| `PHONE` | 电话 |

### 12.7 发送结果 `SEND_STATUS`（v1.1 新增）

| 枚举值 | 中文名 |
| ------ | ------ |
| `SUCCESS` | 发送成功 |
| `FAILED` | 发送失败 |

### 12.8 货位占用程度 `SHELF_LEVEL`（v1.1 新增）

| 枚举值 | 中文名 | 判定条件（`ShelfMapVO.computeLevel`） |
| ------ | ------ | ------------------------------------ |
| `EMPTY` | 空闲 | 容量 ≤ 0 或 已用 ≤ 0 |
| `NORMAL` | 正常 | 占用率 < 80% 且未满 |
| `BUSY` | 较满 | 占用率 ≥ 80% 且未满 |
| `FULL` | 已满 | 已用 ≥ 容量 |

### 12.9 货位占用口径（v1.1 统一）

| 快件状态 | 是否占用货位 |
| -------- | ------------ |
| `IN_STORE` 在库待取 | 占用 |
| `DELIVERING` 派送中 | 占用 |
| `EXCEPTION` 异常件 | 占用 |
| `PICKED_UP` 已取件 | 不占用 |
| `RETURNED` 已退回 | 不占用 |

该口径由 `ParcelService.isShelfOccupied` 统一定义，被取件核销、删除快件、异常件退回、
货位重算与货位地图 `parcels` 字段复用，前后端口径一致。
