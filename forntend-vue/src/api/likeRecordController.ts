// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** batchOperationLikes POST /api/like/batch */
export async function batchOperationLikesUsingPost(
  body: API.LikeBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/like/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** doLike POST /api/like/do */
export async function doLikeUsingPost(body: API.LikeRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/like/do', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getLikeById GET /api/like/get/admin */
export async function getLikeByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLikeByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLikeRecord_>('/api/like/get/admin', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getLikeHistory POST /api/like/history */
export async function getLikeHistoryUsingPost(
  body: API.LikeQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLikeRecordVO_>('/api/like/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listLikesByPage POST /api/like/list/page/admin */
export async function listLikesByPageUsingPost(
  body: API.LikeAdminRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLikeRecord_>('/api/like/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getMyLikeHistory POST /api/like/my/history */
export async function getMyLikeHistoryUsingPost(
  body: API.LikeQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLikeRecordVO_>('/api/like/my/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getLikeStatus GET /api/like/status/${param1}/${param0} */
export async function getLikeStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getLikeStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  const { targetId: param0, targetType: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/like/status/${param1}/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getUnreadLikes GET /api/like/unread */
export async function getUnreadLikesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListLikeRecordVO_>('/api/like/unread', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getUnreadLikesCount GET /api/like/unread/count */
export async function getUnreadLikesCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/like/unread/count', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateLike POST /api/like/update/admin */
export async function updateLikeUsingPost(body: API.LikeRecord, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/like/update/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
