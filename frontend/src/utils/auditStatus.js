/**
 * 资质审核状态展示映射，与订单状态共用同一套固定色板。
 */
export const AUDIT_STATUS_MAP = {
  PENDING: { label: '待审核', tone: 'pending' },
  APPROVED: { label: '已通过', tone: 'done' },
  REJECTED: { label: '已驳回', tone: 'danger' }
}

export function getAuditStatusLabel(status) {
  return AUDIT_STATUS_MAP[status]?.label || status || '-'
}

export function getAuditStatusClass(status) {
  const tone = AUDIT_STATUS_MAP[status]?.tone || 'neutral'
  return `status-tag status-tag--${tone}`
}
