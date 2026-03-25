// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addLoveBoard POST /api/love-board/add */
export async function addLoveBoardUsingPost(body: API.LoveBoard, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/love-board/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** batchOperationLoveBoards POST /api/love-board/batch */
export async function batchOperationLoveBoardsUsingPost(
  body: API.LoveBoardBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteLoveBoard POST /api/love-board/delete */
export async function deleteLoveBoardUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteLoveBoardUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getLoveBoardById GET /api/love-board/get */
export async function getLoveBoardByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLoveBoardByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoveBoard_>('/api/love-board/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getLoveBoardByIdAdmin GET /api/love-board/get/admin */
export async function getLoveBoardByIdAdminUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLoveBoardByIdAdminUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoveBoard_>('/api/love-board/get/admin', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listLoveBoardsByPage POST /api/love-board/list/page/admin */
export async function listLoveBoardsByPageUsingPost(
  body: API.LoveBoardAdminRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLoveBoard_>('/api/love-board/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPublicLoveBoards GET /api/love-board/list/public */
export async function listPublicLoveBoardsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPublicLoveBoardsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLoveBoard_>('/api/love-board/list/public', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',

      // size has a default value: 15
      size: '15',
      // sortField has a default value: viewCount
      sortField: 'viewCount',
      // sortOrder has a default value: desc
      sortOrder: 'desc',
      ...params,
    },
    ...(options || {}),
  })
}

/** getMyLoveBoard GET /api/love-board/my */
export async function getMyLoveBoardUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoveBoard_>('/api/love-board/my', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateLoveBoard POST /api/love-board/update */
export async function updateLoveBoardUsingPost(
  body: API.LoveBoard,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateLoveBoardAdmin POST /api/love-board/update/admin */
export async function updateLoveBoardAdminUsingPost(
  body: API.LoveBoard,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/update/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
