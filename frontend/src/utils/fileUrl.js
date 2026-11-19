import api from '@/api/request'

/**
 * 获取上传文件的短期签名 URL（REG-002）。
 * - JWT 只通过 axios 的 Authorization 头传递，不进入 URL；
 * - 后端先做对象级授权（REG-001），返回 5 分钟有效的签名下载地址；
 * - 图片 <img> 直接使用签名 URL，无需再携带登录 Token。
 */
export async function signedFileUrl(path) {
  if (!path) return ''
  if (/^(https?:)?\/\//i.test(path) || path.startsWith('blob:') || path.startsWith('data:')) {
    return path
  }
  if (path.startsWith('/api/file/download?')) return path

  // 不做跨请求缓存：签名 URL 绑定当前用户，避免同一标签页切换账号后复用旧授权
  const res = await api.get('/file/signed-url', { params: { path } })
  return res.data?.url || ''
}

/** el-upload 等原生上传组件需要的鉴权请求头 */
export function authHeaders() {
  const token = sessionStorage.getItem('token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}
