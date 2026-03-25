import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

// 创建 markdown-it 实例
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return '' // 使用默认的转义
  }
})

// 配置 markdown-it 选项
md.set({
  breaks: true, // 转换换行符为 <br>
})

export function parseMarkdown(text: string): string {
  return md.render(text)
}

// 移除HTML标签的函数，用于预览
export function stripHtml(html: string): string {
  return html.replace(/<[^>]*>/g, '')
}
