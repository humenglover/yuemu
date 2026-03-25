// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** batchOperationShares POST /api/share/batch */
export async function batchOperationSharesUsingPost(
  body: API.ShareBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/share/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** doShare POST /api/share/do */
export async function doShareUsingPost(body: API.ShareRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/share/do', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getShareById GET /api/share/get */
export async function getShareByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getShareByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseShareRecord_>('/api/share/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getShareHistory POST /api/share/history */
export async function getShareHistoryUsingPost(
  body: API.ShareQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageShareRecordVO_>('/api/share/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listSharesByPage POST /api/share/list/page/admin */
export async function listSharesByPageUsingPost(
  body: API.ShareAdminRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageShareRecord_>('/api/share/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getMyShareHistory POST /api/share/my/history */
export async function getMyShareHistoryUsingPost(
  body: API.ShareQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageShareRecordVO_>('/api/share/my/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getShareStatus GET /api/share/status/${param1}/${param0} */
export async function getShareStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getShareStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  const { targetId: param0, targetType: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/share/status/${param1}/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getUnreadShares GET /api/share/unread */
export async function getUnreadSharesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListShareRecordVO_>('/api/share/unread', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getUnreadSharesCount GET /api/share/unread/count */
export async function getUnreadSharesCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/share/unread/count', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateShare POST /api/share/update */
export async function updateShareUsingPost(
  body: API.ShareRecord,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/share/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
