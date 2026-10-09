<template>
  <!--
    主布局
    结构：左侧深蓝渐变侧边栏（菜单来自 /api/auth/menus）
         + 顶部栏（折叠、面包屑、逾期提醒、大屏入口、用户下拉）
         + 主内容区（keep-alive + 过渡动画 + 渲染错误兜底）
  -->
  <el-container class="layout">
    <!-- ==================== 左侧侧边栏 ==================== -->
    <el-aside :width="isCollapse ? '64px' : '232px'" class="layout__aside">
      <!-- 品牌区 -->
      <div class="layout__brand" :class="{ 'is-collapse': isCollapse }" @click="goHome">
        <div class="layout__brand-logo">
          <el-icon :size="18"><Van /></el-icon>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="layout__brand-info">
            <div class="layout__brand-text">快件收发管理系统</div>
            <div class="layout__brand-sub">Express Station Platform</div>
          </div>
        </transition>
      </div>

      <!-- 动态菜单：由 /api/auth/menus 返回的菜单树渲染 -->
      <el-scrollbar class="layout__menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          unique-opened
          router
          class="layout__menu"
        >
          <template v-for="menu in menuTree" :key="menu.path || menu.permCode">
            <!-- 有子菜单：渲染 el-sub-menu -->
            <el-sub-menu v-if="hasChildren(menu)" :index="menu.path || menu.permCode">
              <template #title>
                <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
                <span>{{ menu.permName }}</span>
              </template>
              <el-menu-item
                v-for="child in visibleChildren(menu)"
                :key="child.path"
                :index="child.path"
              >
                <el-icon><component :is="resolveIcon(child.icon)" /></el-icon>
                <template #title>{{ child.permName }}</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 无子菜单：直接渲染 el-menu-item -->
            <el-menu-item v-else :index="menu.path">
              <el-icon><component :is="resolveIcon(menu.icon)" /></el-icon>
              <template #title>{{ menu.permName }}</template>
            </el-menu-item>
          </template>

          <!-- 菜单为空兜底（例如后端未返回菜单时，至少保证首页可点） -->
          <el-menu-item v-if="!menuTree.length" index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <template #title>首页概览</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>

      <!-- 侧边栏底部：折叠开关 + 版本号 -->
      <div class="layout__aside-footer">
        <div class="layout__collapse" @click="appStore.toggleSidebar()">
          <el-icon><Expand v-if="isCollapse" /><Fold v-else /></el-icon>
          <span v-show="!isCollapse" class="layout__collapse-text">收起菜单</span>
        </div>
        <div v-show="!isCollapse" class="layout__version">毕业设计 · v1.1.0</div>
      </div>
    </el-aside>

    <el-container class="layout__main">
      <!-- ==================== 顶部栏 ==================== -->
      <el-header class="layout__header" height="58px">
        <div class="layout__header-left">
          <el-icon class="layout__header-icon" @click="appStore.toggleSidebar()">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>

          <!-- 面包屑 -->
          <el-breadcrumb separator="/" class="layout__breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="index">
              {{ item }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="layout__header-right">
          <!-- 逾期件提醒：点击直达逾期催取页 -->
          <el-tooltip v-if="overdueCount > 0" content="存在逾期未取快件，点击去催取" placement="bottom">
            <div class="layout__badge" @click="goOverdue">
              <el-icon :size="18"><BellFilled /></el-icon>
              <span class="layout__badge-dot">{{ overdueCount > 99 ? '99+' : overdueCount }}</span>
            </div>
          </el-tooltip>

          <!-- 数据大屏入口（有 screen 权限才显示） -->
          <el-tooltip v-if="hasScreenPerm" content="进入数据大屏" placement="bottom">
            <div class="layout__header-icon" @click="goScreen">
              <el-icon :size="18"><Monitor /></el-icon>
            </div>
          </el-tooltip>

          <!-- 当前驿站标识 -->
          <el-tag v-if="userStore.stationName" type="info" effect="plain" class="layout__station-tag">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="layout__station-text">{{ userStore.stationName }}</span>
          </el-tag>

          <el-divider direction="vertical" />

          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="layout__user">
              <div class="layout__avatar">
                <img v-if="userStore.avatar" :src="userStore.avatar" alt="avatar" />
                <span v-else>{{ (userStore.realName || 'U').slice(0, 1) }}</span>
              </div>
              <div class="layout__user-meta">
                <div class="layout__user-name">{{ userStore.realName }}</div>
                <div class="layout__user-role">{{ roleTag }}</div>
              </div>
              <el-icon class="layout__user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="UserFilled">个人中心</el-dropdown-item>
                <el-dropdown-item command="screen" :icon="Monitor">数据大屏</el-dropdown-item>
                <el-dropdown-item command="query" :icon="Search">取件码自助查询</el-dropdown-item>
                <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- ==================== 主内容区 ==================== -->
      <el-main class="layout__content app-main">
        <!-- 子组件渲染异常时的兜底：避免整块内容区空白 -->
        <el-result
          v-if="renderError"
          icon="error"
          title="页面加载失败"
          class="layout__error"
        >
          <template #sub-title>
            <p class="layout__error-msg">{{ renderError }}</p>
            <p class="layout__error-tip">
              如果刚刚更新过代码或重启过开发服务器，浏览器可能仍在使用旧的页面资源。
              点「强制刷新」重新获取即可，也可以直接按 <b>Ctrl + F5</b>。
            </p>
          </template>
          <template #extra>
            <el-button type="primary" @click="hardReload">强制刷新</el-button>
            <el-button @click="goDashboardFromError">返回首页</el-button>
          </template>
        </el-result>

        <router-view v-else v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="keepAliveNames">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 主布局组件
 * 职责：动态菜单渲染、面包屑、逾期提醒、大屏入口、用户下拉、内容区缓存与异常兜底
 */
import { computed, onErrorCaptured, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as ElIcons from '@element-plus/icons-vue'
import {
  ArrowDown,
  BellFilled,
  Expand,
  Fold,
  Monitor,
  OfficeBuilding,
  Odometer,
  Search,
  SwitchButton,
  UserFilled,
  Van
} from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { getOverdueCount } from '@/api/parcel'
import { ROLE_CODE_NAME } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

/** 侧边栏是否折叠 */
const isCollapse = computed(() => appStore.sidebarCollapsed)

/** 待催取的逾期件数量，显示在顶部铃铛角标上 */
const overdueCount = ref(0)

/** 是否拥有数据大屏权限 */
const hasScreenPerm = computed(() => userStore.hasPerm('screen'))

/** 图标映射表：Element Plus 全量图标（组件名 -> 组件） */
const iconMap = ElIcons
/** 找不到 icon 字段对应组件时的兜底图标 */
const FALLBACK_ICON = 'Menu'

/**
 * 根据后端返回的 icon 字段名解析 Element Plus 图标组件
 * 找不到时返回兜底图标，避免渲染报错
 * @param {string} name 图标名，例如 'Box'
 * @returns {Object} 图标组件
 */
function resolveIcon(name) {
  if (name && iconMap[name]) return iconMap[name]
  return iconMap[FALLBACK_ICON] || iconMap.Odometer
}

/**
 * 过滤掉没有 path 的菜单项（例如纯按钮权限、仅用于分组的父节点）
 * 同时递归过滤子节点
 * @param {Array} list 菜单数组
 * @returns {Array}
 */
function normalizeMenus(list) {
  if (!Array.isArray(list)) return []
  return list
    .filter((item) => item && (item.path || (item.children && item.children.length)))
    .map((item) => ({
      ...item,
      children: normalizeMenus(item.children)
    }))
    .filter((item) => item.path || item.children.length)
}

/** 侧边栏菜单树 */
const menuTree = computed(() => normalizeMenus(userStore.menus))

/** 是否存在可显示的子菜单 */
function hasChildren(menu) {
  return Array.isArray(menu.children) && menu.children.length > 0
}

/** 子菜单（仅保留有 path 的项） */
function visibleChildren(menu) {
  return (menu.children || []).filter((child) => !!child.path)
}

/** 当前高亮的菜单项 */
const activeMenu = computed(() => route.path)

/** 面包屑：取当前匹配路由链上的 title */
const breadcrumbs = computed(() => {
  const list = (route.matched || [])
    .filter((item) => item.meta && item.meta.title)
    .map((item) => item.meta.title)
  return list.filter((title, index) => !(index === 0 && title === '首页'))
})

/** 当前用户角色标签 */
const roleTag = computed(() => {
  const role = (userStore.roles || [])[0]
  return role ? ROLE_CODE_NAME[role] || role : ''
})

/**
 * keep-alive 的组件名白名单：取路由 meta.keepAlive 的 name 列表，
 * 避免多个 list.vue 因组件同名而互相顶替
 */
const keepAliveNames = computed(() =>
  router
    .getRoutes()
    .filter((r) => r.meta && r.meta.keepAlive && r.name)
    .map((r) => String(r.name))
)

/* ------------------------------------------------------------------
 * 渲染异常兜底
 * 子组件（含异步路由组件加载失败）抛错时，Vue 默认会留下空白内容区，
 * 这里捕获后展示友好提示与「重新加载」按钮，避免出现"整页空白"的无措状态。
 * ---------------------------------------------------------------- */
const renderError = ref(null)

onErrorCaptured((err) => {
  renderError.value = (err && err.message) || '未知错误'
  // 返回 false 阻止错误继续向上传播，避免整个应用白屏
  return false
})

/** 强制刷新：加时间戳绕过浏览器缓存，重新获取入口与模块清单 */
function hardReload() {
  const url = new URL(window.location.href)
  url.searchParams.set('_v', String(Date.now()))
  window.location.replace(url.toString())
}

/** 从错误页返回首页：先清掉错误状态再导航，避免又被兜底拦住 */
function goDashboardFromError() {
  renderError.value = null
  router.push('/dashboard').catch(() => {
    window.location.href = '/dashboard'
  })
}

/** 点击品牌回到首页 */
function goHome() {
  router.push('/dashboard')
}

/** 前往逾期催取页 */
function goOverdue() {
  router.push('/parcel/overdue')
}

/** 前往数据大屏 */
function goScreen() {
  router.push('/screen')
}

/**
 * 用户下拉命令处理
 * @param {string} command profile | screen | query | logout
 */
async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'screen') {
    router.push('/screen')
    return
  }
  if (command === 'query') {
    // 公开查询页与登录态无关，新开标签避免打断当前作业
    window.open('/query', '_blank')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '退出确认', {
        confirmButtonText: '确定退出',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch (e) {
      return // 用户取消
    }
    await userStore.logout()
    ElMessage.success('已安全退出')
    router.replace('/login')
  }
}

/** 拉取逾期件数量（失败静默，不影响主流程） */
async function loadOverdueCount() {
  if (!userStore.hasPerm('parcel:list')) return
  try {
    overdueCount.value = Number(await getOverdueCount()) || 0
  } catch (e) {
    overdueCount.value = 0
  }
}

/** 布局挂载后拉取菜单与逾期提醒 */
onMounted(async () => {
  try {
    await userStore.fetchMenus()
  } catch (e) {
    // 菜单拉取失败不阻塞页面渲染，已由响应拦截器统一提示
  }
  loadOverdueCount()
})
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

/* ---------------- 左侧侧边栏 ---------------- */
.layout__aside {
  display: flex;
  flex-direction: column;
  background: var(--es-sidebar-gradient);
  transition: width 0.26s var(--es-ease);
  overflow: hidden;
  position: relative;
  box-shadow: 2px 0 16px rgba(7, 26, 58, 0.28);
  z-index: 20;
}

/* 侧边栏保持纯色渐变，不加装饰性光晕，避免"AI 生成感" */

.layout__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 58px;
  padding: 0 14px;
  cursor: pointer;
  flex-shrink: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.layout__brand.is-collapse {
  justify-content: center;
  padding: 0;
}

.layout__brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 9px;
  color: #fff;
  background: var(--es-brand-gradient);
  box-shadow: 0 4px 12px rgba(22, 211, 200, 0.32);
}

.layout__brand-info {
  overflow: hidden;
  white-space: nowrap;
}

.layout__brand-text {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.4px;
  line-height: 1.2;
}

.layout__brand-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.42);
  letter-spacing: 0.6px;
  text-transform: uppercase;
}

.layout__menu-scroll {
  flex: 1;
  overflow-x: hidden;
  position: relative;
  z-index: 1;
}

.layout__menu {
  border-right: none;
  background: transparent;
  padding: 8px;
}

/* 菜单项：透明底 + 悬停玻璃感 + 选中渐变药丸 */
.layout__aside :deep(.el-menu) {
  background: transparent;
}

.layout__aside :deep(.el-menu-item),
.layout__aside :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 3px 0;
  border-radius: 9px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.22s var(--es-ease);
}

.layout__aside :deep(.el-menu-item:hover),
.layout__aside :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.09) !important;
  color: #fff;
}

.layout__aside :deep(.el-menu-item.is-active) {
  background: var(--es-brand-gradient) !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 6px 16px rgba(26, 109, 255, 0.36);
}

/* 选中项左侧的青色指示条 */
.layout__aside :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 0 3px 3px 0;
  background: var(--es-teal-400);
}

.layout__aside :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #fff;
}

.layout__aside :deep(.el-sub-menu .el-menu-item) {
  min-width: auto;
  padding-left: 44px !important;
}

/* 折叠态下的弹出子菜单使用深色底，保持与侧边栏一致 */
.layout__aside :deep(.el-menu--collapse) {
  width: 48px;
}

.layout__aside-footer {
  flex-shrink: 0;
  padding: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.layout__collapse {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px;
  border-radius: 9px;
  color: rgba(255, 255, 255, 0.62);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s var(--es-ease);
}

.layout__collapse:hover {
  background-color: rgba(255, 255, 255, 0.09);
  color: #fff;
}

.layout__collapse-text {
  white-space: nowrap;
}

.layout__version {
  margin-top: 6px;
  padding-left: 10px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  white-space: nowrap;
}

/* ---------------- 顶部栏 ---------------- */
.layout__main {
  background: transparent;
  overflow: hidden;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  background: #ffffff;
  border-bottom: 1px solid var(--es-border);
  z-index: 10;
}

.layout__header-left,
.layout__header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout__header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  color: var(--es-text-2);
  cursor: pointer;
  transition: all 0.2s var(--es-ease);
}

.layout__header-icon:hover {
  background: #f2f4f7;
  color: var(--es-primary);
}

/* 逾期提醒铃铛 */
.layout__badge {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 9px;
  color: var(--el-color-danger);
  cursor: pointer;
  transition: all 0.2s var(--es-ease);
}

.layout__badge:hover {
  background: rgba(240, 68, 56, 0.1);
}

.layout__badge-dot {
  position: absolute;
  top: 1px;
  right: 0;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--el-color-danger);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
  box-shadow: 0 0 0 2px #fff;
}

.layout__breadcrumb {
  font-size: 14px;
}

.layout__station-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.layout__station-text {
  margin-left: 4px;
}

/* 用户信息块 */
.layout__user {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 8px 4px 4px;
  border-radius: 10px;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s var(--es-ease);
}

.layout__user:hover {
  background-color: #f3f7ff;
}

.layout__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  overflow: hidden;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  background: var(--es-brand-gradient);
  flex-shrink: 0;
}

.layout__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.layout__user-meta {
  line-height: 1.25;
}

.layout__user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--es-text-1);
}

.layout__user-role {
  font-size: 11px;
  color: var(--es-text-3);
}

.layout__user-arrow {
  color: var(--es-text-3);
  font-size: 12px;
}

/* ---------------- 内容区 ---------------- */
.layout__content {
  padding: 18px;
  overflow-y: auto;
}

.layout__error {
  margin-top: 60px;
}

.layout__error-msg {
  margin: 0 0 10px;
  color: var(--el-color-danger);
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  word-break: break-all;
}

.layout__error-tip {
  max-width: 560px;
  margin: 0 auto;
  color: var(--es-text-3);
  font-size: 13px;
  line-height: 1.7;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.26s var(--es-ease);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
