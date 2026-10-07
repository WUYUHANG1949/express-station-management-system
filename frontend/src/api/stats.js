/**
 * 统计模块接口（契约第 6 节）
 */
import request from '@/utils/request'

/** 概览统计：GET /api/stats/overview?stationId= */
export function getOverview(params) {
  return request({
    url: '/stats/overview',
    method: 'get',
    params
  })
}

/** 近 N 日出入库趋势：GET /api/stats/trend?days=7&stationId= → { dates, inCounts, pickupCounts } */
export function getTrend(params) {
  return request({
    url: '/stats/trend',
    method: 'get',
    params
  })
}

/** 快递公司分布：GET /api/stats/company?stationId= → [{ name, value }] */
export function getCompanyStats(params) {
  return request({
    url: '/stats/company',
    method: 'get',
    params
  })
}

/** 快件类型分布：GET /api/stats/parcel-type?stationId= → [{ name, value }] */
export function getParcelTypeStats(params) {
  return request({
    url: '/stats/parcel-type',
    method: 'get',
    params
  })
}

/** 驿站业务量排行（仅 ADMIN）：GET /api/stats/station-rank */
export function getStationRank() {
  return request({
    url: '/stats/station-rank',
    method: 'get'
  })
}

/* ==================== v1.1 新增 ==================== */

/**
 * 数据大屏聚合数据：GET /api/stats/screen?stationId=
 * 一次请求返回概览、14 日趋势、公司分布、类型分布、最近出入库、逾期 TOP、今日通知量
 */
export function getScreenData(stationId) {
  return request({
    url: '/stats/screen',
    method: 'get',
    params: { stationId }
  })
}
