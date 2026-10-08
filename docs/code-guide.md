# 源码导读

> 面向答辩与代码自查：**先看什么、每个文件干什么、核心逻辑在哪一行**。
> 项目名：面向快递驿站的快件收发管理系统 · 作者：吴宇航

---

## 一、三种打开方式

### 方式 1：浏览器直接看（什么都不用装，推荐给导师看）

打开 <https://github.com/WUYUHANG1949/express-station-management-system>

| 想看 | 点这个路径 |
| ---- | ---------- |
| 后端 Java 代码 | `backend/src/main/java/com/wuyuhang/delivery/` |
| 数据库建表脚本 | `sql/01_schema.sql` |
| 前端 Vue 代码 | `frontend/src/views/` |
| 需求 / 设计 / 测试文档 | `docs/` |
| 系统运行截图 | `docs/screenshots/` |

### 方式 2：本地文件夹（最快，直接翻文件）

```
D:\wuyuhang-delivery-system
```

### 方式 3：用 IDEA 打开（要改代码 / 调试时）

1. 打开 IntelliJ IDEA → `File` → `Open`
2. 选择 `D:\wuyuhang-delivery-system\backend`（这是 Maven 工程根目录，认 `pom.xml`）
3. 等待右下角 Maven 依赖下载完成（首次约 1–3 分钟）
4. 找到 `src/main/java/com/wuyuhang/delivery/DeliveryApplication.java`，点绿色三角即可启动

前端用 IDEA 或 VS Code 打开 `D:\wuyuhang-delivery-system\frontend` 目录即可。

---

## 二、项目总体结构

```
D:\wuyuhang-delivery-system\
├── backend/          Spring Boot 后端（109 个 Java 文件）
├── frontend/         Vue 3 前端（19 个路由页面）
├── sql/              01_schema.sql 建表 + 02_data.sql 示范数据
├── docs/             5 份文档 + 13 张运行截图
├── scripts/          一键启动脚本 + 接口冒烟测试脚本
└── README.md         项目总说明（先读这个）
```

---

## 三、后端代码地图（`backend/src/main/java/com/wuyuhang/delivery/`）

| 包 | 文件数 | 职责 | 答辩时的说法 |
| -- | -----: | ---- | ------------ |
| `common` | 16 | 统一响应体、全局异常处理、状态枚举、工具类 | 统一返回格式，前端只需判断 `code==200` |
| `config` | 4 | MyBatis-Plus、WebMvc（拦截器/跨域）、Jackson、通用 Bean | 集中配置，业务代码零配置污染 |
| `security` | 6 | JWT 工具、登录拦截器、权限注解与切面、用户上下文 | **认证与鉴权分层**（详见下文第五点） |
| `entity` | 13 | 13 张表的实体类 | 与数据库表一一对应 |
| `mapper` | 14 | Mapper 接口（复杂 SQL 在 `resources/mapper/*.xml`） | 简单查询用注解，复杂关联查询用 XML |
| `dto` | 21 | 请求参数对象（含 `query` 子包） | 用 `@Valid` 做参数校验 |
| `vo` | 11 | 响应视图对象 | 只返回前端需要的字段 |
| `service` | 12 | 业务逻辑（**核心在这里**） | 事务、业务规则、数据权限都在这一层 |
| `controller` | 11 | REST 接口 | 只做参数接收与结果包装，不写业务 |

### 按重要性排序的核心文件

| 文件 | 行数 | 为什么重要 |
| ---- | ---: | ---------- |
| `service/ParcelService.java` | 555 | **全系统最核心**：收件登记、取件核销、货位一致性 |
| `service/NotifyService.java` | ~280 | 通知与催取，含真实短信网关的对接位置 |
| `service/AuthService.java` | 193 | 登录、注册、JWT 签发、菜单树组装 |
| `security/JwtInterceptor.java` | 52 | 认证：解析令牌并写入 ThreadLocal |
| `security/PermissionAspect.java` | 58 | 鉴权：`@RequiresPermission` 注解切面 |
| `common/GlobalExceptionHandler.java` | ~140 | 7 类异常统一处理，前端永远收到同一结构 |
| `controller/ParcelController.java` | 159 | 快件模块全部接口清单（看接口先看它） |

---

## 四、推荐阅读顺序（按这个顺序看，20 分钟能摸清全貌）

### 第 1 步：跑起来看效果（5 分钟）

先读 `README.md` 的「快速开始」，把系统跑起来点一遍，知道每个功能长什么样。
然后再看代码，会快很多。

### 第 2 步：看两个"入口"文件（5 分钟）

1. `backend/src/main/java/com/wuyuhang/delivery/DeliveryApplication.java`（27 行）
   —— 启动类，注解 `@SpringBootApplication` + `@MapperScan`
2. `backend/src/main/resources/application.yml`（86 行）
   —— 端口、数据库、MyBatis-Plus、JWT 配置；注意数据库密码走 `application-dev.yml`，**不进版本库**

### 第 3 步：看数据库（10 分钟）

1. `sql/01_schema.sql` —— 13 张表的建表语句，**每张表都有中文注释**
   - 5 张系统权限表：`sys_user`、`sys_role`、`sys_user_role`、`sys_permission`、`sys_role_permission`
   - 8 张业务表：`station`、`shelf`、`parcel`、`parcel_trace`、`pickup_record`、`ship_order`、`exception_record`、`notify_record`
2. `docs/database-design.md` —— ER 图 + 每个字段的设计理由

### 第 4 步：看核心业务（15 分钟，最重要）

打开 `service/ParcelService.java`，按行号定位：

| 行号 | 方法 | 干什么 |
| ---- | ---- | ------ |
| 87 | `inStore(...)` | **收件登记**：校验运单号 → 查重 → 分配货位 → 占用货位 → 生成取件码 → 写轨迹 → 自动发通知 |
| 166 | `pickup(...)` | **取件核销**：取件码+运单号双重匹配 → 状态校验 → 算逾期费 → 改状态 → **释放货位** → 写取件记录 → 写轨迹 → 自动发通知 |
| 262 | `page(...)` | 条件组合查询，含数据权限过滤 |
| **474** | **`isShelfOccupied(...)`** | **全系统货位口径的唯一出处**（答辩重点，见下文第六点） |
| 483 | `resolveShelfId(...)` | 指定货位时校验归属与容量，未指定时自动挑剩余容量最大的 |
| 542 | `calculateFee(...)` | 逾期保管费 = 超出免费保管期的天数 × 2 元/天 |

### 第 5 步：看认证与鉴权（10 分钟）

| 文件 | 看什么 |
| ---- | ------ |
| `service/AuthService.java` | `login()`：BCrypt 校验 → 查角色与权限 → 签发 JWT → 更新登录时间 |
| `security/JwtInterceptor.java` | `preHandle()`：取 `Authorization` 头 → 解析令牌 → 写入 `UserContext`；`afterCompletion()` 清理 ThreadLocal |
| `security/PermissionAspect.java` | `checkPermission()`：读注解要求的权限 → 与用户权限集合比对 → 不满足抛 403 |
| `security/RequiresPermission.java` | 自定义注解，支持 `AND`/`OR` 两种逻辑 |

### 第 6 步：看前端（15 分钟）

| 文件 | 看什么 |
| ---- | ------ |
| `frontend/src/main.js` | 入口：注册 Element Plus、Pinia、路由、权限指令、主题样式 |
| `frontend/src/utils/request.js` | axios 封装：请求拦截注入 JWT、响应拦截统一解包、401 跳登录 |
| `frontend/src/router/index.js` | 19 个路由 + 全局守卫（登录态 → 权限 → 白名单）+ chunk 加载失败自愈 |
| `frontend/src/layout/index.vue` | 主布局：侧边栏动态菜单、面包屑、逾期提醒、**渲染异常兜底** |
| `frontend/src/views/parcel/pickup.vue` | **核心作业页**：扫码查询 → 选中 → 核销，注释最全 |
| `frontend/src/styles/theme.css` | 全站主题：**改配色只改这一个文件** |

---

## 五、三条必须能讲清楚的链路

### 链路 1：登录 → 拿到权限 → 访问受保护接口

```
前端 login/index.vue 提交表单
  → POST /api/auth/login
  → AuthController.login()
  → AuthService.login()
      ├─ userMapper.selectByUsername()          查用户
      ├─ passwordEncoder.matches()              BCrypt 校验（数据库存的是密文）
      ├─ userMapper.selectRoleCodesByUserId()   查角色
      ├─ permissionMapper.selectPermCodesByUserId()  查权限（43 项）
      └─ jwtUtil.generateToken()                签发 JWT（角色与权限写进载荷）
  ← 返回 { token, tokenType, expiresIn, userInfo }

之后前端每个请求带 Authorization: Bearer <token>
  → JwtInterceptor.preHandle()   解析令牌 → 写入 UserContext（ThreadLocal）
  → PermissionAspect.checkPermission()  比对注解要求的权限 → 不够则 403
  → Controller → Service 执行业务
  → JwtInterceptor.afterCompletion()  清理 ThreadLocal（防止线程复用串号）
```

### 链路 2：收件登记（入库）

```
POST /api/parcels/in-store
→ ParcelService.inStore()  @Transactional  ← 关键：整个方法在一个事务里
   1. ExpressCompanyUtil.isValid()   按快递公司正则校验运单号
   2. 运单号查重（数据库还有唯一索引兜底）
   3. 确定驿站（员工默认本驿站）
   4. resolveShelfId()  指定则校验归属/容量，未指定则 selectBestAvailable()
   5. shelfMapper.occupy()  ← 带容量条件的 UPDATE，返回 0 说明货位满
   6. 循环生成不重复的 8 位取件码
   7. 插入 parcel 记录
   8. 写 parcel_trace 轨迹
   9. notifyService.autoSend() 自动发到件通知（失败不影响入库）
```

### 链路 3：取件核销（出库）—— 本项目最关键的一段

```
POST /api/parcels/pickup
→ ParcelService.pickup()  @Transactional
   1. 按运单号查快件，不存在直接报错
   2. 取件码必须与运单号匹配（防止拿错件）
   3. 状态校验：只允许 IN_STORE（在库）或 DELIVERING（派送中）
   4. 校验 pickupType / verifyType 是否在约定枚举内
   5. 计算应缴保管费（超出免费天数 × 2 元/天）
   6. 更新快件状态为 PICKED_UP、写 pickup_time、写 storage_fee
   7. shelfMapper.release()  ← 释放货位，与第 6 步同事务
   8. 插入 pickup_record 取件凭证
   9. 写 parcel_trace 轨迹
  10. notifyService.autoSend() 自动发取件确认通知
```

> **为什么第 6、7 步必须在同一事务**：如果释放货位失败而状态已改成"已取件"，
> 就会出现"货位显示空闲但快件还在架上"（或反之）的库存不一致。
> 这是本项目重点解决的问题，详见 `docs/thesis-outline.md` 答辩问题 3。

---

## 六、答辩时最能体现深度的三个设计

### 1. 货位占用口径的唯一出处（`ParcelService.java:474`）

```java
public static boolean isShelfOccupied(String status) {
    return IN_STORE.equals(status)        // 在库待取：件在架上
        || DELIVERING.equals(status)      // 派送中：货位仍为其保留
        || EXCEPTION.equals(status);      // 异常件：件还在驿站
}
```

**口径**：以"快件实体是否仍在货架上"为唯一标准。
取件核销、删除快件、异常件退回、货位重算**全部复用这个方法**，避免各处判定不一致。
另提供 `PUT /api/shelves/recalculate` 一键按此口径重算历史数据。

### 2. 认证与鉴权分层

- **认证**（你是谁）：`JwtInterceptor` —— 解析令牌，写入 `UserContext`
- **鉴权**（你能不能做）：`@RequiresPermission` 注解 + `PermissionAspect` 切面

好处：Controller 与 Service 里**看不到任何 if 判断权限的代码**，权限规则集中在注解上。

### 3. 数据权限在服务层强制收敛（不只靠前端）

| 角色 | 可见范围 | 实现位置 |
| ---- | -------- | -------- |
| ADMIN | 全部驿站 | `ParcelService.applyDataScope` |
| STAFF | 强制本驿站 | `ShelfService.resolveStationScope`、`NotifyService` 各方法 |
| USER | 仅本人手机号名下 | `ParcelService.countOverdue`、`applyDataScope` |

前端也做了限制，但**后端会覆盖前端传来的 `stationId`**，所以伪造参数也拿不到别的驿站数据。

---

## 七、"我要改某个东西，该动哪个文件"

| 想做什么 | 改哪个文件 |
| -------- | ---------- |
| 改全站配色 | `frontend/src/styles/theme.css`（改 `:root` 里的变量） |
| 改逾期费单价（现在 2 元/天） | `ParcelService.java` 的 `OVERDUE_FEE_PER_DAY` |
| 改免费保管天数（现在 3 天） | `ParcelService.java` 的 `DEFAULT_FREE_DAYS` |
| 增删快递公司 / 改运单号正则 | `common/util/ExpressCompanyUtil.java` |
| 加菜单 / 加按钮权限 | `sql/02_data.sql` 的 `sys_permission` 插入段 |
| 改侧边栏样式 | `frontend/src/layout/index.vue` |
| 改接口返回格式 | `common/Result.java`（改一处，全局生效） |
| 加新接口 | 照着 `controller/ParcelController.java` 加方法 + `@RequiresPermission` 注解 |
| 对接真实短信网关 | `NotifyService.java` 的 `dispatch()`（注释里已标出位置） |
| 改数据库表结构 | `sql/01_schema.sql` + 对应 `entity/` + `mapper/` |

---

## 八、常见疑问

**Q：为什么 `service` 层没有接口（Interface）而直接是类？**
A：服务层没有多实现的可能（一个领域只有一个实现），直接写具体类减少一层无意义的抽象。
如果将来要替换实现，再抽接口也不迟。

**Q：为什么复杂查询用 XML 而不是注解？**
A：快件列表要动态拼接 9 个可选条件并关联 3 张表，XML 的 `<if>` 与 `<sql>` 片段更好维护；
简单的单表查询用注解更直观。两者混用在 MyBatis 项目里很常见。

**Q：`@TableLogic` 是什么？**
A：MyBatis-Plus 的逻辑删除。`deleted = 0/1`，查询自动加 `deleted = 0`，
删除变成 UPDATE。好处是保留历史台账可追溯。注意手写 SQL 时要自己加这个条件。

**Q：数据库密码在哪？**
A：不在仓库里。本地放在 `backend/src/main/resources/application-dev.yml`（已被 `.gitignore` 忽略），
仓库里只有模板 `application-dev.yml.example`。

---

## 九、相关文档

| 文档 | 什么时候看 |
| ---- | ---------- |
| `README.md` | 第一次接触项目 |
| `docs/spec.md` | 想了解需求范围与业务流程 |
| `docs/database-design.md` | 想了解表结构设计与索引理由 |
| `docs/api-contract.md` | 想知道某个接口的参数与返回 |
| `docs/test-cases.md` | 想知道测了哪些场景 |
| `docs/thesis-outline.md` | 写论文、准备答辩 |
| 本文档 | 想知道代码在哪、怎么读 |
