// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getQaAnswer POST /api/rag/qa/message/answer */
export async function getQaAnswerUsingPost(
  body: API.QaMessageQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagMessageVO_>('/api/rag/qa/message/answer', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** clearSessionContext POST /api/rag/qa/message/clearContext */
export async function clearSessionContextUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.clearSessionContextUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagMessageVO_>('/api/rag/qa/message/clearContext', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteMessageById POST /api/rag/qa/message/delete/admin */
export async function deleteMessageByIdUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/qa/message/delete/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteBatchMessages POST /api/rag/qa/message/delete/batch */
export async function deleteBatchMessagesUsingPost(
  body: API.DeleteRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/qa/message/delete/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getMessageById GET /api/rag/qa/message/get */
export async function getMessageByIdUsingGet1(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMessageByIdUsingGET1Params,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagMessageVO_>('/api/rag/qa/message/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listQaMessages GET /api/rag/qa/message/list */
export async function listQaMessagesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listQaMessagesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageRagMessageVO_>('/api/rag/qa/message/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listAllMessagesByPage POST /api/rag/qa/message/list/page */
export async function listAllMessagesByPageUsingPost(
  body: API.QaMessageQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageRagMessageVO_>('/api/rag/qa/message/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listMessagesBySessionId GET /api/rag/qa/message/list/session */
export async function listMessagesBySessionIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMessagesBySessionIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListRagMessageVO_>('/api/rag/qa/message/list/session', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** sendQaMessage POST /api/rag/qa/message/send */
export async function sendQaMessageUsingPost(
  body: API.QaMessageAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagMessageVO_>('/api/rag/qa/message/send', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** sendQaStreamMessage GET /api/rag/qa/message/send/stream */
export async function sendQaStreamMessageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.sendQaStreamMessageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.SseEmitter>('/api/rag/qa/message/send/stream', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
