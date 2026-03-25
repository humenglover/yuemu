import dayjs from 'dayjs'

/**
 * 格式化时间
 * @param time 时间
 * @param format 格式
 */
export const formatTime = (time: string | number | Date, format: string = 'YYYY-MM-DD') => {
  if (!time) {
    return ''
  }
  return dayjs(time).format(format)
}

/**
 * 格式化相对时间
 * @param time 时间
 */
export const formatRelativeTime = (time: string | number | Date) => {
  if (!time) {
    return ''
  }
  const now = dayjs()
  const target = dayjs(time)
  const diff = target.diff(now, 'second')

  if (diff < 0) {
    return '已过期'
  }

  if (diff < 60) {
    return `${diff}秒后`
  }

  if (diff < 3600) {
    return `${Math.floor(diff / 60)}分钟后`
  }

  if (diff < 86400) {
    return `${Math.floor(diff / 3600)}小时后`
  }

  return `${Math.floor(diff / 86400)}天后`
}

/**
 * 判断时间是否过期
 * @param time 时间
 */
export const isExpired = (time: string | number | Date) => {
  if (!time) {
    return true
  }
  return dayjs(time).isBefore(dayjs())
}
