import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import relativeTime from 'dayjs/plugin/relativeTime'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/**
 * 格式化时间
 * @param time 时间戳或日期字符串
 * @param type 格式化类型：'relative' | 'date' | 'full'
 * @returns 格式化后的时间字符串
 */
export const formatTime = (time: string | number | Date | undefined, type: 'relative' | 'date' | 'full' = 'relative'): string => {
  if (!time) return '未知时间'

  const date = dayjs(time)
  const now = dayjs()

  // 如果是未来时间，根据是否跨年显示不同格式
  if (date.isAfter(now)) {
    if (date.year() === now.year()) {
      return date.format('MM月DD日 HH:mm')
    }
    return date.format('YYYY年MM月DD日 HH:mm')
  }

  switch (type) {
    case 'relative':
      // 如果是今天
      if (date.isSame(now, 'day')) {
        return date.format('HH:mm')
      }
      // 如果是昨天
      if (date.isSame(now.subtract(1, 'day'), 'day')) {
        return '昨天 ' + date.format('HH:mm')
      }
      // 如果是前天
      if (date.isSame(now.subtract(2, 'day'), 'day')) {
        return '前天 ' + date.format('HH:mm')
      }
      // 如果是本周
      if (date.isSame(now, 'week')) {
        return date.format('dddd HH:mm')
      }
      // 如果是今年
      if (date.isSame(now, 'year')) {
        return date.format('MM月DD日 HH:mm')
      }
      // 其他情况
      return date.format('YYYY年MM月DD日 HH:mm')

    case 'date':
      // 如果不是今年，显示年份
      if (!date.isSame(now, 'year')) {
        return date.format('YYYY年MM月DD日')
      }
      return date.format('MM月DD日')

    case 'full':
      return date.format('YYYY年MM月DD日 HH:mm:ss')

    default:
      return date.format('YYYY-MM-DD HH:mm:ss')
  }
}

/**
 * 格式化消息时间
 * @param date 日期
 * @returns 格式化后的时间字符串
 */
export const formatMessageTime = (date: Date | string | number | undefined): string => {
  if (!date) return ''

  const now = dayjs()
  const target = dayjs(date)

  if (target.isSame(now, 'day')) {
    // 当天消息只显示时间
    return target.format('HH:mm')
  } else if (target.isSame(now.subtract(1, 'day'), 'day')) {
    // 昨天的消息显示"昨天"
    return '昨天'
  } else if (target.isSame(now, 'year')) {
    // 今年的消息显示月日
    return target.format('MM-DD')
  } else {
    // 往年的消息显示年月日
    return target.format('YYYY-MM-DD')
  }
}

/**
 * 获取相对时间
 * @param date 日期
 * @returns 相对时间字符串
 */
export const getRelativeTime = (date: Date | string | number): string => {
  return dayjs(date).fromNow()
}
