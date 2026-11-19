import DOMPurify from 'dompurify'

/**
 * AI 生成 HTML 统一消毒（BUG-031）。
 * 只允许基础排版标签，不保留任何属性，因此 on* 事件、style、javascript: 等均被清除。
 * 禁止 script/style/iframe/object/embed/img/svg 等危险或非排版标签。
 */
const ALLOWED_TAGS = [
  'h1', 'h2', 'h3', 'h4',
  'p', 'ul', 'ol', 'li',
  'strong', 'em', 'b', 'i',
  'br', 'blockquote', 'div', 'span'
]

const FORBID_TAGS = [
  'script', 'style', 'iframe', 'object', 'embed',
  'form', 'input', 'button', 'textarea', 'select',
  'link', 'meta', 'base', 'svg', 'math', 'img', 'video', 'audio', 'canvas'
]

export function sanitizeHtml(html) {
  if (html === null || html === undefined) return ''
  return DOMPurify.sanitize(String(html), {
    ALLOWED_TAGS,
    ALLOWED_ATTR: [],
    FORBID_TAGS,
    FORBID_ATTR: ['style'],
    ALLOW_DATA_ATTR: false
  })
}
