/**
 * /upload/** 需要登录后访问（BUG-006）。
 * 图片标签无法携带 Authorization 请求头，这里统一在 URL 上附带登录令牌；
 * 后端 UploadAuthInterceptor 同时支持 Authorization 与 ?token= 两种方式。
 */
export function fileUrl(path) {
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('blob:') || path.startsWith('data:')) {
    return path
  }
  const token = sessionStorage.getItem('token')
  if (!token) return path
  const sep = path.includes('?') ? '&' : '?'
  return `${path}${sep}token=${encodeURIComponent(token)}`
}

/** el-upload 等原生上传组件需要的鉴权请求头 */
export function authHeaders() {
  const token = sessionStorage.getItem('token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}
