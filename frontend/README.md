# 快件收发管理系统 · 前端工程

> 毕业设计项目「面向快递驿站的快件收发管理系统」的前端部分。
> 接口约定以 `../docs/api-contract.md`（前后端唯一接口契约 v1.0）为准。

## 一、技术栈

| 分类 | 选型 |
| ---- | ---- |
| 框架 | Vue 3（`<script setup>` 组合式 API） |
| 构建 | Vite 5 |
| 语言 | JavaScript（不使用 TypeScript） |
| UI 组件库 | Element Plus（完整引入 + 全部图标 `@element-plus/icons-vue`） |
| 状态管理 | Pinia（token 持久化到 localStorage，key 为 `es_token`） |
| 路由 | Vue Router 4（history 模式 + 全局前置守卫） |
| HTTP | axios（统一封装，自动注入 `Authorization: Bearer <token>`） |
| 图表 | ECharts 5（按需引入折线图 / 饼图 / 柱状图） |

## 二、快速开始

```bash
# 1. 安装依赖（Node 建议 18+）
npm install

# 2. 启动开发服务器（默认 http://localhost:5173）
npm run dev

# 3. 生产构建，产物输出到 dist/
npm run build

# 4. 本地预览构建产物
npm run preview
```

开发环境下 Vite 已配置代理：`/api` → `http://localhost:8080`（**不做 rewrite**，因为后端接口路径本身即带 `/api` 前缀）。因此需要先启动后端服务（默认端口 8080）。

### 演示账号

| 账号 | 密码 | 角色 |
| ---- | ---- | ---- |
| `admin` | `123456` | 系统管理员（ADMIN） |
| `staff01` | `123456` | 驿站员工（STAFF） |
| `user01` | `123456` | 普通用户（USER） |

登录页已内置演示账号一键填充。

## 三、目录结构

```
frontend/
├── index.html                 入口 HTML
├── vite.config.js             Vite 配置（5173 端口 + /api 代理）
├── package.json               依赖与脚本（name: express-station-frontend）
├── public/favicon.svg         站点图标
└── src/
    ├── main.js                应用入口：注册 Element Plus / 图标 / Pinia / Router / v-perm
    ├── App.vue                根组件（含全局异常捕获）
    ├── api/                   按模块拆分的接口函数（与契约章节一一对应）
    │   ├── auth.js            认证（登录/注册/登出/当前用户/菜单树）
    │   ├── parcel.js          快件（入库/查询/核销/分页/详情/轨迹/编辑/删除/派送/试算/导出）
    │   ├── ship.js            寄件单
    │   ├── exception.js       异常件
    │   ├── stats.js           统计
    │   ├── user.js            用户（含本人资料与密码）
    │   ├── role.js            角色与权限树
    │   ├── station.js         驿站
    │   ├── shelf.js           货位
    │   └── index.js           统一出口（命名空间导出）
    ├── utils/
    │   ├── request.js         axios 封装：拦截器注入 token、解包 res.data、401/403 统一处理
    │   ├── dict.js            枚举字典与中文名映射（状态/类型/异常/取件方式等）
    │   ├── format.js          日期、金额、手机号等统一格式化工具
    │   └── download.js        blob 文件下载（台账导出）
    ├── stores/
    │   ├── index.js           Pinia 实例
    │   ├── user.js            登录态、权限集合、菜单树、hasPerm 权限判断
    │   └── app.js             侧边栏折叠、全局驿站筛选
    ├── router/index.js        路由表 + 全局前置守卫（登录校验 + 权限校验）
    ├── directives/permission.js  按钮权限指令 v-perm
    ├── layout/index.vue       主布局：动态菜单 + 面包屑 + 用户下拉 + keep-alive
    ├── components/
    │   ├── PageContainer.vue  页面容器（标题 + 操作区 + 卡片内容）
    │   ├── DataTable.vue      通用表格（loading / 空数据 / 分页统一处理）
    │   ├── StatCard.vue       统计指标卡片
    │   └── ChartBox.vue       ECharts 图表容器（实例管理 + resize + 空数据兜底）
    ├── styles/index.css       全局样式与通用类
    └── views/                 页面
        ├── login/index.vue            /login            登录
        ├── register/index.vue         /register         注册
        ├── dashboard/index.vue        /dashboard        首页概览
        ├── parcel/list.vue            /parcel/list      快件查询
        ├── parcel/in.vue              /parcel/in        收件登记
        ├── parcel/pickup.vue          /parcel/pickup    取件核销
        ├── ship/list.vue              /ship/list        寄件管理
        ├── exception/list.vue         /exception/list   异常件管理
        ├── stats/index.vue            /stats            数据统计
        ├── system/user.vue            /system/user      用户管理
        ├── system/role.vue            /system/role      角色权限
        ├── system/station.vue         /system/station   驿站管理
        ├── system/shelf.vue           /system/shelf     货位管理
        ├── profile/index.vue          /profile          个人中心
        ├── error/403.vue              无权限页
        └── error/404.vue              404 页面
```

## 四、路由与权限对照（对应契约第 8 节）

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

另有 `/403`（无权限）与 `/:pathMatch(.*)*`（404）兜底页。

## 五、路由守卫规则

1. 无 token 且目标路由不在白名单（`/login`、`/register`、`/403`、`/404`）→ 跳转 `/login`，并携带 `redirect` 参数，登录成功后自动回跳。
2. 已登录访问 `/login` 或 `/register` → 跳转 `/dashboard`。
3. 已登录但用户信息未加载（如刷新页面）→ 先请求 `GET /api/auth/me` 恢复登录态；失败则清理本地状态并回登录页。
4. 目标路由声明的 `meta.perm` 不在该用户 `permissions` 中 → 提示无权限并跳转 `/403`。

## 六、按钮权限指令 `v-perm`

```vue
<!-- 单个权限 -->
<el-button v-perm="'parcel:export'">导出台账</el-button>

<!-- 数组表示「任一权限满足即显示」 -->
<el-button v-perm="['parcel:edit', 'parcel:delete']">操作</el-button>
```

无权限时元素会被直接从 DOM 中移除。ADMIN 角色默认放行全部权限标识。
前端控制仅用于交互体验，真正的权限校验仍由后端完成。

## 七、约定与说明

- **统一响应解包**：`src/utils/request.js` 的响应拦截器已经解包后端 `{ code, message, data }`，业务代码 `await xxxApi()` 直接拿到 `data`。
- **异常提示**：`code=1001/400/404/500` 会由拦截器统一 `ElMessage` 提示；`401` 清理 token 并跳登录；`403` 提示无权限。
- **日期格式**：全站统一 `YYYY-MM-DD HH:mm:ss`；金额统一保留 2 位小数；查询条件中的日期范围统一拆分为 `YYYY-MM-DD` 的 `startTime` / `endTime`。
- **keep-alive**：需要缓存的页面组件通过 `defineOptions({ name: 'XxxYyy' })` 显式声明组件名，且与路由 `name` 保持一致（避免多个 `list.vue` 因同名互相顶替）。
- **字典唯一来源**：枚举值与中文名映射集中在 `src/utils/dict.js`，与契约保持一致；若后端调整枚举，需同步修改该文件与契约文档。
- **导出功能**：`/api/parcels/export` 使用 `responseType: 'blob'`，拦截器不再解包，由 `downloadBlob` 触发浏览器下载。
