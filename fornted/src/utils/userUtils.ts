import { DEFAULT_AVATAR_URLS } from '@/constants/avatar'

/**
 * 获取默认头像
 * @param userName 用户名
 * @returns 默认头像URL
 */
export const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'

  // 使用用户名生成一个确定性的索引
  let hash = 0
  for (let i = 0; i < defaultName.length; i++) {
    hash = ((hash << 5) - hash) + defaultName.charCodeAt(i)
    hash = hash & hash // Convert to 32-bit integer
  }

  // 确保索引在有效范围内
  const index = Math.abs(hash) % DEFAULT_AVATAR_URLS.length

  return DEFAULT_AVATAR_URLS[index]
}
