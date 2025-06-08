// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** sendMessage POST /api/message/add */
export async function sendMessageUsingPost(body: API.AddMessage, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/message/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteMessage POST /api/message/delete */
export async function deleteMessageUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteMessageUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/message/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getTop500 POST /api/message/getTop500 */
export async function getTop500UsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMessageVO_>('/api/message/getTop500', {
    method: 'POST',
    ...(options || {}),
  })
}

/** listMessageByPage POST /api/message/list/page */
export async function listMessageByPageUsingPost(
  body: API.MessageQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMessage_>('/api/message/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
