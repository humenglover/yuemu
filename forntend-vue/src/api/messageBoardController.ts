// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** deleteMessage DELETE /api/message-board/${param0} */
export async function deleteMessageUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteMessageUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/message-board/${param0}`, {
    method: 'DELETE',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** addMessage POST /api/message-board/add */
export async function addMessageUsingPost(
  body: API.MessageBoard,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/message-board/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** likeMessage POST /api/message-board/like/${param0} */
export async function likeMessageUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likeMessageUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/message-board/like/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listMessagesByPage GET /api/message-board/list/page */
export async function listMessagesByPageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMessagesByPageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageMessageBoard_>('/api/message-board/list/page', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',

      // size has a default value: 10
      size: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** updateMessageStatus POST /api/message-board/status */
export async function updateMessageStatusUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateMessageStatusUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/message-board/status', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
