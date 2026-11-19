const DOC_TYPE_LABELS = {
  LAB_REPORT: '检验单',
  PRESCRIPTION: '处方',
  MEDICAL_RECORD: '病历'
}

export function docTypeLabel(type) {
  return DOC_TYPE_LABELS[type] || type || '单据'
}
