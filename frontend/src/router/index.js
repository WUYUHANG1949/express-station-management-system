import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import Layout from '@/layout/index.vue'
import { useUserStore } from '@/stores/user'

/**
 * 静态路由表（与契约第 8 节「前端页面与权限对照」一一对应）
 * meta.title  ：面包屑与浏览器标题
 * meta.icon   ：菜单图标名（动态菜单由 /auth/menus 返回，此处仅作为兜底与面包屑使用）
 * meta.perm   ：访问该页面所需的权限标识；为空表示登录即可访问
 * meta.hidden ：是否在菜单中隐藏（详情类页面）
 * meta.keepAlive：是否需要缓存组件实例
 */
export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true, hidden: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', public: true, hidden: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', public: true, hidden: true }
  },
  {
    // 主布局：所有业务页面均为其子路由
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页概览', icon: 'Odometer', perm: 'dashboard', keepAlive: true }
      },
      // ---------------- 快件管理 ----------------
      {
        path: 'parcel/list',
        name: 'ParcelList',
        component: () => import('@/views/parcel/list.vue'),
        meta: { title: '快件查询', icon: 'Search', perm: 'parcel:list', keepAlive: true }
      },
      {
        path: 'parcel/in',
        name: 'ParcelIn',
        component: () => import('@/views/parcel/in.vue'),
        meta: { title: '收件登记', icon: 'Download', perm: 'parcel:in' }
      },
      {
        path: 'parcel/pickup',
        name: 'ParcelPickup',
        component: () => import('@/views/parcel/pickup.vue'),
        meta: { title: '取件核销', icon: 'Finished', perm: 'parcel:pickup' }
      },
      {
        path: 'parcel/overdue',
        name: 'ParcelOverdue',
        component: () => import('@/views/parcel/overdue.vue'),
        meta: { title: '逾期催取', icon: 'AlarmClock', perm: 'parcel:overdue', keepAlive: true }
      },
      // ---------------- 寄件管理 ----------------
      {
        path: 'ship/list',
        name: 'ShipList',
        component: () => import('@/views/ship/list.vue'),
        meta: { title: '寄件查询', icon: 'Van', perm: 'ship:list', keepAlive: true }
      },
      // ---------------- 异常件管理 ----------------
      {
        path: 'exception/list',
        name: 'ExceptionList',
        component: () => import('@/views/exception/list.vue'),
        meta: { title: '异常件管理', icon: 'Warning', perm: 'exception:list', keepAlive: true }
      },
      // ---------------- 数据统计 ----------------
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/index.vue'),
        meta: { title: '数据统计', icon: 'DataAnalysis', perm: 'stats:view' }
      },
      // ---------------- 系统管理 ----------------
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', icon: 'User', perm: 'system:user:list', keepAlive: true }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色权限', icon: 'Key', perm: 'system:role:list' }
      },
      {
        path: 'system/station',
        name: 'SystemStation',
        component: () => import('@/views/system/station.vue'),
        meta: { title: '驿站管理', icon: 'OfficeBuilding', perm: 'system:station:list', keepAlive: true }
      },
      {
        path: 'system/shelf',
        name: 'SystemShelf',
        component: () => import('@/views/system/shelf.vue'),
        meta: { title: '货位管理', icon: 'Grid', perm: 'system:shelf:list', keepAlive: true }
      },
      // ---------------- 通知管理 ----------------
      {
        path: 'notify/list',
        name: 'NotifyList',
        component: () => import('@/views/notify/list.vue'),
        meta: { title: '通知记录', icon: 'ChatDotSquare', perm: 'notify:list', keepAlive: true }
      },
      // ---------------- 货位地图 ----------------
      {
        path: 'shelf-map',
        name: 'ShelfMap',
        component: () => import('@/views/shelf/map.vue'),
        meta: { title: '货位地图', icon: 'MapLocation', perm: 'shelfmap:view', keepAlive: true }
      },
      // ---------------- 个人中心 ----------------
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'UserFilled', perm: 'profile' }
      }
    ]
  },
  {
    // 数据大屏：脱离主布局全屏展示，需要 screen 权限
    path: '/screen',
    name: 'DataScreen',
    component: () => import('@/views/screen/index.vue'),
    meta: { title: '数据大屏', perm: 'screen', hidden: true }
  },
  {
    // 收件人自助查询取件码：免登录，脱离主布局
    path: '/query',
    name: 'PickupQuery',
    component: () => import('@/views/query/index.vue'),
    meta: { title: '取件码查询', public: true, hidden: true }
  },
  {
    // 404 兜底，必须放在最后
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', public: true, hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/** 免登录白名单 */
const WHITE_LIST = ['/login', '/register', '/403', '/404']

/**
 * 收集目标路由上声明的全部权限标识
 * @param {Object} route 目标路由对象
 * @returns {Array<string>}
 */
function collectRoutePerms(route) {
  const perms = []
  ;(route.matched || []).forEach((record) => {
    if (record.meta && record.meta.perm) {
      perms.push(record.meta.perm)
    }
  })
  return perms
}

/**
 * 全局前置守卫
 * 规则（按契约与需求）：
 * 1. 无 token 且目标不在白名单 → 跳 /login，并带上 redirect 参数
 * 2. 已登录访问 /login 或 /register → 跳 /dashboard
 * 3. 已登录但用户信息未加载（例如刷新页面）→ 先请求 /auth/me 恢复登录态
 * 4. 目标路由所需的 perm 不在该用户 permissions 中 → 跳 /403 并提示无权限
 */
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // 浏览器标题
  document.title = to.meta && to.meta.title ? `${to.meta.title} - 快件收发管理系统` : '快件收发管理系统'

  const hasToken = !!userStore.token

  // 规则 1：未登录
  if (!hasToken) {
    if (WHITE_LIST.includes(to.path) || to.meta.public) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    return
  }

  // 规则 2：已登录访问登录/注册页
  if (to.path === '/login' || to.path === '/register') {
    next({ path: '/dashboard' })
    return
  }

  // 规则 3：恢复登录态（刷新页面后 Pinia 状态为空）
  if (!userStore.loaded) {
    try {
      await userStore.fetchUserInfo()
    } catch (e) {
      // token 失效或后端不可用：清理登录态并回到登录页
      userStore.resetState()
      ElMessage.error('登录状态已失效，请重新登录')
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  // 规则 4：权限校验
  const requiredPerms = collectRoutePerms(to)
  const allowed = requiredPerms.every((perm) => userStore.hasPerm(perm))
  if (!allowed) {
    ElMessage.error('无访问权限，请联系管理员')
    next({ path: '/403' })
    return
  }

  next()
})

/* ------------------------------------------------------------------
 * 动态模块加载失败的自愈
 *
 * 背景：页面组件是懒加载的。如果开发服务器重启过、或部署后发布了新版本而旧页面
 * 仍开着，浏览器会拿着已失效的模块地址去请求，报
 *   "Failed to fetch dynamically imported module" / "Loading chunk xxx failed"
 * 此时 Vue Router 无法渲染目标组件，界面表现为：
 *   左侧菜单、顶部栏、面包屑都正常，只有**内容区一片空白**。
 *
 * 处理：捕获这类错误 → 用带时间戳的新地址强制重新加载（绕过浏览器缓存），
 * 并用 sessionStorage 记录次数，避免服务器真的挂了时无限刷新。
 * ---------------------------------------------------------------- */
const CHUNK_ERROR_PATTERN =
  /Loading chunk .* failed|Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module|Outdated Optimize Dep|504 \(Outdated Optimize Dep\)/i

/** 单次会话内允许的自动刷新次数上限，防止死循环 */
const MAX_AUTO_RELOAD = 2
const RELOAD_FLAG = 'es_auto_reload_count'

router.onError((error) => {
  const message = (error && error.message) || ''

  if (CHUNK_ERROR_PATTERN.test(message)) {
    let count = 0
    try {
      count = Number(sessionStorage.getItem(RELOAD_FLAG) || 0)
    } catch (e) {
      count = MAX_AUTO_RELOAD // 隐私模式下拿不到 sessionStorage，不自动刷新
    }

    if (count < MAX_AUTO_RELOAD) {
      try {
        sessionStorage.setItem(RELOAD_FLAG, String(count + 1))
      } catch (e) {
        /* 忽略写入异常 */
      }
      ElMessage.warning('页面资源已更新，正在重新加载…')
      // 加时间戳强制绕过缓存，重新拿到最新的入口与模块清单
      window.setTimeout(() => {
        const url = new URL(window.location.href)
        url.searchParams.set('_v', String(Date.now()))
        window.location.replace(url.toString())
      }, 500)
      return
    }

    // 已经自动刷新过仍失败：说明不是缓存问题，明确提示用户手动处理
    ElMessage.error('页面资源加载失败，请按 Ctrl + F5 强制刷新，或关闭标签页重新打开')
    console.error('[router error] chunk 加载失败且自动刷新无效', error)
    return
  }

  // 其它路由级错误：打印出来方便定位，不影响用户继续操作
  console.error('[router error]', error)
})

/**
 * 正常导航后清空刷新计数，让下一次出现问题时仍可自愈
 */
router.afterEach(() => {
  try {
    sessionStorage.removeItem(RELOAD_FLAG)
  } catch (e) {
    /* 忽略 */
  }
})

export default router
