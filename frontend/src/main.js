import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import App from './App.vue'
import router from './router'
import pinia from './stores'
import permissionDirective from './directives/permission'
import './styles/index.css'

/**
 * 应用入口
 * - 完整引入 Element Plus 与全部图标组件（不使用 unplugin 自动导入，规避构建风险）
 * - 注册 Pinia、Vue Router、全局按钮权限指令 v-perm
 */
const app = createApp(App)

// 注册 Element Plus 全部图标为全局组件，供菜单与管理页面通过组件名动态使用
Object.entries(ElementPlusIconsVue).forEach(([key, component]) => {
  app.component(key, component)
})

app.use(pinia)
app.use(router)
// 中文语言包（分页、日期选择器等内置文案）
app.use(ElementPlus, { locale: zhCn })
app.use(permissionDirective)

app.mount('#app')
