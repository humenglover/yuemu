// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getChatHistory POST /api/deepseek/history */
export async function getChatHistoryUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getChatHistoryUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.PageAiChatVO_>('/api/deepseek/history', {
    method: 'POST',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 20
      pageSize: '20',
      ...params,
    },
    ...(options || {}),
  })
}

/** send POST /api/deepseek/send */
export async function sendUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.sendUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<string>('/api/deepseek/send', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
