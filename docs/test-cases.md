# 面向快递驿站的快件收发管理系统 系统测试用例说明书

| 项目名称 | 面向快递驿站的快件收发管理系统 |
| -------- | ------------------------------ |
| 文档名称 | 系统测试用例说明书 |
| 作者 | 吴宇航 |
| 版本 | V1.1 |
| 测试对象 | 后端服务（Spring Boot 3.2.5，8080 端口）、前端应用（Vue 3 + Vite，5173 端口） |
| 接口前缀 | `/api` |

> **V1.1 变更说明**：新增功能测试用例 TC-074 至 TC-096（通知与催取、货位可视化与数据大屏、收件人自助查询三组）
> 与接口测试用例 TC-API-047 至 TC-API-061；需求覆盖矩阵纳入 FR-26 至 FR-30；
> 基线数据核对补充 `notify_record` 的条数与构成、权限总数 43 项与角色权限分布；
> 并按 v1.1 代码行为修正取件核销状态口径、错误提示语与「派送中快件可当面签收核销」等既有用例。
>
> **本次修订（找回密码）**：新增功能测试用例 TC-101 至 TC-106 与接口测试用例 TC-API-072、TC-API-073，
> 覆盖验证码找回的正常流程、双要素防探测、一次性、错误次数上限、发送限流与通道切换；
> 基线数据核对表补充 `sms_code`（初始化后 0 行），全文用例总数更新为功能 **106** 条、接口 **73** 条、合计 **179** 条。

---

## 1 测试目的、测试环境与测试方法

### 1.1 测试目的

1. 验证系统实现与《软件需求规格说明书》及《前后端接口契约》的一致性，确认全部功能需求（FR-01 至 FR-30）均已实现且行为正确。
2. 验证两类登录角色（`ADMIN`、`STAFF`）的菜单级与按钮级权限隔离有效，不存在越权访问与越权查询；收件人不设账号、不占角色，其查件能力由免登录自助查询按入参限定。
3. 验证关键业务路径的数据一致性，重点检查收件登记、取件核销、异常件处理过程中的多表写入与货位计数是否准确。
4. 验证系统对异常输入与非法操作的容错能力，确认返回的业务码与中文提示语符合接口契约约定。
5. 为毕业设计论文第 6 章"系统测试"提供可复现的测试记录与结论依据。
6. 本轮共设计功能测试用例 **106** 条、接口测试用例 **73** 条，合计 **179** 条；其中认证模块含 TC-001 至 TC-011 与本次新增的 TC-101 至 TC-106，接口用例含 TC-API-072、TC-API-073。

### 1.2 测试环境

| 类别 | 配置项 | 说明 |
| ---- | ------ | ---- |
| 硬件环境 | 处理器 | 主频 2.0 GHz 及以上，内存 8 GB 及以上 |
| 操作系统 | 名称与版本 | Windows 11 |
| 运行环境 | Java | JDK 17（`JAVA_HOME` 已配置） |
| 数据库 | 名称与版本 | MySQL 8.0.39，数据库 `express_station`，字符集 `utf8mb4` |
| 后端 | 框架与端口 | Spring Boot 3.2.5、MyBatis-Plus 3.5.7，监听 8080 端口 |
| 前端 | 框架与端口 | Vue 3、Vite、Element Plus，开发服务 5173 端口，通过 Vite 代理转发 `/api` |
| 浏览器 | 名称与版本 | Google Chrome（100 及以上版本） |
| 接口工具 | 名称 | Postman，用于构造请求、校验响应码与响应体 |
| 单元与集成测试 | 工具 | JUnit 5、Spring Boot Test、MockMvc |
| 数据准备 | 脚本 | 依次执行 `sql/01_schema.sql`、`sql/02_data.sql` |

### 1.3 测试账号与基线数据

| 用户名 | 角色 | 姓名 | 所属驿站 | 初始密码 | 用途 |
| ------ | ---- | ---- | -------- | -------- | ---- |
| admin | ADMIN | 吴宇航 | 无（不限驿站） | 123456 | 系统管理与全量业务验证 |
| staff01 | STAFF | 李思远 | station_id = 1（幸福小区快递驿站） | 123456 | 驿站业务主流程验证 |
| staff02 | STAFF | 陈欣怡 | station_id = 2（大学城菜鸟驿站） | 123456 | 跨驿数据隔离验证 |

账号共 **3 个**（`admin`、`staff01`、`staff02`），角色共 **2 个**（`ADMIN`、`STAFF`）。**自助注册已关闭**：`POST /api/auth/register` 固定返回 `code = 1001`，因此账号数量只能由管理员在「用户管理」中变更，不会随收件人注册而增长；收件人**无需账号、不占角色**，查件走 `/query` 的免登录自助查询。原 `USER`（普通用户）角色已彻底移除，`user01`、`user02` 两个账号随之删除，故不存在以普通用户身份登录的前置条件。

基线数据要点：`parcel` 表初始 25 条记录；`parcel.id = 1`（运单号 `SF1234567890123`，取件码 `10012034`）为驿站 1 的在库快件；`parcel.id = 7`（运单号 `EMS8899001122334`，取件码 `10042319`，`in_time` 为 4 天前，`overdue_days = 3`，`storage_fee = 2.00`）为逾期在库快件；`parcel.id = 10`（运单号 `ZT7788990011240`）为已取件快件；`parcel.id = 13`（运单号 `YD1122334455670`）为派送中快件；`parcel.id = 14`（运单号 `EMS8899001122340`）为异常件；`parcel.id = 15`（运单号 `JT5566778899010`）为已退回快件；`ship_order` 表初始 8 条记录，其中 `S20260101100004`、`S20260101100008` 为 `PENDING` 状态。

初始化数据核对（v1.1 更新）：

| 数据对象 | 初始条数 | 构成说明 |
| -------- | -------- | -------- |
| `parcel` | 25 | 驿站 1 共 15 条、驿站 2 共 10 条；状态构成为在库 16 条、已取件 5 条、派送中 1 条、异常 2 条、已退回 1 条 |
| `ship_order` | 8 | 状态构成为 `PENDING` 2 条、`ACCEPTED` 2 条、`SHIPPED` 3 条、`CANCELLED` 1 条 |
| `exception_record` | 3 | 仍为 3 条：破损（`HANDLING`）、地址错误（`PENDING`）、拒收（`RESOLVED`），v1.1 未变更 |
| `notify_record` | 29 | 由 `parcel` 表推导生成，构成为：① 为仍在货架上的快件（`IN_STORE`/`DELIVERING`/`EXCEPTION`）生成 19 条 `IN_STORE` 到件通知；② 为已超过免费保管期的在库快件生成 3 条 `OVERDUE` 逾期催取通知；③ 为已取件快件生成 5 条 `PICKUP_DONE` 取件确认通知（渠道 `APP`）；④ 另有 2 条 `send_status = FAILED` 的示例记录（1 条 `OVERDUE`、1 条 `EXCEPTION`），带 `fail_reason` 用于演示失败提示与重发。按类型汇总为 `IN_STORE` 19 条、`OVERDUE` 4 条、`PICKUP_DONE` 5 条、`EXCEPTION` 1 条 |
| `sys_permission` | 43 | v1.1 由 35 项增加到 43 项（菜单类 21 条、按钮类 22 条），本轮角色收敛未改变权限项数 |
| `sys_role` | 2 | `ADMIN` 系统管理员、`STAFF` 驿站员工；原 `USER` 普通用户角色已删除 |
| `sys_user` | 3 | `admin`、`staff01`、`staff02`，密码均为 `$2a$10$` 开头的 BCrypt 密文 |
| `sys_user_role` | 3 | (1,1) admin→ADMIN、(2,2) staff01→STAFF、(3,2) staff02→STAFF；原 user01/user02 的关联已删除 |
| `sys_role_permission` | 72 | ADMIN 43 项（全部权限）+ STAFF 29 项；原 USER 的 3 项授权（`dashboard`、`parcel:list`、`profile`）已删除，总数由 75 降为 72 |
| `sms_code` | 0 | 验证码表，初始化后无记录；`02_data.sql` 已加入 `DELETE FROM sms_code;`。执行找回密码用例后会产生记录，还原基线时需清空 |

初始化后数据库共 **14 张表**（6 张认证与权限表 + 8 张业务表）。上表列出与用例直接相关的数据对象，其中 `sms_code` 为本次新增、初始 **0 行**；其余表结构与字段说明见 `docs/database-design.md`。

### 1.4 测试方法

1. **黑盒功能测试**：以需求与接口契约为依据，通过前端界面与接口工具验证功能行为，综合运用等价类划分（如手机号有效类与无效类）、边界值分析（如密码长度 6 位与 20 位、取件码位数、分页页码）、错误推测（如重复运单号、并发核销）等方法设计用例。
2. **接口测试**：使用 Postman 逐个验证接口的请求方法、路径、参数、HTTP 状态与业务 `code`，并结合 JUnit 5 与 MockMvc 编写自动化集成测试用例，覆盖认证拦截、权限校验与事务回滚场景。
3. **数据一致性测试**：在关键用例执行前后查询数据库，比对 `parcel`、`pickup_record`、`parcel_trace`、`shelf` 等表的记录数与字段值，验证事务完整性。
4. **回归测试**：缺陷修复后针对受影响模块及其关联模块执行回归用例，重点回归权限校验与核销事务相关用例。

### 1.5 通过准则

1. 功能测试用例通过率不低于 95%，核心业务用例（收件登记、取件核销、异常件处理、权限校验）通过率必须为 100%。
2. 用例执行过程中不得存在"致命"与"严重"等级的未关闭缺陷。
3. 接口返回的业务码、中文提示语与 `docs/api-contract.md` 第 9 节完全一致。
4. 关键业务操作后的数据库状态与用例预期结果一致，不出现货位计数偏差与孤立明细记录。

### 1.6 结果记录说明

下表"实际结果"列依据设计预期预填为"通过"，用于标示该用例的预期执行结论。**该列须由测试人员实际执行用例后人工复核确认，若实测与预期不符，应改为"失败"并在缺陷记录中登记缺陷编号；最终结论以实测结果为准。**

### 1.7 用例编号规则与优先级说明

1. 功能测试用例编号格式为 `TC-` 加三位序号，按模块分段连续编号，段内顺序体现业务先后关系：认证模块 TC-001 至 TC-011，权限模块 TC-012 至 TC-018，收件登记模块 TC-019 至 TC-025，取件核销与派送模块 TC-026 至 TC-037，寄件登记模块 TC-038 至 TC-043，异常件管理模块 TC-044 至 TC-050，查询与统计模块 TC-051 至 TC-063，系统管理与基础数据模块 TC-064 至 TC-073，通知与逾期催取模块 TC-074 至 TC-087，货位可视化与数据大屏模块 TC-088 至 TC-092，收件人自助查询模块 TC-093 至 TC-098，取件方式与核验方式校验 TC-099 至 TC-100（后四段为 v1.1 新增），找回密码 TC-101 至 TC-106（本次新增，共 6 条）。
2. 接口测试用例编号格式为 `TC-API-` 加三位序号，按接口所属模块分组连续编号，与功能用例分开统计，避免重复计数。
3. 优先级分为三级。高：核心业务路径与安全相关用例，必须全部通过，任一条失败即判定本轮测试不通过；中：影响使用效率或数据完整性的用例，允许在修复后回归；低：边界与异常提示类用例，可在验收前完成。
4. 每条用例的"预期结果"均包含三项可验证内容中的一项或多项：预期业务码与提示语原文、预期界面表现、预期数据库变化（表名与字段取值），以保证测试结论可复现。
5. 用例一经执行不得修改预期结果；若需求或接口契约发生变更，应先更新 `docs/api-contract.md`，再同步修订相关用例并注明修订原因。

---

## 2 功能测试用例

### 2.1 认证与账号模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-001 | 认证模块 | 使用正确账号密码登录 | 已执行初始化脚本，admin 账号 status = 1 | 1. 打开登录页；2. 输入用户名 admin、密码 123456；3. 点击登录 | HTTP 200，`code = 200`，`data.token` 非空、`tokenType = Bearer`、`expiresIn = 86400`；`data.userInfo.roles` 含 `ADMIN`，`permissions` 含 `system:user:list` 与 `parcel:export`；`sys_user.last_login_time` 更新为当前时间；跳转首页概览 | 通过 | 高 |
| TC-002 | 认证模块 | 使用错误密码登录 | admin 账号存在 | 1. 输入用户名 admin、密码 654321；2. 点击登录 | HTTP 200，`code = 1001`，`message = 用户名或密码错误`；不返回 token；停留在登录页；`sys_user.last_login_time` 不变 | 通过 | 高 |
| TC-003 | 认证模块 | 使用不存在的用户名登录 | 数据库中不存在用户名 zhangsan999 | 1. 输入用户名 zhangsan999、密码 123456；2. 点击登录 | HTTP 200，`code = 1001`，`message = 用户名或密码错误`；提示语与密码错误场景一致，不泄露账号是否存在 | 通过 | 中 |
| TC-004 | 认证模块 | 被禁用账号登录 | 将 staff02 的 `sys_user.status` 置为 0 | 1. 输入用户名 staff02、密码 123456；2. 点击登录 | HTTP 200，`code = 1001`，`message = 账号已被禁用，请联系管理员`；不返回 token；测试完成后将 status 恢复为 1 并确认 staff02 可正常登录 | 通过 | 高 |
| TC-005 | 认证模块 | 用户名或密码为空提交 | 打开登录页 | 1. 用户名与密码均留空；2. 点击登录 | 前端表单校验拦截并提示必填；若绕过前端直接请求，HTTP 200，`code = 400`，提示参数校验不通过；不产生无效登录记录 | 通过 | 中 |
| TC-006 | 认证模块 | 自助注册接口已关闭，调用被拒绝 | 已执行初始化脚本，`sys_user` 共 3 行 | 1. 用 Postman 请求 `POST /api/auth/register`，body 携带 `{"username":"tester01","password":"123456","confirmPassword":"123456","realName":"测试员","phone":"13900000099"}`；2. 再不带 body 请求一次 | 两次均 HTTP 200，`code = 1001`，`message = 自助注册已关闭：收件人无需账号，请直接使用自助查询；系统内账号请联系管理员创建`；`sys_user`、`sys_user_role` 记录数均不变（`AuthService.register()` 与 `RegisterDTO` 已删除，接口不再解析请求体） | 通过 | 高 |
| TC-007 | 认证模块 | 登录页不再提供注册入口 | 未登录状态，清除 localStorage 中的 `es_token` | 1. 打开 `/login`；2. 查看卡片底部入口；3. 点击该入口；4. 直接在地址栏访问 `/register` | 底部只显示「收件人查件？免登录自助查询 →」，**没有**「立即注册」入口；点击后进入 `/query` 免登录查询页；访问 `/register` 被重定向到 `/login`（注册页文件已删除，无独立页面） | 通过 | 高 |
| TC-008 | 认证模块 | 管理员创建账号后可正常登录 | 已用 admin 登录；`sys_user` 中不存在 tester01 | 1. 进入「用户管理」新增账号 tester01，手机号 13900000099，分配 `STAFF` 角色、所属驿站 幸福小区快递驿站；2. 退出登录；3. 用 tester01 / 123456 登录 | 新增返回 `code = 200`；`sys_user` 新增 1 行，`password` 为 `$2a$10$` 开头的 BCrypt 密文、`status = 1`、`gender = 0`；`sys_user_role` 新增 1 行关联 STAFF 角色（`role_id = 2`）；新账号可登录，侧边栏为员工菜单（与 staff01 一致，只有业务菜单、无系统管理），且数据范围为本驿站；角色下拉中只能选 `ADMIN` 或 `STAFF`，不存在 `USER` | 通过 | 高 |
| TC-009 | 认证模块 | 管理员创建账号时用户名重复被拒绝 | `admin` 账号已存在；已用 admin 登录 | 1. 在「用户管理」新增账号，用户名填 admin，其余字段合法；2. 提交 | HTTP 200，`code = 1001`，`message = 该用户名已被注册`；`sys_user` 记录数不变（由唯一索引 `uk_user_username` 保障） | 通过 | 中 |
| TC-010 | 认证模块 | 退出登录后令牌失效 | 已用 staff01 登录 | 1. 点击退出登录；2. 观察 localStorage 中 `es_token`；3. 使用退出前的 token 直接请求 `GET /api/auth/me` | 前端清除 `es_token` 并跳转登录页；后端接口返回 HTTP 200，`code = 401`，`message = 登录已过期，请重新登录` | 通过 | 中 |
| TC-011 | 认证模块 | 未携带令牌访问受保护接口 | 未登录状态 | 1. 使用 Postman 请求 `GET /api/parcels/page?pageNum=1&pageSize=10`，不带 Authorization 头 | HTTP 200，`code = 401`，`message = 登录已过期，请重新登录`；不返回任何快件数据 | 通过 | 高 |

### 2.2 权限控制模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-012 | 权限模块 | 原普通用户账号已删除，登录失败 | 已执行初始化脚本；`sys_user` 中不存在 user01（账号 3 个：admin / staff01 / staff02） | 1. 用 Postman 请求 `POST /api/auth/login`，body 为 `{"username":"user01","password":"123456"}` | HTTP 200，`code = 1001`，`message = 用户名或密码错误`；不返回 token；提示语与"用户不存在"完全一致，不泄露该账号是否曾经存在 | 通过 | 高 |
| TC-013 | 权限模块 | STAFF 角色访问用户管理接口被拒 | 已用 staff01 登录并取得 token | 1. 请求 `GET /api/users/page?pageNum=1&pageSize=10`，携带 staff01 的 token；2. 请求 `GET /api/users/stats` | 两次均 HTTP 200，`code = 403`，`message = 无操作权限，请联系管理员`；数据库无任何变更 | 通过 | 高 |
| TC-014 | 权限模块 | 角色列表只有 ADMIN 与 STAFF 两个角色 | 已用 admin 登录 | 1. 请求 `GET /api/roles/list`；2. 请求 `GET /api/roles/page?pageNum=1&pageSize=10`；3. 进入「系统管理 → 角色权限」页面；4. 请求 `GET /api/users/page?pageNum=1&pageSize=10&roleCode=USER` | 两次角色接口均 `code = 200`，列表仅含 `ADMIN` 与 `STAFF` 两条，不存在 `USER`；页面角色编码占位文案为 `ADMIN / STAFF`；第 4 步返回空列表（`total = 0`），证明系统中无 USER 角色账号 | 通过 | 高 |
| TC-015 | 权限模块 | STAFF 伪造 stationId 仍只返回本驿站 | 已用 staff01（所属驿站 1）登录；驿站 2 有快件数据 | 1. 请求 `GET /api/parcels/page?pageNum=1&pageSize=10&stationId=2`，携带 staff01 的 token；2. 请求不带 `stationId` 的同一接口；3. 用 admin 携带同样 `stationId=2` 请求一次作对照 | 第 1 次 `code = 200`，但 `data.list` 中所有记录的 `stationId` 均为 1（后端在 `ParcelService.applyDataScope` 中把 `stationId` 无条件覆盖为本驿站，前端传什么都盖掉）；第 2 次结果相同；第 3 次 admin 返回驿站 2 的数据，证明员工跨站查询被拦截 | 通过 | 高 |
| TC-016 | 权限模块 | STAFF 无删除快件按钮与权限 | 已用 staff01 登录 | 1. 进入快件查询页，观察操作列；2. 直接请求 `DELETE /api/parcels/1` | 页面不显示"删除"按钮（staff01 无 `parcel:delete` 权限）；直接调用接口返回 `code = 403`，`message = 无操作权限，请联系管理员`；`parcel.id = 1` 的 `deleted` 仍为 0 | 通过 | 高 |
| TC-017 | 权限模块 | 驿站排行统计仅 ADMIN 可访问 | 分别取得 admin 与 staff01 的 token | 1. 用 admin 请求 `GET /api/stats/station-rank`；2. 用 staff01 请求同一接口 | 第 1 次 `code = 200`，`data` 为含 `stationName`、`parcelCount`、`pickupCount` 的数组；第 2 次 `code = 403`，`message = 无操作权限，请联系管理员` | 通过 | 中 |
| TC-018 | 权限模块 | 令牌被篡改后校验失败 | 已取得 staff01 的合法 token | 1. 将 token 末位字符替换后请求 `GET /api/auth/me` | HTTP 200，`code = 401`，`message = 登录已过期，请重新登录`；前端清除 token 并跳转登录页 | 通过 | 中 |

### 2.3 收件登记模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-019 | 收件登记 | 正常登记入库并生成取件码 | 已用 staff01 登录；`SF1234567890999` 不在库中；驿站 1 的 `A-01-01`（id = 1）`used_count` 记为 N | 1. 进入收件登记页；2. 录入运单号 `SF1234567890999`、快递公司 顺丰速运、快件类型 `NORMAL`、收件人 王小明、手机号 13900000001、重量 1.5、免费保管天数 3；3. 指定货位 A-01-01；4. 提交 | HTTP 200，`code = 200`；返回对象 `pickupCode` 为 8 位纯数字、`status = IN_STORE`、`statusName = 在库待取`、`shelfCode = A-01-01`；`parcel` 表新增 1 行且 `in_time` 为当前时间；`shelf.id = 1` 的 `used_count` 变为 N+1；`parcel_trace` 新增 1 行 `operate_type = IN_STORE` 且 `operator_name = 李思远` | 通过 | 高 |
| TC-020 | 收件登记 | 运单号格式错误被拦截 | 已用 staff01 登录 | 1. 运单号输入 `abc123`（不符合快递公司运单号规则）；2. 其余字段合法；3. 提交 | HTTP 200，`code = 1001`，`message = 运单号格式不正确，请核对快递公司`；`parcel` 表记录数不变，货位 `used_count` 不变 | 通过 | 高 |
| TC-021 | 收件登记 | 运单号重复入库被拦截 | 数据库中已存在 `SF1234567890123` | 1. 运单号输入 `SF1234567890123`；2. 收件人与手机号任意合法值；3. 提交 | HTTP 200，`code = 1001`，`message = 该运单号已登记，请勿重复入库`；`parcel` 表记录数不变；`uk_parcel_waybill` 唯一索引未被触发插入错误页 | 通过 | 高 |
| TC-022 | 收件登记 | 指定货位已满时被拦截 | 新建或选取某货位并将其 `used_count` 调整为等于 `capacity` | 1. 登记新快件并在表单中指定该已满货位；2. 提交 | HTTP 200，`code = 1001`，`message = 该货位已满，请选择其他货位`；`parcel` 表无新增记录；该货位 `used_count` 不变 | 通过 | 高 |
| TC-023 | 收件登记 | 不指定货位时自动分配剩余容量最大的货位 | 已用 staff01 登录；驿站 1 的 B-01-01（`capacity = 60`，`used_count` 记为 M）剩余容量最大 | 1. 登记新快件，`shelfId` 留空不选；2. 提交 | HTTP 200，`code = 200`；返回对象 `shelfCode = B-01-01`；该货位 `used_count` 变为 M+1；`parcel_trace.operate_desc` 中包含分配的库位编号 | 通过 | 高 |
| TC-024 | 收件登记 | 收件人手机号非 11 位被拦截 | 已用 staff01 登录 | 1. 运单号合法，收件人手机号输入 1390000000（10 位）；2. 提交 | HTTP 200，`code = 400` 或 `1001`，提示手机号格式不正确；`parcel` 表记录数不变 | 通过 | 中 |
| TC-025 | 收件登记 | 无可用货位时登记失败 | 将某驿站全部货位的 `used_count` 调整为等于 `capacity` | 1. 登记新快件且不指定货位；2. 提交 | HTTP 200，`code = 1001`，提示该驿站无可用货位，请先新增货位；`parcel` 表记录数不变；测试后恢复货位 `used_count` 基线值 | 通过 | 中 |

### 2.4 取件核销与派送模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-026 | 取件核销 | 取件码与运单号正确完成核销 | 已用 staff01 登录；`parcel.id = 1` 状态为 `IN_STORE`，取件码 `10012034`，`shelf_id = 1` 的 `used_count` 记为 N | 1. 进入取件核销页；2. 输入取件码 `10012034`；3. 选择目标快件并确认取件方式 SELF、核验方式 CODE、实收取件人 王小明；4. 提交核销 | HTTP 200，`code = 200`；`parcel.id = 1` 的 `status` 变为 `PICKED_UP`、`pickup_time` 为当前时间；`pickup_record` 新增 1 行（`parcel_id = 1`、`pickup_code = 10012034`、`pickup_type = SELF`、`verify_type = CODE`、`operator_name = 李思远`）；`parcel_trace` 新增 1 行 `operate_type = PICKUP`；`shelf.id = 1` 的 `used_count` 变为 N-1 | 通过 | 高 |
| TC-027 | 取件核销 | 取件码错误时拒绝核销 | `parcel.id = 1` 仍为 `IN_STORE` | 1. 运单号填 `SF1234567890123`，取件码填 `99999999`；2. 提交核销 | HTTP 200，`code = 1001`，`message = 取件码不正确，请核对`；`parcel.id = 1` 状态仍为 `IN_STORE`；`pickup_record` 与 `parcel_trace` 均无新增记录 | 通过 | 高 |
| TC-028 | 取件核销 | 取件码与运单号不匹配（张冠李戴） | `parcel.id = 1` 与 `parcel.id = 2` 均为 `IN_STORE` | 1. 运单号填 `SF1234567890123`（id = 1），取件码填 `10015678`（属于 id = 2）；2. 提交核销 | HTTP 200，`code = 1001`，`message = 取件码不正确，请核对`；两票快件状态均不变，数据库无写入 | 通过 | 高 |
| TC-029 | 取件核销 | 已取件快件再次核销被拒绝 | `parcel.id = 10` 状态为 `PICKED_UP`，取件码 `10058765` | 1. 运单号填 `ZT7788990011240`，取件码填 `10058765`；2. 提交核销 | HTTP 200，`code = 1001`，`message = 该快件当前状态为「已取件」，不可取件`；`pickup_record` 中 `parcel_id = 10` 的记录仍只有 1 条；货位占用不再二次扣减 | 通过 | 高 |
| TC-030 | 取件核销 | 派送中快件当面签收核销（v1.1 口径修正） | `parcel.id = 13` 状态为 `DELIVERING`，取件码 `10073456`，`shelf_id = 7` 的 `used_count` 记为 N | 1. 运单号填 `YD1122334455670`，取件码填 `10073456`；2. 取件方式选 `DELIVERY`、核验方式选 `ID_CARD`；3. 提交核销 | HTTP 200，`code = 200`；`parcel.id = 13` 的 `status` 变为 `PICKED_UP` 并写入 `pickup_time`；`pickup_record` 新增 1 行且 `pickup_type = DELIVERY`、`verify_type = ID_CARD`；`shelf.id = 7` 的 `used_count` 变为 N-1；`notify_record` 新增 1 条 `PICKUP_DONE` 记录 | 通过 | 高 |
| TC-031 | 取件核销 | 代取场景记录实际取件人 | `parcel.id = 2` 为 `IN_STORE`，取件码 `10015678` | 1. 定位快件 id = 2；2. 取件方式选 AGENT、核验方式选 PHONE；3. 取件人填写 赵丽娜、手机号 13900000002；4. 备注"家属代取"；5. 提交核销 | HTTP 200，`code = 200`；`pickup_record` 新增记录的 `pickup_type = AGENT`、`verify_type = PHONE`、`receiver_name = 赵丽娜`、`remark = 家属代取`；`parcel.receiver_name` 仍为原收件人信息不变 | 通过 | 中 |
| TC-032 | 取件核销 | 逾期保管费试算结果正确 | `parcel.id = 7` 的 `in_time` 为 4 天前、`overdue_days = 3` | 1. 请求 `GET /api/parcels/7/overdue-fee` | HTTP 200，`code = 200`；`data` 中 `storageDays = 4`、`overdueDays = 3`、`overdueFee = 2.00`（超出免费保管期 1 天，按 2.00 元每天计算），与基线数据 `parcel.storage_fee = 2.00` 一致 | 通过 | 中 |
| TC-033 | 取件核销 | 逾期快件核销时收取并记录保管费 | `parcel.id = 7` 为逾期在库快件，取件码 `10042319` | 1. 定位快件 id = 7；2. 按试算金额 2.00 元填写实收保管费；3. 提交核销 | HTTP 200，`code = 200`；`parcel.id = 7` 状态为 `PICKED_UP` 且 `storage_fee = 2.00`；`pickup_record` 新增记录 `storage_fee = 2.00`；`remark` 记录逾期情况 | 通过 | 中 |
| TC-034 | 取件核销 | 核销页按手机号检索快件 | 已用 staff01 登录 | 1. 请求 `GET /api/parcels/query?keyword=13900000001&stationId=1` | HTTP 200，`code = 200`；`data` 为快件对象数组（最多 20 条），包含手机号为 13900000001 的快件；数组元素含 `pickupCode`、`shelfCode`、`status`、`statusName` 字段 | 通过 | 中 |
| TC-035 | 派送管理 | 在库快件发起派送 | `parcel.id = 16` 状态为 `IN_STORE` | 1. 在快件列表选择 id = 16，点击派送；2. 确认 | HTTP 200，`code = 200`；`parcel.id = 16` 的 `status` 变为 `DELIVERING`；`parcel_trace` 新增 1 行 `operate_type = DELIVER`；货位 `used_count` 不变（派送中仍占用货位） | 通过 | 中 |
| TC-036 | 派送管理 | 非在库状态快件发起派送被拒 | `parcel.id = 10` 状态为 `PICKED_UP` | 1. 请求 `PUT /api/parcels/10/deliver` | HTTP 200，`code = 1001`，提示该快件当前状态不可派送；`parcel.id = 10` 状态保持 `PICKED_UP`；`parcel_trace` 无新增记录 | 通过 | 中 |
| TC-037 | 取件核销 | 核销事务失败时不产生半成品数据 | 已用 staff01 登录 | 1. 在核销请求提交过程中模拟写轨迹异常（如临时调整表结构或注入异常）；2. 提交核销 id = 3 的快件 | 请求返回失败提示；事务整体回滚，`parcel.id = 3` 状态仍为 `IN_STORE`、`pickup_time` 仍为 NULL；`pickup_record` 无新增记录；`shelf.used_count` 未被扣减，`parcel`、`pickup_record`、`parcel_trace`、`shelf`、`notify_record` 各表数据保持一致 | 通过 | 高 |

### 2.5 寄件登记模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-038 | 寄件登记 | 正常登记寄件单 | 已用 staff01 登录 | 1. 进入寄件登记页；2. 填写驿站 幸福小区快递驿站、快递公司 顺丰速运、寄件人 王小明/13900000001/地址、收件人 刘洋/13700000001/地址、快件类型 NORMAL、重量 1.5、运费 18、保价 0；3. 提交 | HTTP 200，`code = 200`；`ship_order` 新增 1 行，`order_no` 形如 `S` + 14 位时间戳 + 2 位随机数、`status = PENDING`、`statusName = 待揽收`、`operator_id = 2`、`waybill_no` 为 NULL；`deleted = 0` | 通过 | 高 |
| TC-039 | 寄件登记 | 待揽收寄件单编辑 | `ship_order` 中 `S20260101100004` 为 `PENDING` | 1. 编辑该寄件单，修改重量为 2.5、运费为 16；2. 保存 | HTTP 200，`code = 200`；`ship_order` 对应记录的 `weight = 2.50`、`freight = 16.00`、`update_time` 更新；`status` 保持 `PENDING` | 通过 | 中 |
| TC-040 | 寄件登记 | 非待揽收状态寄件单禁止编辑 | `S20260101100001` 状态为 `SHIPPED` | 1. 请求 `PUT /api/ship-orders/{id}` 修改该寄件单重量 | HTTP 200，`code = 1001`，提示仅待揽收状态可修改；数据库记录不变 | 通过 | 中 |
| TC-041 | 寄件登记 | 寄件单状态流转并回填运单号 | `S20260101100004` 为 `PENDING` | 1. 请求 `PUT /api/ship-orders/{id}/status?status=ACCEPTED&waybillNo=SF3344556677001`；2. 再请求 `PUT /api/ship-orders/{id}/status?status=SHIPPED` | 第 1 次 `code = 200`，`status = ACCEPTED`、`statusName = 已揽收`、`waybill_no = SF3344556677001`；第 2 次 `code = 200`，`status = SHIPPED`、`statusName = 已发出`；`update_time` 同步刷新 | 通过 | 高 |
| TC-042 | 寄件登记 | 取消寄件单 | 存在 `PENDING` 状态的寄件单 | 1. 请求 `PUT /api/ship-orders/{id}/status?status=CANCELLED` | HTTP 200，`code = 200`；`status = CANCELLED`、`statusName = 已取消`；该单据不再参与状态流转 | 通过 | 中 |
| TC-043 | 寄件登记 | 逻辑删除寄件单 | 已用 staff01 登录且拥有 `ship:delete` 权限 | 1. 请求 `DELETE /api/ship-orders/{id}`；2. 查询该寄件单所在分页 | HTTP 200，`code = 200`；数据库中该记录仍物理存在且 `deleted = 1`；分页查询结果中不再出现该寄件单 | 通过 | 中 |

### 2.6 异常件管理模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-044 | 异常件管理 | 登记异常并同步快件状态 | 已用 staff01 登录；`parcel.id = 4` 状态为 `IN_STORE` | 1. 进入异常件管理页；2. 选择快件 id = 4，异常类型选 DAMAGED，描述填写"外包装破损，已拍照留证"；3. 提交 | HTTP 200，`code = 200`；`exception_record` 新增 1 行，`handle_status = PENDING`、`handler_id` 为 NULL、`station_id = 1`；`parcel.id = 4` 的 `status` 变为 `EXCEPTION`；`parcel_trace` 新增 1 行 `operate_type = EXCEPTION`；返回对象含 `exceptionTypeName = 破损`、`handleStatusName = 待处理` | 通过 | 高 |
| TC-045 | 异常件管理 | 异常处理置为处理中 | 上一步新增的异常记录存在 | 1. 请求 `PUT /api/exceptions/{id}/handle`，body 为 `{"handleStatus":"HANDLING","handleResult":"已联系发件网点协商理赔"}` | HTTP 200，`code = 200`；`handle_status = HANDLING`、`handleStatusName = 处理中`、`handle_result` 写入；`handle_time` 为空 | 通过 | 中 |
| TC-046 | 异常件管理 | 异常处理完成并记录处理人与时间 | 该异常记录处于 `HANDLING` | 1. 请求 `PUT /api/exceptions/{id}/handle`，body 为 `{"handleStatus":"RESOLVED","handleResult":"已协商理赔完毕"}` | HTTP 200，`code = 200`；`handle_status = RESOLVED`、`handleStatusName = 已解决`；`handler_id = 2`、`handler_name = 李思远`、`handle_time` 为当前时间 | 通过 | 高 |
| TC-047 | 异常件管理 | 处理异常时同步快件状态为已退回 | 存在 `handle_status` 为 `HANDLING` 的异常记录，对应快件状态为 `EXCEPTION` | 1. 请求 `PUT /api/exceptions/{id}/handle`，body 为 `{"handleStatus":"RESOLVED","handleResult":"已办理退回","parcelStatus":"RETURNED"}` | HTTP 200，`code = 200`；对应 `parcel.status` 变为 `RETURNED`、`statusName = 已退回`；异常记录处理状态与处理结果同步更新 | 通过 | 高 |
| TC-048 | 异常件管理 | 处理不存在的异常记录 | 数据库中不存在该异常记录 id | 1. 请求 `PUT /api/exceptions/999999/handle`，body 合法 | HTTP 200，`code = 404`，提示资源不存在；不产生任何数据变更 | 通过 | 低 |
| TC-049 | 异常件管理 | 按处理状态筛选待处理异常 | 已用 staff01 登录；存在 `PENDING` 异常记录 | 1. 请求 `GET /api/exceptions/page?pageNum=1&pageSize=10&handleStatus=PENDING&stationId=1` | HTTP 200，`code = 200`；`data.list` 全部为 `handleStatus = PENDING` 的记录；`total` 与数据库查询结果一致 | 通过 | 中 |
| TC-050 | 异常件管理 | 无异常登记权限时被拒绝 | 已用 staff01 登录，并在「角色权限」页撤销 STAFF 角色的 `exception:add` 授权后重新登录（测试后恢复授权并重登） | 1. 请求 `POST /api/exceptions`，body 为 `{"parcelId":1,"exceptionType":"OTHER","description":"测试"}` | HTTP 200，`code = 403`，`message = 无操作权限，请联系管理员`；`exception_record` 表无新增记录；`parcel.id = 1` 状态不变 | 通过 | 中 |

### 2.7 查询、导出与统计模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-051 | 快件查询 | 多条件组合查询 | 已用 staff01 登录 | 1. 请求 `GET /api/parcels/page?pageNum=1&pageSize=10&stationId=1&status=IN_STORE&expressCompany=顺丰速运&parcelType=NORMAL` | HTTP 200，`code = 200`；`data.list` 全部满足驿站 1、状态 `IN_STORE`、快递公司顺丰速运、类型 `NORMAL`；`total` 与数据库一致；每项含 `statusName = 在库待取`、`parcelTypeName = 普通件`、`stationName`、`shelfCode` | 通过 | 高 |
| TC-052 | 快件查询 | 分页正确性验证 | 数据库中驿站 1 的快件数量大于 10 | 1. 请求 `pageNum=1&pageSize=10`；2. 请求 `pageNum=2&pageSize=10` | 两次均 `code = 200`；`pageSize = 10`、`pageNum` 回显正确、`pages` 等于总页数；两页记录的 `id` 无交集、无遗漏；`total` 两次一致 | 通过 | 高 |
| TC-053 | 快件查询 | 收件人姓名模糊查询 | 存在收件人姓名包含"王"的快件 | 1. 请求 `GET /api/parcels/page?pageNum=1&pageSize=10&receiverName=王` | HTTP 200，`code = 200`；`data.list` 中记录 `receiverName` 均包含"王"，包含 王小明；不返回姓名不含"王"的记录 | 通过 | 中 |
| TC-054 | 快件查询 | 入库时间区间查询 | 存在多个不同入库日期的快件 | 1. 请求 `startTime` 与 `endTime` 覆盖最近 2 天；2. 观察结果 | HTTP 200，`code = 200`；返回记录的 `inTime` 全部落在区间内；区间外记录不被返回；`idx_parcel_in_time` 生效，查询响应时间小于 1 秒 | 通过 | 中 |
| TC-055 | 快件查询 | 查看快件轨迹 | `parcel.id = 10` 已完成取件 | 1. 请求 `GET /api/parcels/10/traces` | HTTP 200，`code = 200`；`data` 至少包含 `IN_STORE` 与 `PICKUP` 两条轨迹；每项含 `operateType`、`operateDesc`、`operatorName`、`operateTime`；按 `operateTime` 正序排列 | 通过 | 中 |
| TC-056 | 快件查询 | 编辑快件基本信息 | 已用 staff01 登录，拥有 `parcel:edit` 权限；`parcel.id = 3` 为 `IN_STORE` | 1. 请求 `PUT /api/parcels/3`，修改 `receiverName` 为 李强强、`overdueDays` 为 5、`remark` 为"大件，需两人搬运"；2. 查询详情 | HTTP 200，`code = 200`；`receiver_name`、`overdue_days`、`remark` 更新，`update_time` 刷新；`waybill_no` 与 `pickup_code` 保持不变 | 通过 | 中 |
| TC-057 | 快件查询 | 更换存放货位时同步货位计数 | `parcel.id = 3` 的 `shelf_id = 2` | 1. 请求 `PUT /api/parcels/3` 将 `shelfId` 改为 3；2. 查询 `shelf` 表 | HTTP 200，`code = 200`；`parcel.id = 3` 的 `shelf_id = 3`；`shelf.id = 2` 的 `used_count` 减 1，`shelf.id = 3` 的 `used_count` 加 1 | 通过 | 中 |
| TC-058 | 导出 | 导出台账为 Excel 文件 | 已用 staff01 登录，拥有 `parcel:export` 权限 | 1. 在快件查询页选择筛选条件后点击导出；2. 观察响应与文件 | HTTP 200；响应头含 `Content-Disposition: attachment`；文件为 `.xlsx` 格式且可正常打开；表头包含运单号、快递公司、快件类型、收件人、手机号、取件码、状态、入库时间、取件时间；记录数与筛选结果一致 | 通过 | 中 |
| TC-059 | 导出 | 无导出权限时被拒绝 | 已用 staff01 登录，并在「角色权限」页撤销 STAFF 角色的 `parcel:export` 授权后重新登录（测试后恢复授权并重登） | 1. 请求 `GET /api/parcels/export?stationId=1` | HTTP 200，`code = 403`，`message = 无操作权限，请联系管理员`；不返回文件流 | 通过 | 中 |
| TC-060 | 统计 | 首页概览指标正确 | 已用 staff01 登录 | 1. 请求 `GET /api/stats/overview?stationId=1`；2. 与数据库统计结果比对 | HTTP 200，`code = 200`；`data` 含 `todayInCount`、`todayPickupCount`、`todayShipCount`、`inStoreCount`、`overdueCount`、`exceptionCount`、`deliveringCount`、`totalParcelCount` 与 `shelfUsage`（`total`、`used`、`rate`）；各项数值与数据库统计一致，`shelfUsage.rate` 为占用率百分比 | 通过 | 中 |
| TC-061 | 统计 | 近 7 日出入库趋势 | 已用 staff01 登录 | 1. 请求 `GET /api/stats/trend?days=7&stationId=1` | HTTP 200，`code = 200`；`data.dates` 长度为 7、格式为 `MM-dd`；`data.inCounts` 与 `data.pickupCounts` 长度均为 7 且与 `dates` 一一对应；当日入库数与 `overview.todayInCount` 一致 | 通过 | 中 |
| TC-062 | 统计 | 快递公司分布统计 | 已用 staff01 登录 | 1. 请求 `GET /api/stats/company?stationId=1` | HTTP 200，`code = 200`；`data` 为 `[{ name, value }]` 数组，`name` 为快递公司名称（如 顺丰速运）、`value` 为数量；所有 `value` 之和等于该驿站快件总数 | 通过 | 中 |
| TC-063 | 统计 | 快件类型分布统计 | 已用 staff01 登录 | 1. 请求 `GET /api/stats/parcel-type?stationId=1` | HTTP 200，`code = 200`；`data` 结构同公司分布，但 `name` 为中文类型名（如 普通件、易碎品、生鲜）；覆盖数据库中实际存在的类型，不出现英文枚举值 | 通过 | 中 |

### 2.8 系统管理与基础数据模块

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-064 | 用户管理 | 新增用户并指定角色与驿站 | 已用 admin 登录 | 1. 请求 `POST /api/users`，新增用户名 staff03、密码 123456、姓名 周明、手机号 13800000004、`roleIds = [2]`、`stationId = 1`；2. 查询列表 | HTTP 200，`code = 200`；`sys_user` 新增 1 行且密码为 BCrypt 密文；`sys_user_role` 新增 1 行 `role_id = 2`；列表返回对象含 `roleNames = ["驿站员工"]` 与 `stationName = 幸福小区快递驿站`；新账号可登录且可见业务菜单 | 通过 | 高 |
| TC-065 | 用户管理 | 禁用用户后无法登录 | 存在状态正常的账号 staff02（`id = 3`） | 1. 请求 `PUT /api/users/3/status?status=0`；2. 使用 staff02 登录 | 第 1 步 `code = 200`，`sys_user.status = 0`；第 2 步 `code = 1001`，`message = 账号已被禁用，请联系管理员`；测试后恢复 `status = 1` | 通过 | 中 |
| TC-066 | 用户管理 | 重置用户密码 | 已用 admin 登录 | 1. 请求 `PUT /api/users/3/password?password=888888`；2. 用 staff02/888888 登录；3. 用 staff02/123456 登录 | 第 1 步 `code = 200`，`sys_user.password` 更新为新密文的 BCrypt 值；第 2 步登录成功；第 3 步 `code = 1001`；测试后恢复初始密码 | 通过 | 中 |
| TC-067 | 用户管理 | 逻辑删除用户 | 已用 admin 登录；存在测试账号 tester01 | 1. 请求 `DELETE /api/users/{id}`；2. 查询用户分页 | HTTP 200，`code = 200`；`sys_user.deleted = 1`；分页列表中不再出现该用户；该用户的 `sys_user_role` 关联记录保留 | 通过 | 中 |
| TC-068 | 角色权限 | 查看与重新分配角色权限 | 已用 admin 登录 | 1. 请求 `GET /api/roles/2/permissions`；2. 请求 `PUT /api/roles/2/permissions`，body 为 `{"permIds":[1,10,11,12,13,70]}`；3. 重新查询 | 第 1 次返回 staff 原有权限 id 数组；第 2 次 `code = 200`；第 3 次返回的数组与提交集合一致；`sys_role_permission` 中 `role_id = 2` 的记录数等于 6，无重复行；测试后恢复原权限集合 | 通过 | 中 |
| TC-069 | 角色权限 | 获取权限树 | 已用 admin 登录 | 1. 请求 `GET /api/permissions/tree?type=MENU`；2. 请求 `GET /api/permissions/tree?type=ALL` | 两次均 `code = 200`；`type = MENU` 时节点 `permType` 均为 `MENU`；`type = ALL` 时含 `BUTTON` 节点；顶层节点 `parent_id = 0`，子节点位于父节点 `children` 中，节点含 `permCode`、`permName` | 通过 | 中 |
| TC-070 | 驿站管理 | 新增驿站并校验编号唯一 | 已用 admin 登录 | 1. 请求 `POST /api/stations`，`station_code = ST003`、名称 测试驿站、地址、容量 300；2. 再次提交相同 `station_code` | 第 1 次 `code = 200`，`station` 表新增 1 行；第 2 次 `code = 1001`，提示驿站编号已存在；`uk_station_code` 生效；`station` 表记录数不再增加 | 通过 | 中 |
| TC-071 | 货位管理 | 新增货位并校验同驿站编号唯一 | 已用 admin 登录 | 1. 请求 `POST /api/shelves`，`stationId = 1`、`shelfCode = E-01-01`、`area = E`、`capacity = 30`；2. 在同一驿站重复提交相同 `shelfCode`；3. 在 `stationId = 2` 提交 `shelfCode = E-01-01` | 第 1 次 `code = 200`；第 2 次 `code = 1001` 且提示库位编号已存在（`uk_shelf_code` 生效）；第 3 次 `code = 200`，允许不同驿站使用相同库位编号 | 通过 | 中 |
| TC-072 | 货位管理 | 删除仍有在库快件的货位被拒绝 | 存在 `used_count > 0` 的货位（如 id = 1） | 1. 请求 `DELETE /api/shelves/1` | HTTP 200，`code = 1001`，提示该货位仍有在库快件，不允许删除；`shelf` 表记录未被删除 | 通过 | 中 |
| TC-073 | 货位管理 | 可用货位列表仅返回有余量的货位 | 已用 staff01 登录；存在 `used_count < capacity` 与 `used_count = capacity` 的货位 | 1. 请求 `GET /api/shelves/available?stationId=1` | HTTP 200，`code = 200`；`data` 中每条记录均满足 `usedCount < capacity`；返回对象含 `freeCount` 且等于 `capacity - usedCount`；已满货位不在列表中 | 通过 | 中 |

### 2.9 通知与逾期催取模块（v1.1 新增）

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-074 | 通知管理 | 通知记录分页查询与条件筛选 | 已执行初始化脚本（`notify_record` 基线 29 条）；已用 staff01 登录，拥有 `notify:list` | 1. 请求 `GET /api/notifications/page?pageNum=1&pageSize=10&stationId=1`；2. 追加 `notifyType=OVERDUE` 再次请求；3. 追加 `sendStatus=FAILED` 再次请求 | 三次均 HTTP 200、`code = 200`；第 1 次 `total` 等于驿站 1 的通知记录数；第 2 次 `data.list` 全部 `notifyType = OVERDUE`；第 3 次 `data.list` 全部 `sendStatus = FAILED` 且 `failReason` 非空；列表按 `sendTime` 倒序；每项含 `notifyTypeName`、`channelName`、`sendStatusName` | 通过 | 高 |
| TC-075 | 通知管理 | 按运单号与手机号模糊筛选通知记录 | 已用 staff01 登录 | 1. 请求 `GET /api/notifications/page?waybillNo=JT5566`；2. 请求 `GET /api/notifications/page?receiverPhone=13900000008` | 两次均 `code = 200`；`data.list` 中记录的 `waybillNo` 均包含 `JT5566`、`receiverPhone` 均包含 `13900000008`；模糊匹配而非精确匹配 | 通过 | 中 |
| TC-076 | 通知管理 | 查询某快件的全部通知记录 | `parcel.id = 8` 在基线中既有 `IN_STORE` 通知又有失败的 `OVERDUE` 通知；已用 staff01 登录 | 1. 请求 `GET /api/notifications/parcel/8` | HTTP 200，`code = 200`；`data` 为数组且至少含 2 条记录，`notifyType` 分别覆盖 `IN_STORE` 与 `OVERDUE`；按 `sendTime` 倒序排列；含 `receiverName`、`stationName` 关联字段 | 通过 | 中 |
| TC-077 | 通知管理 | 手动发送通知（内容按模板自动生成） | 已用 staff01 登录，拥有 `notify:send`；`parcel.id = 3` 为在库快件；发送前记录 `notify_record` 总条数 N | 1. 请求 `POST /api/notifications`，body 为 `{"parcelId":3,"notifyType":"OVERDUE","channel":"SMS"}`（不传 `content` 与 `receiverPhone`） | HTTP 200，`code = 200`，`message = 通知已发送`；返回对象 `content` 含"已超过免费保管期"与取件码 `10023451`、`content` 非空且为系统模板文案；`receiverPhone = 13900000003`（取快件收件人手机号）；`sendStatus = SUCCESS`；`operatorId = 2`、`operatorName = 李思远`；`notify_record` 总条数变为 N+1 | 通过 | 高 |
| TC-078 | 通知管理 | 手动发送通知（自定义内容与接收号码） | 已用 staff01 登录；`parcel.id = 25` 为在库快件 | 1. 请求 `POST /api/notifications`，body 为 `{"parcelId":25,"notifyType":"IN_STORE","channel":"PHONE","content":"您的生鲜件已到站，请尽快取件。","receiverPhone":"13900000025"}` | HTTP 200，`code = 200`；返回对象 `content` 与请求体完全一致（未被模板覆盖）、`channel = PHONE`、`channelName = 电话`、`receiverPhone = 13900000025`；`notify_record` 新增 1 行且 `operator_name = 李思远` | 通过 | 高 |
| TC-079 | 通知管理 | 批量催取逾期件 | 已用 staff01 登录，拥有 `notify:send`；驿站 1 存在逾期未取的 `IN_STORE` 快件（如 `parcel.id = 7`、`8`）；查询 `notify_record` 中当天 `OVERDUE` 记录数 M | 1. 请求 `POST /api/notifications/batch-overdue?stationId=1&minDays=1` | HTTP 200，`code = 200`，`message = 本次共发送 N 条催取通知`（N 为实际发送条数）；`data` 为整数且与 `message` 中的 N 一致；`notify_record` 新增 N 条 `notifyType = OVERDUE`、`channel = SMS` 的记录，`sendTime` 为当前时间；快件状态与 `shelf.used_count` 均不变 | 通过 | 高 |
| TC-080 | 通知管理 | 同一快件当天重复批量催取被跳过 | 紧接 TC-079 执行，当天已催取过的快件仍在逾期列表中 | 1. 再次请求 `POST /api/notifications/batch-overdue?stationId=1&minDays=1`；2. 对比两次调用前后 `notify_record` 中当天 `OVERDUE` 记录数 | HTTP 200，`code = 200`；第二次返回的实际发送条数为 0（或显著小于第一次），`message = 本次共发送 0 条催取通知`；当天 `OVERDUE` 记录数不再增加，同一 `parcel_id` 当天不存在两条 `OVERDUE` 通知；客户不会被重复骚扰 | 通过 | 高 |
| TC-081 | 通知管理 | 待催取逾期件数量统计 | 已用 staff01 登录；驿站 1 存在逾期未取快件，且数量不超过 50 件 | 1. 请求 `GET /api/notifications/overdue-pending?stationId=1`；2. 与 `GET /api/parcels/overdue/count?stationId=1` 的结果比对 | HTTP 200，`code = 200`；`data` 为非负整数，在逾期件不超过 50 件时与逾期件数量接口结果一致（两者同为"至少逾期 1 天"口径，待催取数量接口因单次处理上限按 50 截断）；无逾期件时返回 0 | 通过 | 中 |
| TC-082 | 通知管理 | 入库后自动产生到件通知 | 已用 staff01 登录；目标运单号 `SF1234567890998` 不在库中；记录 `notify_record` 总条数 N | 1. 请求 `POST /api/parcels/in-store` 登记新快件，`receiverPhone = 13900000001`；2. 查询该快件 id 的通知记录 | 入库接口 `code = 200`；`notify_record` 总条数变为 N+1；新增记录 `notifyType = IN_STORE`、`notifyTypeName = 到件通知`、`channel = SMS`、`sendStatus = SUCCESS`、`operatorId` 为 NULL（系统自动发送）、`content` 含 `expressCompany`、`waybillNo`、驿站名称与新生成的取件码；`sendTime` 与入库时间一致 | 通过 | 高 |
| TC-083 | 通知管理 | 取件核销后自动产生取件确认通知 | 紧接 TC-082，该快件仍为 `IN_STORE`，已取得其 `pickupCode` | 1. 请求 `POST /api/parcels/pickup` 完成核销；2. 查询该快件 id 的通知记录 | 核销接口 `code = 200`；通知记录中新增 1 条 `notifyType = PICKUP_DONE`、`notifyTypeName = 取件确认`、`channel = APP`、`channelName = 站内通知` 的记录，`content` 含运单号与取件完成时间；`operatorId` 为 NULL；核销事务未被通知写入影响 | 通过 | 高 |
| TC-084 | 通知管理 | 收件人手机号非法时记为发送失败 | 已用 staff01 登录；存在一条 `receiver_phone` 非 11 位或不存在的快件（可临时将某在库快件手机号改为空或 10 位） | 1. 请求 `POST /api/notifications`，body 为 `{"parcelId":4,"notifyType":"IN_STORE","channel":"SMS","receiverPhone":"139000000"}`；2. 查询返回对象与数据库记录 | HTTP 200，`code = 200`（接口本身成功执行）；返回对象 `sendStatus = FAILED`、`sendStatusName = 发送失败`、`failReason` 非空且提示号码不合法或为空；失败记录仍持久化到 `notify_record`，便于后续重发与核对 | 通过 | 中 |
| TC-085 | 逾期催取 | 逾期列表按逾期天数分层筛选 | 已用 staff01 登录；驿站 1 存在逾期 1 天以上、3 天以上与 7 天以上的在库快件（可先调整 `in_time` 构造） | 1. 依次请求 `GET /api/parcels/overdue/page?pageNum=1&pageSize=10&stationId=1&minDays=1`、`minDays=3`、`minDays=7` | 三次均 `code = 200`；结果集逐层收窄（`minDays = 7` 的结果是 `minDays = 3` 的子集，`minDays = 3` 是 `minDays = 1` 的子集）；每项 `overdueDayCount` 均不小于对应 `minDays`；列表按 `overdueDayCount` 倒序；`minDays` 传 0 时按 1 处理 | 通过 | 高 |
| TC-086 | 逾期催取 | 逾期列表角色数据权限隔离 | 分别取得 staff01（驿站 1）与 staff02（驿站 2）的令牌；两个驿站均存在逾期件 | 1. staff01 请求 `GET /api/parcels/overdue/page?stationId=2&minDays=1`；2. staff01 请求不带 `stationId` 的同一接口；3. staff02 请求不带 `stationId` 的同一接口 | 第 1 次虽显式传入驿站 2，后端仍强制收敛为 staff01 的本驿站，返回记录 `stationId` 全部为 1；第 2 次返回驿站 1 的逾期件；第 3 次返回驿站 2 的逾期件；三人（含 ADMIN）结果互不越权 | 通过 | 高 |
| TC-087 | 逾期催取 | 逾期件数量角标与列表条数一致性 | 已用 staff01 登录；驿站 1 存在逾期未取快件 | 1. 请求 `GET /api/parcels/overdue/count?stationId=2`；2. 请求不带 `stationId`；3. 依次请求 `GET /api/stats/overview`、`GET /api/parcels/overdue/count`、`GET /api/parcels/overdue/page?minDays=1`、`GET /api/notifications/overdue-pending` | 前两次均 `code = 200`；第 1 次仍按 staff01 本驿站统计（不因传入驿站 2 而统计他站）；第 2 次返回本驿站逾期件数，且与 `parcel` 表中 `status = 'IN_STORE'` 且 `DATEDIFF(NOW(), in_time) - overdue_days >= 1` 的记录数一致；第 3 步**四处数字必须完全相同**（概览角标 / 数量接口 / 逾期列表条数 / 待催取数量），确保"看板显示的数字"与"点进去能看到的条数"一致。补充：`ParcelService.countOverdue` 另保留"既非 ADMIN 也非 STAFF"时按本人手机号统计的防御性分支，当前无此类账号，故本用例不再包含普通用户步骤 | 通过 | 高 |

### 2.10 货位可视化与数据大屏模块（v1.1 新增）

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-088 | 货位地图 | 货位地图格子占用程度与占用率一致 | 已用 staff01 登录，拥有 `shelfmap:view`；驿站 1 存在空货位、正常货位与接近满的货位（可调整 `used_count` 构造） | 1. 请求 `GET /api/shelves/map?stationId=1`；2. 逐一比对每格的 `usedCount`、`capacity`、`rate`、`level` | HTTP 200，`code = 200`；数组每项含 `shelfId`、`shelfCode`、`area`、`capacity`、`usedCount`、`freeCount`、`rate`、`level`、`status`、`parcels`；`freeCount = max(capacity - usedCount, 0)`；`rate` 等于 `usedCount / capacity × 100` 且保留 1 位小数；`usedCount = 0` 时 `level = EMPTY`，`usedCount ≥ capacity` 时 `level = FULL`，占用率 ≥ 80% 且未满时 `level = BUSY`，其余为 `NORMAL`；不存在同一格子 `level` 与 `rate` 相互矛盾的情形 | 通过 | 高 |
| TC-089 | 货位地图 | 货位地图按库位挂载在架快件清单 | 驿站 1 的 `shelf.id = 1` 上存在多件 `IN_STORE` 快件，同时存在已取件快件 | 1. 请求 `GET /api/shelves/map?stationId=1`；2. 检查 `shelfId = 1` 的 `parcels` 数组 | HTTP 200，`code = 200`；`shelfId = 1` 的 `parcels` 仅包含 `status` 属于 `IN_STORE`、`DELIVERING`、`EXCEPTION` 的快件，不含 `PICKED_UP` 与 `RETURNED` 的快件；`parcels` 元素含 `pickupCode`、`waybillNo`、`receiverName`、`statusName`、`storageDays`、`overdueDayCount`；接口通过 2 次查询完成（未按库位逐条查询） | 通过 | 高 |
| TC-090 | 货位地图 | 货位占用重算修复历史不一致 | 已用 admin 或 staff01 登录并拥有 `system:shelf:edit`；人为把某货位的 `used_count` 改为与在架快件数不符的值（如 `used_count = 0` 但架上有 2 件） | 1. 请求 `PUT /api/shelves/recalculate?stationId=1`；2. 重新查询 `shelf` 表与货位地图 | HTTP 200，`code = 200`，`message = 重算完成，共更新 N 个货位`，`data` 为受影响货位行数；重算后每个货位的 `used_count` 等于该货位上 `deleted = 0` 且 `status IN ('IN_STORE','DELIVERING','EXCEPTION')` 的快件数；`PICKED_UP`、`RETURNED` 快件所在的货位不再计入占用；货位地图格子的 `usedCount` 与 `parcels.length` 一致 | 通过 | 高 |
| TC-091 | 数据大屏 | 数据大屏聚合数据 | 已用 admin 登录，拥有 `screen` 权限 | 1. 访问 `/screen` 全屏页面；2. 用 Postman 请求 `GET /api/stats/screen?stationId=1` | 页面一屏展示概览指标、14 日趋势、公司分布、类型分布、最近入库、最近取件、逾期 TOP 与今日通知量；接口 HTTP 200、`code = 200`；`data` 含 `overview`、`trend`（`dates` 长度为 14）、`company`、`parcelType`、`stationRank`、`recentInStore`（≤ 8 条）、`recentPickup`（≤ 8 条）、`topOverdue`（≤ 8 条）、`notifyToday`、`serverTime`；`overview` 各项与 `GET /api/stats/overview?stationId=1` 结果一致；前端首屏只发起 1 次该请求 | 通过 | 高 |
| TC-092 | 数据大屏 | 大屏对员工不返回驿站排行 | 分别取得 admin 与 staff01 的令牌，两者均有 `screen` 权限 | 1. 用 admin 请求 `GET /api/stats/screen`；2. 用 staff01 请求 `GET /api/stats/screen` | 两次均 `code = 200`；admin 返回的 `stationRank` 为非空数组（含 `stationName`、`parcelCount`、`pickupCount`），且与 `GET /api/stats/station-rank` 结果一致；staff01 返回的 `stationRank` 为空数组，且其 `overview`、`trend` 等指标仅覆盖本驿站，员工无法看到其他驿站的业务量 | 通过 | 高 |

### 2.11 收件人自助查询模块（v1.1 新增）

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-093 | 自助查询 | 按手机号查询（返回名下全部待取件，手机号明文） | 手机号 13900000001 名下存在在库快件（`parcel.id = 1`，取件码 `10012034`） | 1. 未登录状态打开 `/query` 页面；2. 输入手机号 13900000001 并查询；3. 对照 Postman 请求 `GET /api/public/pickup-query?keyword=13900000001` | 页面展示"共查询到 N 件待取快件"；结果含 `pickupCode = 10012034`、`waybillNo`、`expressCompany`、`statusName = 在库待取`、`shelfCode`、`inTime`；接口 `code = 200`、`message = 共查询到 1 件待取快件`，响应中 `receiverPhone` 为**明文** `13900000001`（手机号查询不打码；页面本身不展示手机号）；不带令牌访问时接口仍返回 `code = 200` 而非 401，页面也不被重定向到登录页（路由 `meta.public = true`），对照验证：不带令牌请求 `/api/parcels/page` 返回 `code = 401` | 通过 | 高 |
| TC-094 | 自助查询 | 按取件码查询（手机号打码） | 8 位取件码 10012034 对应 `parcel.id = 1`，状态 `IN_STORE` | 1. 在 `/query` 页面输入 8 位取件码 10012034 并查询；2. 检查响应字段 | 返回唯一对应的那 **1** 件，`pickupCode = 10012034`、`waybillNo = SF1234567890123`；`receiverPhone` 为**打码**值 `139****0001`；`code = 200`，未命中其他记录 | 通过 | 高 |
| TC-095 | 自助查询 | 按运单号查询（自动转大写，手机号打码） | 运单号 `SF1234567890123` 对应 `parcel.id = 1`，状态 `IN_STORE` | 1. 输入 `SF1234567890123` 查询；2. 输入小写 `sf1234567890123` 再查一次；3. 检查两次响应 | 两次均返回那 **1** 件（后端自动转大写后精确匹配）；`receiverPhone` 均为**打码**值 `139****0001`；`code = 200` | 通过 | 高 |
| TC-096 | 自助查询 | 非法入参格式被拦截 | 未登录状态 | 1. 在 `/query` 页面输入 `123`（3 位数字）查询；2. 用 Postman 绕过前端，分别请求 `?keyword=123`、`?keyword=12345678901`（11 位但第二位不是 3~9）、`?keyword=SF12`（字母数字位数不足）；3. 不带 `keyword` 与 `phone` 直接请求接口 | 第 1 步页面本地校验拦截并提示「格式不正确：请输入 11 位手机号、8 位取件码或运单号」，不发请求；第 2 步三次均 HTTP 200、`code = 1001`、`message = 请输入 11 位手机号、8 位取件码或运单号（如 SF1234567890123）`；第 3 步 `code = 1001`、`message = 请输入手机号、取件码或运单号`；全过程不返回任何快件数据 | 通过 | 高 |
| TC-097 | 自助查询 | 已取件不返回，且内部字段置空 | `ZT7788990011240` 对应 `parcel.id = 10`，状态 `PICKED_UP`；`parcel.id = 1` 的 `freight`、`remark`、`shelf_id`、`operator_id` 在库中有值 | 1. 请求 `GET /api/public/pickup-query?keyword=ZT7788990011240`；2. 请求 `?keyword=13900000001`；3. 检查第 2 次响应的字段 | 第 1 次 `code = 200`、`data` 为空数组、`message = 未查询到待取快件，请核对手机号 / 取件码 / 运单号`（已取件历史不返回，也不提示"该件已取走"）；第 2 次返回记录中 `stationId`、`shelfId`、`operatorId`、`operatorName`、`freight`、`remark` 均为 `null`；`overdueDays` 被复用为"还可免费保管天数"，`overdueFee` 为按 2 元/天估算值 | 通过 | 高 |
| TC-098 | 自助查询 | 旧参数 `phone` 兼容，且手机号查询最多返回 20 条 | 构造同一手机号名下 25 件在库快件 | 1. 请求 `GET /api/public/pickup-query?phone=13900000001`；2. 请求 `?keyword=13900000001`；3. 用生产 25 件的手机号查询 | 第 1、2 次返回结果完全一致（`phone` 为兼容参数，语义等同 `keyword`，手机号明文）；第 3 次 `data` 长度为 20（不超过 20），按 `inTime` 倒序返回最近的 20 件，避免被用于批量拖取数据 | 通过 | 中 |

### 2.12 取件方式与核验方式校验（v1.1 补测）

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-099 | 取件核销 | 非法取件方式被拒绝 | 已用 staff01 登录；`parcel.id = 2` 为 `IN_STORE`，取件码 `10015678` | 1. 请求 `POST /api/parcels/pickup`，body 中 `pickupType = "DOOR"`（早期文档误用的枚举值），其余字段合法 | HTTP 200，`code = 1001`，`message = 非法的取件方式：DOOR`；`parcel.id = 2` 状态仍为 `IN_STORE`；`pickup_record`、`parcel_trace`、`notify_record` 均无新增记录；正确取值 `DELIVERY` 时可正常核销（见 TC-030） | 通过 | 高 |
| TC-100 | 取件核销 | 非法核验方式被拒绝 | 已用 staff01 登录；`parcel.id = 2` 仍为 `IN_STORE` | 1. 请求 `POST /api/parcels/pickup`，body 中 `verifyType = "FACE"`，其余字段合法 | HTTP 200，`code = 1001`，`message = 非法的核验方式：FACE`；数据库无任何变更；取值 `CODE`、`ID_CARD`、`PHONE` 之一时校验通过 | 通过 | 中 |

### 2.13 找回密码模块（本次新增）

| 用例编号 | 所属模块 | 用例标题 | 前置条件 | 测试步骤 | 预期结果 | 实际结果 | 优先级 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ |
| TC-101 | 找回密码 | 正常找回密码并用新密码登录 | 已执行初始化脚本；`staff01` 的手机号为 `13800000002`，原密码 `123456`；后端 `sms.provider = mock`；`sms_code` 表 0 行 | 1. 退出登录，在登录页点「忘记密码？」；2. 第一步输入账号 `staff01`、手机号 `13800000002`，点「获取验证码」；3. 读取弹窗提示与自动填入的验证码；4. 第二步输入新密码 `abc123456` 与确认密码，点「重置密码」；5. 用 `staff01 / 123456` 登录一次，再用 `staff01 / abc123456` 登录一次；6. 查询 `sms_code` 表并恢复原密码 | 第 2 步 `code = 200`，`message = 验证码已发送`，`data.provider = mock`、`data.maskedPhone = 138****0002`、`data.expireMinutes = 5`、`data.demoCode` 为 6 位数字；前端显示黄色提示「演示模式（未接入短信网关）：验证码 XXXXXX 已自动填入」并自动填入验证码；`sms_code` 新增 1 行（`username = staff01`、`phone = 13800000002`、`scene = PASSWORD_RESET`、`used = 0`、`attempts = 0`、`expire_time = 当前时间 + 5 分钟`）；第 4 步 `code = 200`、`message = 密码已重置，请用新密码登录`、`data = null`，弹窗关闭、账号名自动带回登录表单、密码清空；该验证码行 `used` 变为 1；第 5 步旧密码登录返回 `code = 1001`、`message = 用户名或密码错误`，新密码登录成功；`sys_user.password` 为新的 BCrypt 密文；测试完成后用管理员重置密码或用新密码登录后改回 `123456` | 通过 | 高 |
| TC-102 | 找回密码 | 双要素校验：手机号不匹配与账号不存在提示完全一致 | `staff01` 存在且手机号为 `13800000002`；数据库不存在账号 `zhangsan999`；`sms_code` 表 0 行 | 1. 请求 `POST /api/auth/forgot-password/code`，body `{"username":"staff01","phone":"13900000009"}`；2. 请求同一接口，body `{"username":"zhangsan999","phone":"13800000002"}`；3. 对比两次 `message`；4. 换一个未绑定该手机号的账号再试一次（如 `admin` + `13800000002`） | 三次均 HTTP 200、`code = 1001`、`data = null`，`message` **完全相同**，均为「账号与手机号不匹配，请核对后重试」；`sms_code` 记录数不变（0 行），未触发任何短信或邮件投递；接口不会因账号不存在而返回不同文案，因此无法被用来枚举用户名 | 通过 | 高 |
| TC-103 | 找回密码 | 验证码一次性：同一验证码二次兑换被拒 | 已按 TC-101 第 2 步取得 `staff01` 的验证码并成功重置过一次密码；该验证码行的 `used = 1` | 1. 用同一个 `username`、`phone`、`code` 再次请求 `POST /api/auth/forgot-password/reset`，`newPassword` 换一个值；2. 查询 `sms_code` 与该账号密码 | HTTP 200、`code = 1001`、`message = 验证码已失效，请重新获取`；第二次不修改密码，`sys_user.password` 与第一次重置后的值一致；`sms_code` 中该行仍为 `used = 1`，未新增记录；说明验证码在校验通过时立即作废，不可重复兑换 | 通过 | 高 |
| TC-104 | 找回密码 | 错误次数上限：连续输错 5 次后验证码作废 | 已为 `staff01` 申请验证码并记下正确验证码；`sms_code` 中该行 `attempts = 0`（若 60 秒内刚申请过，需等待间隔或先执行 `DELETE FROM sms_code;` 再申请） | 1. 连续 5 次请求 `POST /api/auth/forgot-password/reset`，每次提交错误验证码（如 `000000`，若与真码相同则换 `111111`），`username`、`phone` 正确；2. 记录每次 `message`；3. 第 6 次提交**正确**验证码；4. 查询 `sms_code` | 第 1 至 4 次均 `code = 1001`，`message` 依次为「验证码不正确，还可尝试 4 次」「……3 次」「……2 次」「……1 次」；第 5 次 `message = 验证码错误次数过多，请重新获取`，该行 `attempts = 5`；第 6 次即使验证码正确也返回 `验证码错误次数过多，请重新获取`，且该行 `used` 被置为 1；必须重新申请验证码才能继续，证明 6 位码不能被无限次暴力尝试 | 通过 | 高 |
| TC-105 | 找回密码 | 发送限流：60 秒间隔与每日上限 | `staff01` 已成功申请过 1 次验证码；`sms_code` 中该手机号当日已有 1 条记录 | 1. 60 秒内再次请求 `POST /api/auth/forgot-password/code`（同账号同手机号）；2. 等待超过 60 秒后再申请一次；3. 用 SQL 把该手机号当日记录的 `create_time` 逐条前移（或直接插入 9 条 `create_time` 为 61 秒前的当日记录），使当日累计达到 10 条，再次申请；4. 清理测试记录 | 第 1 次 HTTP 200、`code = 1001`、`message` 形如「请求过于频繁，请 58 秒后再试」（`N` 为剩余秒数），`sms_code` 不新增；第 2 次成功、`code = 200`；第 3 次 `code = 1001`、`message = 该手机号今日验证码发送次数已达上限，请明天再试`，不再生成新验证码；测试后删除本用例新增的 `sms_code` 记录，恢复 0 行基线 | 通过 | 高 |
| TC-106 | 找回密码 | 通道切换：`mail` 未配置邮箱时返回可读提示而非 500 | 已停止后端；`application-dev.yml` 中 `sms.mail.to` 留空（或环境变量 `SMS_MAIL_TO` 为空）；先执行 `DELETE FROM sms_code;` 清空验证码表；设置环境变量 `SMS_PROVIDER=mail` 后重启后端 | 1. 请求 `POST /api/auth/forgot-password/code`，body 为 `{"username":"staff01","phone":"13800000002"}`；2. 查看响应状态码与响应体；3. 查询 `sms_code`；4. 恢复 `SMS_PROVIDER=mock` 并重启 | 第 1 步 HTTP **200**（不是 500），`code = 1001`、`data = null`，`message = 验证码发送失败，请稍后重试或联系管理员`；后端日志出现 `[MAIL-SMS] 未配置 sms.mail.to` 警告；`sms_code` 记录数仍为 **0**（`sendCode` 标注 `@Transactional(rollbackFor = Exception.class)`，发送失败抛出的业务异常使刚插入的验证码一并回滚，不会留下"永远收不到"的无效记录）；切回 `mock` 后重复申请可正常返回 `demoCode`，证明发送通道由配置切换、业务代码不感知具体通道 | 通过（另做了一次**真实发信**验证：用 QQ 邮箱 `smtp.qq.com:465` + SSL 配置 `spring.mail.*` 与 `sms.mail.to` 后，`provider = mail`、`demoCode = null`、日志出现 `[MAIL-SMS] 验证码已投递至…`，`sms_code` 落库 1 行，用邮件中的验证码完成重置且新密码可登录。首次发信报 `501 Mail from address must be same as authorization user`，原因是信封发件人与认证账号不一致，已通过让发件人恒等于 `spring.mail.username` 修复） | 高 |

---

## 3 接口测试用例

| 用例编号 | 接口名称 | 请求方法 | 请求路径 | 请求参数 / 请求体 | 预期 HTTP 状态 | 预期业务 code | 预期 data 关键字段 | 备注 |
| -------- | -------- | -------- | -------- | ----------------- | -------------- | ------------- | ------------------ | ---- |
| TC-API-001 | 登录 | POST | `/api/auth/login` | `{"username":"admin","password":"123456"}` | 200 | 200 | `token`、`tokenType=Bearer`、`expiresIn=86400`、`userInfo.permissions` | 免登录接口 |
| TC-API-002 | 登录（密码错误） | POST | `/api/auth/login` | `{"username":"admin","password":"wrong"}` | 200 | 1001 | `data` 为空，`message = 用户名或密码错误` | 提示语须一致 |
| TC-API-003 | 注册（已关闭） | POST | `/api/auth/register` | `{"username":"tester01","password":"123456","confirmPassword":"123456","realName":"测试员","phone":"13900000099"}` | 200 | 1001 | `data` 为空，`message = 自助注册已关闭：收件人无需账号，请直接使用自助查询；系统内账号请联系管理员创建` | 免登录接口；不写 `sys_user` |
| TC-API-004 | 注册（已关闭，不带请求体） | POST | `/api/auth/register` | 无请求体 | 200 | 1001 | 同上，`data` 为空 | 路径保留只为给出可读提示，不返回 404 |
| TC-API-005 | 退出登录 | POST | `/api/auth/logout` | 请求头 `Authorization: Bearer <token>` | 200 | 200 | `data = null` | 前端同时清除 `es_token` |
| TC-API-006 | 当前用户信息 | GET | `/api/auth/me` | 请求头 `Authorization: Bearer <staff01 token>` | 200 | 200 | `id`、`username`、`realName`、`stationId=1`、`stationName`、`roles`、`permissions` | 无令牌时 401 |
| TC-API-007 | 当前用户菜单树 | GET | `/api/auth/menus` | 请求头 `Authorization: Bearer <staff01 token>` | 200 | 200 | 数组元素含 `id`、`permCode`、`permName`、`path`、`icon`、`children` | STAFF 不含「系统管理」节点 |
| TC-API-008 | 收件登记 | POST | `/api/parcels/in-store` | `{"waybillNo":"SF1234567890999","stationId":1,"expressCompany":"顺丰速运","parcelType":"NORMAL","receiverName":"王小明","receiverPhone":"13900000001","weight":1.5,"freight":0,"shelfId":1,"overdueDays":3,"remark":"客户要求短信通知"}` | 200 | 200 | `id`、`waybillNo`、`pickupCode`（8 位）、`status=IN_STORE`、`shelfCode` | 写 parcel、trace 并更新货位计数 |
| TC-API-009 | 收件登记（运单号重复） | POST | `/api/parcels/in-store` | 同上传入已存在的 `waybillNo = SF1234567890123` | 200 | 1001 | `message = 该运单号已登记，请勿重复入库` | 数据库无写入 |
| TC-API-010 | 收件登记（运单号格式错误） | POST | `/api/parcels/in-store` | `waybillNo = abc-123`，其余字段合法 | 200 | 1001 | `message = 运单号格式不正确，请核对快递公司` | 正则校验拦截 |
| TC-API-011 | 快件快速检索 | GET | `/api/parcels/query` | `keyword=10012034&stationId=1` | 200 | 200 | 快件对象数组（最多 20 条），含 `pickupCode`、`shelfCode`、`status`、`statusName` | 取件核销页使用 |
| TC-API-012 | 取件核销 | POST | `/api/parcels/pickup` | `{"waybillNo":"SF1234567890123","pickupCode":"10012034","receiverName":"王小明","receiverPhone":"13900000001","pickupType":"SELF","verifyType":"CODE","storageFee":0,"remark":""}` | 200 | 200 | 快件状态更新为 `PICKED_UP`；`pickup_record`、`parcel_trace` 与 `notify_record`（`PICKUP_DONE`）新增 | 五表同一事务，通知失败不影响核销 |
| TC-API-013 | 取件核销（取件码错误） | POST | `/api/parcels/pickup` | 同上但 `pickupCode = 99999999` | 200 | 1001 | `message = 取件码不正确，请核对` | 状态与记录均不变 |
| TC-API-014 | 取件核销（状态不允许） | POST | `/api/parcels/pickup` | `waybillNo = ZT7788990011240`，`pickupCode = 10058765`（该件已取件） | 200 | 1001 | `message = 该快件当前状态为「已取件」，不可取件` | 状态前置校验 |
| TC-API-015 | 快件分页条件查询 | GET | `/api/parcels/page` | `pageNum=1&pageSize=10&stationId=1&status=IN_STORE&expressCompany=顺丰速运` | 200 | 200 | `total`、`pages`、`pageNum`、`pageSize`、`list`；列表项含 `statusName`、`parcelTypeName`、`stationName`、`shelfCode` | STAFF 调用时强制按本驿站覆盖 `stationId` |
| TC-API-016 | 快件详情 | GET | `/api/parcels/{id}` | `id = 1` | 200 | 200 | `waybillNo`、`pickupCode`、`status`、`inTime`、`overdueDays`、`storageDays`、`overdueFee`、`operatorName` | 不存在时 404 |
| TC-API-017 | 快件轨迹 | GET | `/api/parcels/{id}/traces` | `id = 10` | 200 | 200 | 数组元素含 `id`、`operateType`、`operateDesc`、`operatorName`、`operateTime` | 至少含 IN_STORE 与 PICKUP |
| TC-API-018 | 编辑快件 | PUT | `/api/parcels/{id}` | `{"receiverName":"李强强","receiverPhone":"13900000003","parcelType":"LARGE","shelfId":3,"overdueDays":5,"remark":"大件"}` | 200 | 200 | 更新后的快件对象；`update_time` 刷新 | 需 `parcel:edit` 权限 |
| TC-API-019 | 删除快件 | DELETE | `/api/parcels/{id}` | `id = 2`，请求头为 admin 令牌 | 200 | 200 | `data = null`；`parcel.deleted = 1`；货位占用释放 | 需 `parcel:delete`，STAFF 返回 403 |
| TC-API-020 | 派送 | PUT | `/api/parcels/{id}/deliver` | `id = 16` | 200 | 200 | `status = DELIVERING`、`statusName = 派送中`；新增 DELIVER 轨迹 | 需 `parcel:deliver` 权限 |
| TC-API-021 | 逾期费用试算 | GET | `/api/parcels/{id}/overdue-fee` | `id = 7` | 200 | 200 | `storageDays = 4`、`overdueDays = 3`、`overdueFee = 2.00` | 与基线 `storage_fee` 一致 |
| TC-API-022 | 导出台账 | GET | `/api/parcels/export` | `stationId=1&status=IN_STORE` | 200 | — | 响应体为 `.xlsx` 文件流，响应头含 `Content-Disposition: attachment` | 需 `parcel:export` 权限 |
| TC-API-023 | 寄件登记 | POST | `/api/ship-orders` | `{"stationId":1,"expressCompany":"顺丰速运","senderName":"王小明","senderPhone":"13900000001","senderAddress":"江苏省南京市江宁区幸福路 128 号","receiverName":"刘洋","receiverPhone":"13700000001","receiverAddress":"北京市海淀区中关村大街 1 号","parcelType":"NORMAL","weight":1.5,"freight":18,"insuredValue":0}` | 200 | 200 | `orderNo` 以 `S` 开头、`status = PENDING`、`statusName = 待揽收`、`operatorId = 2` | `uk_ship_order_no` 保障唯一 |
| TC-API-024 | 寄件单分页查询 | GET | `/api/ship-orders/page` | `pageNum=1&pageSize=10&status=PENDING&stationId=1` | 200 | 200 | 分页结构；列表项含 `orderNo`、`stationName`、`receiverName`、`statusName`、`waybillNo` | 需 `ship:list` 权限 |
| TC-API-025 | 更新寄件状态并回填运单号 | PUT | `/api/ship-orders/{id}/status` | `status=ACCEPTED&waybillNo=SF3344556677001` | 200 | 200 | `status = ACCEPTED`、`statusName = 已揽收`、`waybillNo` 已回填 | 需 `ship:status` 权限 |
| TC-API-026 | 删除寄件单 | DELETE | `/api/ship-orders/{id}` | 请求头为 admin 令牌 | 200 | 200 | `ship_order.deleted = 1`；分页查询不再返回 | 需 `ship:delete`，STAFF 返回 403 |
| TC-API-027 | 异常件分页查询 | GET | `/api/exceptions/page` | `pageNum=1&pageSize=10&handleStatus=PENDING&stationId=2` | 200 | 200 | 分页结构；列表项含 `waybillNo`、`exceptionType`、`exceptionTypeName`、`handleStatus`、`handleStatusName`、`handlerName` | 需 `exception:list` 权限 |
| TC-API-028 | 登记异常 | POST | `/api/exceptions` | `{"parcelId":14,"exceptionType":"DAMAGED","description":"外包装破损"}` | 200 | 200 | 异常记录对象；对应 `parcel.status = EXCEPTION`；新增 EXCEPTION 轨迹 | 需 `exception:add` 权限 |
| TC-API-029 | 处理异常 | PUT | `/api/exceptions/{id}/handle` | `{"handleStatus":"RESOLVED","handleResult":"已协商理赔，快件退回","parcelStatus":"RETURNED"}` | 200 | 200 | `handleStatus = RESOLVED`、`handlerName`、`handleTime`；`parcel.status = RETURNED` | `parcelStatus` 可选 |
| TC-API-030 | 统计概览 | GET | `/api/stats/overview` | `stationId=1` | 200 | 200 | `todayInCount`、`todayPickupCount`、`todayShipCount`、`inStoreCount`、`overdueCount`、`exceptionCount`、`deliveringCount`、`totalParcelCount`、`shelfUsage` | 需登录 |
| TC-API-031 | 出入库趋势 | GET | `/api/stats/trend` | `days=7&stationId=1` | 200 | 200 | `dates`、`inCounts`、`pickupCounts`（长度均为 7） | 需登录 |
| TC-API-032 | 快递公司分布 | GET | `/api/stats/company` | `stationId=1` | 200 | 200 | `[{ name, value }]` | 需登录 |
| TC-API-033 | 快件类型分布 | GET | `/api/stats/parcel-type` | `stationId=1` | 200 | 200 | `[{ name, value }]`，`name` 为中文类型名 | 需登录 |
| TC-API-034 | 驿站业务量排行 | GET | `/api/stats/station-rank` | 请求头为 admin 令牌 | 200 | 200 | `[{ stationName, parcelCount, pickupCount }]` | 仅 ADMIN，STAFF 返回 403 |
| TC-API-035 | 用户分页查询 | GET | `/api/users/page` | `pageNum=1&pageSize=10&status=1&roleCode=STAFF` | 200 | 200 | 分页结构；列表项含 `username`、`realName`、`stationName`、`roleNames` | 需 `system:user:list` |
| TC-API-036 | 新增用户 | POST | `/api/users` | `{"username":"staff03","password":"123456","realName":"周明","phone":"13800000004","roleIds":[2],"stationId":1}` | 200 | 200 | 新增用户对象；`sys_user_role` 写入 `role_id = 2` | 需 `system:user:add` |
| TC-API-037 | 启用／禁用用户 | PUT | `/api/users/{id}/status` | `status=0` | 200 | 200 | 用户对象 `status = 0`；该账号登录返回 1001 | 需 `system:user:edit` |
| TC-API-038 | 重置密码 | PUT | `/api/users/{id}/password` | `password=123456` | 200 | 200 | `data = null`；`sys_user.password` 更新为 BCrypt 密文 | 需 `system:user:reset` |
| TC-API-039 | 修改本人资料 | PUT | `/api/users/profile` | `{"realName":"李思远","phone":"13800000002","gender":1}` | 200 | 200 | 更新后的用户对象 | 登录即可 |
| TC-API-040 | 修改本人密码 | PUT | `/api/users/self/password` | `{"oldPassword":"123456","newPassword":"654321"}` | 200 | 200 | `data = null`；旧密码错误时返回 1001 | 登录即可 |
| TC-API-041 | 角色权限查询与分配 | GET / PUT | `/api/roles/{id}/permissions` | GET 无参数；PUT body `{"permIds":[1,10,11,12,13,70]}` | 200 | 200 | GET 返回权限 id 数组；PUT 后集合与提交一致 | 需 `system:role:assign` |
| TC-API-042 | 权限树 | GET | `/api/permissions/tree` | `type=ALL` | 200 | 200 | 树形数组，节点含 `id`、`permCode`、`permName`、`permType`、`children` | 需 ADMIN |
| TC-API-043 | 驿站列表 | GET | `/api/stations/list` | 请求头为 staff01 令牌 | 200 | 200 | 启用驿站数组，含 `id`、`stationCode`、`stationName` | 所有登录用户可读 |
| TC-API-044 | 驿站分页与新增 | GET / POST | `/api/stations/page`、`/api/stations` | GET `pageNum=1&pageSize=10&status=1`；POST 含 `stationCode`、`stationName`、`address`、`capacity` | 200 | 200 | 驿站对象含 `shelfCount`、`usedCount`；`stationCode` 重复时 1001 | 需 `system:station:list` |
| TC-API-045 | 货位列表与新增 | GET / POST | `/api/shelves/list`、`/api/shelves` | GET `stationId=1`；POST 含 `stationId`、`shelfCode`、`area`、`capacity` | 200 | 200 | 货位对象含 `shelfCode`、`capacity`、`usedCount`、`freeCount` | 同一驿站编号唯一 |
| TC-API-046 | 删除货位（有在库快件） | DELETE | `/api/shelves/{id}` | `id = 1`（`used_count > 0`） | 200 | 1001 | `message` 提示该货位仍有在库快件 | 需 `system:shelf:edit` |
| TC-API-047 | 通知记录分页查询 | GET | `/api/notifications/page` | `pageNum=1&pageSize=10&stationId=1&notifyType=OVERDUE&channel=SMS&sendStatus=SUCCESS` | 200 | 200 | `total`、`pages`、`pageNum`、`pageSize`、`list`；列表项含 `id`、`parcelId`、`waybillNo`、`pickupCode`、`notifyType`、`notifyTypeName`、`channel`、`channelName`、`receiverPhone`、`content`、`sendStatus`、`sendStatusName`、`stationId`、`stationName`、`receiverName`、`operatorId`、`operatorName`、`sendTime` | 需 `notify:list`；STAFF 未传 `stationId` 时强制本驿站 |
| TC-API-048 | 通知记录时间区间筛选 | GET | `/api/notifications/page` | `pageNum=1&pageSize=10&startTime=2026-10-01&endTime=2026-10-07` | 200 | 200 | 分页结构；`list` 中每条 `sendTime` 均落在区间内（`endTime` 含当天） | `send_time` 范围查询 |
| TC-API-049 | 某快件的通知记录 | GET | `/api/notifications/parcel/{parcelId}` | `parcelId = 8`，请求头为 staff01 令牌 | 200 | 200 | 通知记录数组，按 `sendTime` 倒序；至少含 `IN_STORE` 与 `OVERDUE` 两类 | 需同时具备 `notify:list` 与 `parcel:list`（注解默认 AND） |
| TC-API-050 | 手动发送通知 | POST | `/api/notifications` | `{"parcelId":3,"notifyType":"OVERDUE","channel":"SMS","content":"您的快件已超期，请尽快取件。","receiverPhone":"13900000003"}` | 200 | 200 | `message = 通知已发送`；返回记录含 `id`、`notifyType=OVERDUE`、`channel=SMS`、`content` 与请求一致、`sendStatus=SUCCESS`、`operatorId=2`、`operatorName=李思远` | 需 `notify:send` |
| TC-API-051 | 手动发送通知（内容留空自动生成） | POST | `/api/notifications` | `{"parcelId":3,"notifyType":"IN_STORE"}` | 200 | 200 | 返回记录 `content` 为系统模板文案（含快递公司、运单号与取件码）；`channel` 默认 `SMS`；`receiverPhone` 取快件收件人手机号 | 默认值生效 |
| TC-API-052 | 手动发送通知（非法通知类型） | POST | `/api/notifications` | `{"parcelId":3,"notifyType":"UNKNOWN","channel":"SMS"}` | 200 | 1001 | `message = 非法的通知类型：UNKNOWN`；不写入 `notify_record` | 枚举校验 |
| TC-API-053 | 手动发送通知（非法渠道） | POST | `/api/notifications` | `{"parcelId":3,"notifyType":"IN_STORE","channel":"WECHAT"}` | 200 | 1001 | `message = 非法的通知渠道：WECHAT`；不写入 `notify_record` | 枚举校验 |
| TC-API-054 | 批量催取逾期件 | POST | `/api/notifications/batch-overdue` | `stationId=1&minDays=1` | 200 | 200 | `message = 本次共发送 N 条催取通知`；`data` 为整数 N；`notify_record` 新增 N 条 `OVERDUE`/`SMS` 记录 | 需 `notify:send`；同一快件当天去重 |
| TC-API-055 | 批量催取（当天重复调用） | POST | `/api/notifications/batch-overdue` | 紧接上一条再次调用 `stationId=1&minDays=1` | 200 | 200 | `data` 为 0（或显著减少）；当天 `OVERDUE` 记录数不再增加 | 去重逻辑生效 |
| TC-API-056 | 待催取逾期件数量 | GET | `/api/notifications/overdue-pending` | `stationId=1` | 200 | 200 | `data` 为非负整数，口径为"至少逾期 1 天"的在库快件数（上限 50） | 需 `notify:list` |
| TC-API-057 | 通知记录查询（无 `notify:list` 权限） | GET | `/api/notifications/page` | `pageNum=1&pageSize=10`，请求头为"撤销 STAFF 的 `notify:list` 授权后重新登录"的 staff01 令牌 | 200 | 403 | `message = 无操作权限，请联系管理员` | 先被权限切面拦截；测试后恢复授权 |
| TC-API-058 | 逾期未取分页查询 | GET | `/api/parcels/overdue/page` | `pageNum=1&pageSize=10&stationId=1&minDays=3` | 200 | 200 | 分页结构；`list` 中每条 `overdueDayCount ≥ 3`；按逾期天数倒序；元素含 `statusName`、`storageDays`、`overdueFee` | 需 `parcel:overdue` |
| TC-API-059 | 逾期件数量 | GET | `/api/parcels/overdue/count` | `stationId=1` | 200 | 200 | `data` 为整数，与 `parcel` 表中在库且已超免费保管期的记录数一致 | 需 `parcel:list`；员工强制本驿站 |
| TC-API-060 | 货位地图 | GET | `/api/shelves/map` | `stationId=1` | 200 | 200 | `ShelfMapVO` 数组；元素含 `shelfId`、`shelfCode`、`area`、`capacity`、`usedCount`、`freeCount`、`rate`、`level`（`EMPTY`/`NORMAL`/`BUSY`/`FULL`）、`status`、`parcels` | 需 `shelfmap:view` |
| TC-API-061 | 重算货位占用 | PUT | `/api/shelves/recalculate` | `stationId=1` | 200 | 200 | `message = 重算完成，共更新 N 个货位`；`data` 为受影响货位行数；重算后 `used_count` 与在架快件数一致 | 需 `system:shelf:edit` |
| TC-API-062 | 数据大屏聚合 | GET | `/api/stats/screen` | `stationId=1`，请求头为 admin 令牌 | 200 | 200 | `overview`、`trend`（14 日）、`company`、`parcelType`、`stationRank`（非空）、`recentInStore`、`recentPickup`、`topOverdue`、`notifyToday`、`serverTime` | 需 `screen`；一次请求返回全部指标 |
| TC-API-063 | 数据大屏（员工视角） | GET | `/api/stats/screen` | 请求头为 staff01 令牌 | 200 | 200 | 结构同上，但 `stationRank` 为空数组；其余指标仅覆盖 staff01 所属驿站 | 驿站排行仅 ADMIN 有数据 |
| TC-API-064 | 公开取件码查询（手机号，明文） | GET | `/api/public/pickup-query` | `keyword=13900000001`，**不带 Authorization 头** | 200 | 200 | `message = 共查询到 1 件待取快件`；数组元素含 `pickupCode`、`waybillNo`、`expressCompany`、`status`、`statusName`、`shelfCode`、`inTime`；`receiverPhone = 13900000001`（手机号查询**不打码**） | 免登录接口 |
| TC-API-065 | 公开取件码查询（取件码，打码） | GET | `/api/public/pickup-query` | `keyword=10012034`，不带 Authorization 头 | 200 | 200 | 命中 1 件，`pickupCode = 10012034`；`receiverPhone = 139****0001`（**已打码**） | 取件码查询不暴露完整手机号 |
| TC-API-066 | 公开取件码查询（运单号，转大写 + 打码） | GET | `/api/public/pickup-query` | `keyword=sf1234567890123`（小写） | 200 | 200 | 命中 1 件，`waybillNo = SF1234567890123`（自动转大写后匹配）；`receiverPhone = 139****0001` | 运单号印在包裹上，故打码 |
| TC-API-067 | 公开取件码查询（非法格式） | GET | `/api/public/pickup-query` | `keyword=123`，不带 Authorization 头 | 200 | 1001 | `message = 请输入 11 位手机号、8 位取件码或运单号（如 SF1234567890123）`；不执行查询 | 非三种格式一律拒绝查询；空入参提示"请输入手机号、取件码或运单号" |
| TC-API-068 | 取件核销（非法取件方式） | POST | `/api/parcels/pickup` | `{"waybillNo":"SF1234567890124","pickupCode":"10015678","receiverName":"赵丽娜","pickupType":"DOOR","verifyType":"CODE"}` | 200 | 1001 | `message = 非法的取件方式：DOOR`；状态与记录均不变 | 枚举校验，正确值为 `DELIVERY` |
| TC-API-069 | 取件核销（非法核验方式） | POST | `/api/parcels/pickup` | 同上但 `pickupType = "SELF"`、`verifyType = "FACE"` | 200 | 1001 | `message = 非法的核验方式：FACE`；状态与记录均不变 | 枚举校验 |
| TC-API-070 | 入库自动通知 | POST | `/api/parcels/in-store` | `{"waybillNo":"SF1234567890997","stationId":1,"expressCompany":"顺丰速运","parcelType":"NORMAL","receiverName":"王小明","receiverPhone":"13900000001","overdueDays":3}` | 200 | 200 | 入库成功；随后查询该快件通知记录，新增 1 条 `IN_STORE`/`SMS`、`sendStatus=SUCCESS`、`operatorId=null` 的记录 | 通知在入库事务内写入 |
| TC-API-071 | 核销自动通知 | POST | `/api/parcels/pickup` | 对 TC-API-070 登记的快件执行核销（`pickupType = "SELF"`、`verifyType = "CODE"`） | 200 | 200 | 核销成功；该快件通知记录新增 1 条 `PICKUP_DONE`/`APP` 的记录 | 通知失败不影响核销 |
| TC-API-072 | 找回密码-申请验证码 | POST | `/api/auth/forgot-password/code` | 正常：`{"username":"staff01","phone":"13800000002"}`；异常：手机号换成 `13900000009`；账号换成 `zhangsan999`；60 秒内重复申请 | 200 | 200 / 1001 | 正常时 `message = 验证码已发送`，`data` 含 `message`、`maskedPhone = 138****0002`、`expireMinutes = 5`、`provider = mock`、`demoCode` 为 6 位数字（`mail`/`aliyun` 下 `demoCode = null`）；异常时 `data = null`，手机号不匹配与账号不存在均返回「账号与手机号不匹配，请核对后重试」，限流返回「请求过于频繁，请 N 秒后再试」；**不带 `Authorization` 头也能访问** | 免登录白名单；`sms_code` 正常时新增 1 行、异常时不新增；每日上限需先把该手机号当日记录构造到 10 条再申请 |
| TC-API-073 | 找回密码-重置密码 | POST | `/api/auth/forgot-password/reset` | 正常：`{"username":"staff01","phone":"13800000002","code":"<上一步 demoCode>","newPassword":"abc123456"}`；异常：同一个 `code` 再提交一次；`code` 改成 `000000`；`code` 改成 `12345`；`newPassword` 改成 `123` | 200 | 200 / 1001 / 400 | 正常时 `message = 密码已重置，请用新密码登录`、`data = null`，`sms_code.used = 1`，`sys_user.password` 更新为 BCrypt 密文；重复兑换返回「验证码已失效，请重新获取」；错误码返回「验证码不正确，还可尝试 N 次」；6 位数字与密码长度不合法时 `code = 400`；**不带 `Authorization` 头也能访问** | 免登录白名单；测试后把 `staff01` 密码恢复为 `123456` |

---

## 4 需求覆盖矩阵

| 需求编号 | 需求名称 | 覆盖用例 |
| -------- | -------- | -------- |
| FR-01 | 用户登录 | TC-001 至 TC-005、TC-101 至 TC-106、TC-API-001、TC-API-002、TC-API-072、TC-API-073 |
| FR-02 | 账号创建（自助注册已取消） | TC-006、TC-007、TC-008、TC-009、TC-API-003、TC-API-004 |
| FR-03 | 登录状态维护 | TC-010、TC-011、TC-018、TC-API-005、TC-API-006、TC-API-007 |
| FR-04 | 用户管理 | TC-064、TC-065、TC-066、TC-067、TC-API-035 至 TC-API-038 |
| FR-05 | 角色管理与权限分配 | TC-013、TC-015、TC-068、TC-069、TC-API-041、TC-API-042 |
| FR-06 | 个人中心 | TC-API-039、TC-API-040 |
| FR-07 | 快件入库登记 | TC-019、TC-020、TC-021、TC-024、TC-API-008 至 TC-API-010 |
| FR-08 | 货位自动分配 | TC-022、TC-023、TC-025 |
| FR-09 | 快件快速定位 | TC-034、TC-API-011 |
| FR-10 | 快件分页条件查询 | TC-051、TC-052、TC-053、TC-054、TC-API-015 |
| FR-11 | 快件详情与轨迹查看 | TC-055、TC-API-016、TC-API-017 |
| FR-12 | 快件信息编辑与删除 | TC-056、TC-057、TC-API-018、TC-API-019 |
| FR-13 | 取件核销出库 | TC-026、TC-027、TC-028、TC-029、TC-030、TC-037、TC-099、TC-100、TC-API-012 至 TC-API-014、TC-API-068、TC-API-069 |
| FR-14 | 逾期保管费试算与收取 | TC-032、TC-033、TC-API-021 |
| FR-15 | 代取与送货上门登记 | TC-031 |
| FR-16 | 派送操作 | TC-035、TC-036、TC-API-020 |
| FR-17 | 寄件单登记 | TC-038、TC-API-023 |
| FR-18 | 寄件单查询、编辑与状态流转 | TC-039、TC-040、TC-041、TC-042、TC-043、TC-API-024 至 TC-API-026 |
| FR-19 | 异常件登记 | TC-044、TC-API-028 |
| FR-20 | 异常件查询与处理 | TC-045、TC-046、TC-047、TC-048、TC-049、TC-050、TC-API-027、TC-API-029 |
| FR-21 | 业务概览统计 | TC-060、TC-API-030 |
| FR-22 | 多维度统计分析 | TC-061、TC-062、TC-063、TC-API-031 至 TC-API-034 |
| FR-23 | 台账导出 Excel | TC-058、TC-059、TC-API-022 |
| FR-24 | 驿站管理 | TC-070、TC-API-043、TC-API-044 |
| FR-25 | 货位管理 | TC-071、TC-072、TC-073、TC-API-045、TC-API-046 |
| FR-26 | 到件与逾期通知 | TC-074、TC-075、TC-076、TC-077、TC-078、TC-082、TC-083、TC-084、TC-API-047 至 TC-API-053、TC-API-070、TC-API-071 |
| FR-27 | 逾期催取 | TC-079、TC-080、TC-081、TC-085、TC-086、TC-087、TC-API-054 至 TC-API-059 |
| FR-28 | 货位可视化地图 | TC-088、TC-089、TC-090、TC-API-060、TC-API-061 |
| FR-29 | 数据大屏 | TC-091、TC-092、TC-API-062、TC-API-063 |
| FR-30 | 收件人自助查询（手机号 / 取件码 / 运单号三种入参） | TC-093、TC-094、TC-095、TC-096、TC-097、TC-098、TC-API-064 至 TC-API-067 |
| NFR 安全性 | 密码加密与接口鉴权 | TC-004、TC-010、TC-011、TC-012、TC-013、TC-016、TC-017、TC-018、TC-050、TC-059、TC-096、TC-097、TC-102、TC-104、TC-105、TC-API-057、TC-API-065、TC-API-066、TC-API-072、TC-API-073 |
| NFR 性能 | 查询与统计响应时间 | TC-052、TC-054、TC-060、TC-061、TC-088、TC-091、TC-098 |

---

## 5 测试执行记录与缺陷管理

### 5.1 执行记录表（模板）

| 测试轮次 | 执行日期 | 执行人 | 用例总数 | 执行数 | 通过数 | 失败数 | 阻塞数 | 通过率 |
| -------- | -------- | ------ | -------- | ------ | ------ | ------ | ------ | ------ |
| 第 1 轮（功能测试） | ____ | ____ | ____ | ____ | ____ | ____ | ____ | ____% |
| 第 2 轮（回归测试） | ____ | ____ | ____ | ____ | ____ | ____ | ____ | ____% |
| 第 3 轮（验收测试） | ____ | ____ | ____ | ____ | ____ | ____ | ____ | ____% |

### 5.2 缺陷记录表（模板）

| 缺陷编号 | 关联用例 | 缺陷描述 | 缺陷等级 | 发现日期 | 修复状态 | 复测结果 |
| -------- | -------- | -------- | -------- | -------- | -------- | -------- |
| BUG-001 | ____ | ____ | ____ | ____ | ____ | ____ |
| BUG-002 | ____ | ____ | ____ | ____ | ____ | ____ |
| BUG-003 | ____ | ____ | ____ | ____ | ____ | ____ |

缺陷等级定义：

| 等级 | 定义 | 处理要求 |
| ---- | ---- | -------- |
| 致命 | 系统无法启动、核心业务完全不可用、数据严重错乱或权限被绕过 | 必须立即修复并重新回归全部核心用例 |
| 严重 | 主要功能不可用、返回结果错误、事务不一致 | 本轮必须修复，修复后回归相关模块 |
| 一般 | 次要功能异常、提示语不准确、界面显示问题 | 可在本轮或下轮修复 |
| 提示 | 文案、样式、易用性建议 | 视情况优化 |

### 5.3 阻塞与风险说明（模板）

1. 系统会话状态全部由 JWT 令牌承载，后端服务不依赖任何外部缓存或中间件即可独立运行，因此测试环境搭建不存在因第三方组件缺失导致的阻塞项。
2. 未接入真实快递公司接口，运单号依赖人工录入，运单号正则规则的覆盖范围需在验收时与指导教师确认。
3. 若测试环境时间与服务器时间不一致，将影响"当日入库量""逾期天数"等时间敏感用例的结果，测试前需同步系统时间。
4. 涉及修改数据库基线数据的用例（如 TC-004、TC-022、TC-025、TC-065、TC-066、TC-068、TC-090、TC-098）在执行完毕后必须恢复数据，建议在用例执行前后各执行一次基线数据备份与还原。
5. v1.1 的通知功能未接入真实短信网关，`NotifyService.dispatch` 以写入 `notify_record` 的方式模拟发送。因此通知类用例（TC-074 至 TC-084、TC-API-047 至 TC-API-056）验证的是"通知是否按规则落库、结果与失败原因是否正确、当天是否去重"，而非短信是否真实送达；接入网关后需补充真实发送与运营商回执的验证项。
6. 找回密码的验证码发送通道默认 `mock`，TC-101 至 TC-105 在 `mock` 下执行（接口回传 `demoCode` 便于自动化断言）；TC-106 需临时把 `SMS_PROVIDER` 切到 `mail` 并留空 `sms.mail.to`。执行后须清空 `sms_code` 并把 `staff01` 的密码恢复为 `123456`，否则会影响后续用例的登录前置条件。
7. 通知类用例与逾期类用例涉及"当天"这一时间维度（如 TC-080 的当天去重、TC-081 的待催取数量），跨零点执行会导致结果变化，建议在同一自然日内连续执行并记录执行时间。
8. **数据范围已在服务层强制收敛（v1.1 修复项）**：v1.1 开发过程中曾发现部分接口的数据范围只在前端限制、后端未强制收敛，具体为：`GET /api/shelves/map` 与 `PUT /api/shelves/recalculate` 不校验驿站归属；`POST /api/notifications/batch-overdue`、`GET /api/notifications/overdue-pending` 与 `GET /api/notifications/page` 仅在入参为空时才取登录用户所属驿站；`GET /api/parcels/overdue/count` 曾对未绑定驿站的账号返回全部驿站的逾期件数。**上述问题均已修复**：货位地图与重算统一走 `ShelfService.resolveStationScope`；通知相关接口对非管理员一律强制覆盖为本人所属驿站；逾期件数量接口与分页接口统一口径（`countOverdue` 对"既非 ADMIN 也非 STAFF"的账号按本人手机号统计，属防御性分支，当前无此类账号）。TC-086、TC-087 已按修复后的行为描述，验证方式为"员工伪造 `stationId` 请求他站数据，断言响应仍只含本驿站记录"。

### 5.4 测试数据清理与基线还原

为保证多轮测试结果可比，每轮测试结束后应按下列步骤将数据库还原至基线状态。

| 步骤 | 操作内容 | 校验方式 |
| ---- | -------- | -------- |
| 1 | 重新执行 `sql/01_schema.sql` 与 `sql/02_data.sql`，重建 `express_station` 库 | 查询 14 张表，记录数与初始化脚本一致 |
| 2 | 核验 `parcel` 表记录数为 25、`ship_order` 表记录数为 8、`exception_record` 表记录数为 3、`notify_record` 表记录数为 29、`sms_code` 表记录数为 0 | 执行计数查询比对 |
| 3 | 核验 `notify_record` 的构成：`IN_STORE` 19 条、`OVERDUE` 4 条、`PICKUP_DONE` 5 条、`EXCEPTION` 1 条，其中 `send_status = 'FAILED'` 2 条 | 按 `notify_type`、`send_status` 分组计数比对 |
| 4 | 核验 `shelf.used_count` 与占用货位的快件数量一致 | 按 `shelf_id` 分组统计 `deleted = 0` 且 `status IN ('IN_STORE','DELIVERING','EXCEPTION')` 的快件数并比对 |
| 5 | 核验 `sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission` 的初始数据未被用例污染 | 比对角色数 2（ADMIN、STAFF）、权限数 43（菜单类 21、按钮类 22）、用户数 3（admin / staff01 / staff02）、用户角色关联 3 条、角色权限关联 72 条（ADMIN 43 / STAFF 29） |
| 6 | 清除测试过程中新增的测试快件、测试用户、测试货位与测试通知，并清空验证码表 `sms_code` | 全表检索测试标识（如 tester01）应无结果；`notify_record` 条数回到 29 条；`sms_code` 条数为 0（执行 `DELETE FROM sms_code;`） |

---

## 6 测试结论模板

### 6.1 结论内容

1. 本轮共设计功能测试用例 106 条、接口测试用例 73 条（合计 179 条），实际执行 ____ 条，通过 ____ 条，失败 ____ 条，阻塞 ____ 条，用例通过率为 ____%。
2. 缺陷统计：致命 ____ 个，严重 ____ 个，一般 ____ 个，提示 ____ 个；已修复 ____ 个，遗留 ____ 个。
3. 核心业务功能（收件登记、取件核销、异常件处理）测试结果为 ____，事务一致性与货位计数经数据库比对 ____ 偏差。
4. 权限控制测试结果为 ____，两类登录角色（`ADMIN`、`STAFF`）的菜单级与按钮级权限隔离 ____ 有效，未发现越权访问与越权查询；收件人不设账号，其查件由免登录接口按入参限定。
5. 结论：系统功能实现与《软件需求规格说明书》《前后端接口契约》____ 一致，达到毕业设计验收标准，可以进入答辩演示与文档整理阶段。

### 6.2 遗留问题与改进建议（模板）

| 序号 | 遗留问题 | 影响范围 | 建议处理方式 |
| ---- | -------- | -------- | ------------ |
| 1 | ____ | ____ | ____ |
| 2 | ____ | ____ | ____ |
| 3 | ____ | ____ | ____ |

### 6.3 签署

| 角色 | 姓名 | 日期 | 备注 |
| ---- | ---- | ---- | ---- |
| 测试人员 | ____ | ____ | 负责用例执行与缺陷登记 |
| 开发人员 | 吴宇航 | ____ | 负责缺陷修复与回归 |
| 指导教师 | ____ | ____ | 负责验收确认 |
