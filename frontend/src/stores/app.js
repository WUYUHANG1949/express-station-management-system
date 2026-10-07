import { defineStore } from 'pinia'

/** 侧边栏折叠状态本地缓存 key */
const COLLAPSE_KEY = 'es_sidebar_collapse'
/** 标签页 / 面包屑等界面偏好缓存 key */
const STATION_KEY = 'es_filter_station'

/**
 * 应用界面状态仓库
 * 负责：侧边栏折叠、全局驿站筛选（统计页与各列表页共享）
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    /** 侧边栏是否折叠 */
    sidebarCollapsed: localStorage.getItem(COLLAPSE_KEY) === '1',
    /** 全局驿站筛选（null 表示全部驿站） */
    filterStationId: localStorage.getItem(STATION_KEY) ? Number(localStorage.getItem(STATION_KEY)) : null
  }),

  actions: {
    /** 切换侧边栏折叠状态并持久化 */
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(COLLAPSE_KEY, this.sidebarCollapsed ? '1' : '0')
    },

    /** 设置侧边栏折叠状态 */
    setSidebar(collapsed) {
      this.sidebarCollapsed = !!collapsed
      localStorage.setItem(COLLAPSE_KEY, this.sidebarCollapsed ? '1' : '0')
    },

    /** 设置全局驿站筛选条件 */
    setFilterStation(stationId) {
      this.filterStationId = stationId === '' || stationId === undefined ? null : stationId
      if (this.filterStationId === null) {
        localStorage.removeItem(STATION_KEY)
      } else {
        localStorage.setItem(STATION_KEY, String(this.filterStationId))
      }
    }
  }
})
