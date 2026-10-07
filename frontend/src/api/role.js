/**
 * 角色与权限模块接口（契约 7.2 / 7.3）
 */
import request from '@/utils/request'

/** 全部启用角色：GET /api/roles/list（用于下拉框） */
export function listRoles() {
  return request({
    url: '/roles/list',
    method: 'get'
  })
}

/** 角色分页：GET /api/roles/page */
export function pageRoles(params) {
  return request({
    url: '/roles/page',
    method: 'get',
    params
  })
}

/** 新增角色：POST /api/roles */
export function createRole(data) {
  return request({
    url: '/roles',
    method: 'post',
    data
  })
}

/** 编辑角色：PUT /api/roles/{id} */
export function updateRole(id, data) {
  return request({
    url: `/roles/${id}`,
    method: 'put',
    data
  })
}

/** 删除角色：DELETE /api/roles/{id} */
export function deleteRole(id) {
  return request({
    url: `/roles/${id}`,
    method: 'delete'
  })
}

/** 查询角色已有权限 id 数组：GET /api/roles/{id}/permissions */
export function getRolePermissions(id) {
  return request({
    url: `/roles/${id}/permissions`,
    method: 'get'
  })
}

/** 重新分配角色权限：PUT /api/roles/{id}/permissions，body { permIds: [] } */
export function assignRolePermissions(id, permIds) {
  return request({
    url: `/roles/${id}/permissions`,
    method: 'put',
    data: { permIds }
  })
}

/** 权限树：GET /api/permissions/tree?type=MENU|ALL（默认 ALL） */
export function getPermissionTree(params) {
  return request({
    url: '/permissions/tree',
    method: 'get',
    params
  })
}
