/**
 * 三端共用的订单状态展示映射。
 * 只负责标签文案与 el-tag type，不参与任何业务判断。
 */
export const ORDER_STATUS_MAP = {
  PENDING_DISPATCH: { label: '待派单', type: 'warning' },
  DISPATCHED: { label: '已派单', type: 'info' },
  ACCEPTED: { label: '已接单', type: 'primary' },
  IN_SERVICE: { label: '服务中', type: 'primary' },
  SERVICE_FINISHED: { label: '服务完成', type: 'success' },
  SETTLED: { label: '待结算', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

export function getOrderStatus(status) {
  return ORDER_STATUS_MAP[status] || { label: status || '未知状态', type: 'info' }
}

export function getOrderStatusLabel(status) {
  return getOrderStatus(status).label
}

export function getOrderStatusType(status) {
  return getOrderStatus(status).type
}

/**
 * 全局固定状态色板：
 * 进行中 → 深蓝；完成/通过 → 浅绿；待处理/待结算 → 浅橙；取消/驳回/禁用 → 浅红。
 */
export function getOrderStatusClass(status) {
  const tone = {
    PENDING_DISPATCH: 'pending',
    DISPATCHED: 'progress',
    ACCEPTED: 'progress',
    IN_SERVICE: 'progress',
    SERVICE_FINISHED: 'done',
    SETTLED: 'pending',
    COMPLETED: 'done',
    CANCELLED: 'danger'
  }[status] || 'neutral'
  return `status-tag status-tag--${tone}`
}
