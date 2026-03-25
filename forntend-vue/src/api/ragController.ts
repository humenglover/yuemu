// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** chat POST /api/rag/chat */
export async function chatUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagChatResponse_>('/api/rag/chat', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** chatStream GET /api/rag/chat/stream */
export async function chatStreamUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.chatStreamUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>('/api/rag/chat/stream', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** clearContext POST /api/rag/clearContext */
export async function clearContextUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/rag/clearContext', {
    method: 'POST',
    ...(options || {}),
  })
}
