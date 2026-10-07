/**
 * 通知模块接口（v1.1 新增）
 * 对应后端 NotifyController：/api/notifications/**
 */
import request from '@/utils/request'

/**
 * 通知记录分页查询
 * GET /api/notifications/page?pageNum&pageSize&waybillNo&receiverPhone&notifyType&channel&sendStatus&stationId&startTime&endTime
 */
export function pageNotifications(params) {
  return request({
    url: '/notifications/page',
    method: 'get',
    params
  })
}

/** 某快件的全部通知记录：GET /api/notifications/parcel/{parcelId} */
export function listNotificationsByParcel(parcelId) {
  return request({
    url: `/notifications/parcel/${parcelId}`,
    method: 'get'
  })
}

/**
 * 发送通知：POST /api/notifications
 * @param {{parcelId:number, notifyType:string, channel?:string, content?:string, receiverPhone?:string}} data
 */
export function sendNotification(data) {
  return request({
    url: '/notifications',
    method: 'post',
    data
  })
}

/**
 * 批量催取逾期件：POST /api/notifications/batch-overdue?stationId=&minDays=
 * 同一快件当天不会重复发送，返回实际发送条数
 */
export function batchNotifyOverdue(params) {
  return request({
    url: '/notifications/batch-overdue',
    method: 'post',
    params
  })
}

/** 待催取的逾期件数量：GET /api/notifications/overdue-pending?stationId= */
export function getOverduePending(stationId) {
  return request({
    url: '/notifications/overdue-pending',
    method: 'get',
    params: { stationId }
  })
}
