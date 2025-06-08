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
