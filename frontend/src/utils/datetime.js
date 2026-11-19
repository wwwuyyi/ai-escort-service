/**
 * 统一时间展示（BUG-043）：避免页面直接渲染后端 ISO 字符串。
 */
export function formatDateTime(value) {
  if (!value) return '-'
  const s = String(value)
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  if (m) return `${m[1]}-${m[2]}-${m[3]} ${m[4]}:${m[5]}`
  const d = new Date(s)
  if (!Number.isNaN(d.getTime())) {
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  }
  return s
}

export function formatDate(value) {
  if (!value) return '-'
  const s = String(value)
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]}` : s
}
