/**
 * 格式化文件大小
 * @param bytes 字节数
 * @returns 格式化后的文件大小字符串
 */
export function formatSize(bytes?: string | number): string {
  if (!bytes) return '0 B'

  const num = typeof bytes === 'string' ? parseInt(bytes) : bytes
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = num
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  return `${size.toFixed(2)} ${units[unitIndex]}`
}
