// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** batchOperationMessages POST /api/chat/batch */
export async function batchOperationMessagesUsingPost(
  body: API.ChatMessageBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/chat/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getMessageById GET /api/chat/get */
export async function getMessageByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMessageByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseChatMessage_>('/api/chat/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listMessagesByPage POST /api/chat/list/page/admin */
export async function listMessagesByPageUsingPost(
  body: API.ChatMessageAdminRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageChatMessage_>('/api/chat/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateMessage POST /api/chat/update */
export async function updateMessageUsingPost(
  body: API.ChatMessage,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/chat/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
