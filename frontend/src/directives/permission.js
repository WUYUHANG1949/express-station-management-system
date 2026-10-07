import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令 v-perm
 *
 * 用法：
 *   <el-button v-perm="'parcel:export'">导出台账</el-button>
 *   <el-button v-perm="['parcel:edit','parcel:delete']">编辑</el-button>  // 任一权限满足即可显示
 *   <el-button v-perm="'parcel:delete'">删除</el-button>                  // 无权限时元素被移除
 *
 * 实现：无权限时直接从 DOM 中移除元素（而非隐藏），避免用户通过开发者工具提交无权限操作。
 * 注意：前端隐藏只是体验优化，真正的权限校验仍由后端完成。
 */

/** 判断当前用户是否具备指令值声明的权限 */
function checkPermission(el, binding) {
  const userStore = useUserStore()
  const value = binding.value

  // 未传权限标识时不做限制
  if (value === undefined || value === null || value === '') return true

  return userStore.hasPerm(value)
}

export const permission = {
  mounted(el, binding) {
    if (!checkPermission(el, binding)) {
      // 移除元素本身；若存在父节点则从父节点移除
      if (el.parentNode) {
        el.parentNode.removeChild(el)
      } else {
        el.style.display = 'none'
      }
    }
  },
  updated(el, binding) {
    // 权限可能在运行期变化（例如切换账号后重新加载），此处做兜底处理
    if (!checkPermission(el, binding)) {
      if (el.parentNode) {
        el.parentNode.removeChild(el)
      } else {
        el.style.display = 'none'
      }
    }
  }
}

/**
 * 注册全局指令的插件形式，在 main.js 中 app.use(permissionDirective)
 */
export default {
  install(app) {
    app.directive('perm', permission)
  }
}
