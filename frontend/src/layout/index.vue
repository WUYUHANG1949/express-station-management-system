<template>
  <!--
    主布局
    结构：左侧可折叠菜单（数据来自 /api/auth/menus） + 顶部面包屑/用户下拉 + 主内容区（keep-alive）
  -->
  <el-container class="layout">
    <!-- ==================== 左侧菜单 ==================== -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout__aside">
      <!-- 品牌区 -->
      <div class="layout__brand" :class="{ 'is-collapse': isCollapse }" @click="goHome">
        <img class="layout__brand-logo" src="/favicon.svg" alt="logo" />
        <transition name="fade">
          <span v-show="!isCollapse" class="layout__brand-text">快件收发管理系统</span>
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
          background-color="#1f2d3d"
          text-color="#c0c4cc"
          active-text-color="#ffffff"
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
    </el-aside>

    <el-container class="layout__main">
      <!-- ==================== 顶部栏 ==================== -->
      <el-header class="layout__header" height="56px">
        <div class="layout__header-left">
          <!-- 折叠按钮 -->
          <el-icon class="layout__collapse-btn" @click="appStore.toggleSidebar()">
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
          <!-- 当前驿站标识 -->
          <el-tag v-if="userStore.stationName" type="info" effect="plain" class="layout__station-tag">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="layout__station-text">{{ userStore.stationName }}</span>
          </el-tag>

          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="layout__user">
              <el-avatar :size="30" :src="userStore.avatar || ''">
                {{ (userStore.realName || 'U').slice(0, 1) }}
              </el-avatar>
              <span class="layout__user-name">{{ userStore.realName }}</span>
              <el-tag v-if="roleTag" size="small" type="warning" effect="dark">{{ roleTag }}</el-tag>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile" :icon="UserFilled">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- ==================== 主内容区 ==================== -->
      <el-main class="layout__content">
        <router-view v-slot="{ Component, route }">
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
 * 职责：动态菜单渲染、面包屑、用户下拉、内容区 keep-alive 缓存
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as ElIcons from '@element-plus/icons-vue'
import { ArrowDown, Expand, Fold, OfficeBuilding, Odometer, SwitchButton, UserFilled } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { ROLE_CODE_NAME } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

/** 侧边栏是否折叠 */
const isCollapse = computed(() => appStore.sidebarCollapsed)

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

/**
 * 当前高亮的菜单项
 * 取当前路由的 path；对于详情类子页面回退到父级列表页
 */
const activeMenu = computed(() => route.path)

/** 面包屑：取当前匹配路由链上的 title */
const breadcrumbs = computed(() => {
  const list = (route.matched || [])
    .filter((item) => item.meta && item.meta.title)
    .map((item) => item.meta.title)
  // 去掉重复的「首页」
  return list.filter((title, index) => !(index === 0 && title === '首页'))
})

/** 当前用户角色标签 */
const roleTag = computed(() => {
  const role = (userStore.roles || [])[0]
  return role ? ROLE_CODE_NAME[role] || role : ''
})

/**
 * keep-alive 的组件名白名单
 * 约定：需要缓存的页面组件通过 defineOptions({ name: 'xxx' }) 显式声明组件名，
 * 且与路由 name 保持一致（例如 ParcelList / ShipList / ExceptionList / SystemUser / SystemStation / SystemShelf / Dashboard），
 * 这里直接取路由 name 列表，避免多个 list.vue 因同名而互相顶替。
 * @returns {Array<string>}
 */
const keepAliveNames = computed(() =>
  router
    .getRoutes()
    .filter((r) => r.meta && r.meta.keepAlive && r.name)
    .map((r) => String(r.name))
)

/** 点击品牌回到首页 */
function goHome() {
  router.push('/dashboard')
}

/**
 * 用户下拉命令处理
 * @param {string} command profile | logout
 */
async function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
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

/** 布局挂载后拉取菜单（路由守卫已保证登录态有效） */
onMounted(async () => {
  try {
    await userStore.fetchMenus()
  } catch (e) {
    // 菜单拉取失败不阻塞页面渲染，已由响应拦截器统一提示
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

/* ---------------- 左侧 ---------------- */
.layout__aside {
  display: flex;
  flex-direction: column;
  background-color: #1f2d3d;
  transition: width 0.25s ease;
  overflow: hidden;
}

.layout__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  flex-shrink: 0;
}

.layout__brand.is-collapse {
  justify-content: center;
  padding: 0;
}

.layout__brand-logo {
  width: 26px;
  height: 26px;
  flex-shrink: 0;
}

.layout__brand-text {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.layout__menu-scroll {
  flex: 1;
  overflow-x: hidden;
}

.layout__aside :deep(.el-menu) {
  border-right: none;
}

.layout__aside :deep(.el-menu-item.is-active) {
  background-color: #2563eb !important;
}

.layout__aside :deep(.el-menu-item:hover),
.layout__aside :deep(.el-sub-menu__title:hover) {
  background-color: #2b3a4d !important;
}

/* ---------------- 顶部 ---------------- */
.layout__main {
  background-color: #f0f2f5;
  overflow: hidden;
}

.layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.layout__header-left,
.layout__header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.layout__collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #5a6a7a;
}

.layout__collapse-btn:hover {
  color: #2563eb;
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

.layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 6px;
  cursor: pointer;
  outline: none;
}

.layout__user:hover {
  background-color: #f5f7fa;
}

.layout__user-name {
  font-size: 14px;
  color: #303133;
}

/* ---------------- 内容区 ---------------- */
.layout__content {
  padding: 16px;
  overflow-y: auto;
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
  transition: all 0.25s ease;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>
