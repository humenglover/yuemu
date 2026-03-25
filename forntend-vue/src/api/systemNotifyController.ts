// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** listSystemNotifies GET /api/notifies */
export async function listSystemNotifiesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listSystemNotifiesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemNotify_>('/api/notifies', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** addSystemNotify POST /api/notifies */
export async function addSystemNotifyUsingPost(
  body: API.SystemNotifyAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/notifies', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSystemNotifyById GET /api/notifies/${param0} */
export async function getSystemNotifyByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSystemNotifyByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseSystemNotify_>(`/api/notifies/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** updateSystemNotify PUT /api/notifies/${param0} */
export async function updateSystemNotifyUsingPut(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateSystemNotifyUsingPUTParams,
  body: API.SystemNotifyUpdateRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseSystemNotify_>(`/api/notifies/${param0}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** deleteSystemNotify DELETE /api/notifies/${param0} */
export async function deleteSystemNotifyUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteSystemNotifyUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/notifies/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listUserNotifies GET /api/notifies/user/notifies */
export async function listUserNotifiesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listUserNotifiesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemNotify_>('/api/notifies/user/notifies', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserNotifyById GET /api/notifies/user/notifies/${param0} */
export async function getUserNotifyByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserNotifyByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseSystemNotify_>(`/api/notifies/user/notifies/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** markAsRead PUT /api/notifies/user/notifies/${param0}/read */
export async function markAsReadUsingPut(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.markAsReadUsingPUTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/notifies/user/notifies/${param0}/read`, {
    method: 'PUT',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listUserReadNotifies GET /api/notifies/user/notifies/read-history */
export async function listUserReadNotifiesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listUserReadNotifiesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSystemNotify_>('/api/notifies/user/notifies/read-history', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserUnreadCount GET /api/notifies/user/notifies/unread-count */
export async function getUserUnreadCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/notifies/user/notifies/unread-count', {
    method: 'GET',
    ...(options || {}),
  })
}
