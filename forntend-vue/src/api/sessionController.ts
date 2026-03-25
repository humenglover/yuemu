// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** createSession POST /api/rag/session/create */
export async function createSessionUsingPost(
  body: API.SessionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagSessionVO_>('/api/rag/session/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteSession POST /api/rag/session/delete */
export async function deleteSessionUsingPost(
  body: API.SessionDeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/session/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteSessionById POST /api/rag/session/delete/admin */
export async function deleteSessionByIdUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/session/delete/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteBatchSessions POST /api/rag/session/delete/batch */
export async function deleteBatchSessionsUsingPost(
  body: API.DeleteRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/session/delete/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSessionById GET /api/rag/session/get */
export async function getSessionByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSessionByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagSessionVO_>('/api/rag/session/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listSessions GET /api/rag/session/list */
export async function listSessionsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listSessionsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageRagSessionVO_>('/api/rag/session/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listSessionsByPage POST /api/rag/session/list/page */
export async function listSessionsByPageUsingPost(
  body: API.SessionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseIPageRagSessionVO_>('/api/rag/session/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** switchSession POST /api/rag/session/switch */
export async function switchSessionUsingPost(
  body: API.SessionSwitchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseRagSessionVO_>('/api/rag/session/switch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateSessionName POST /api/rag/session/updateName */
export async function updateSessionNameUsingPost(
  body: API.SessionQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/rag/session/updateName', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
