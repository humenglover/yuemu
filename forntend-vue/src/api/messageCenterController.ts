// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getMessageList GET /api/message/list */
export async function getMessageListUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMessageListUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageObject_>('/api/message/list', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 10
      pageSize: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** markSingleAsRead POST /api/message/read/${param1}/${param0} */
export async function markSingleAsReadUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.markSingleAsReadUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, type: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/message/read/${param1}/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** markAllAsRead POST /api/message/read/all */
export async function markAllAsReadUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/message/read/all', {
    method: 'POST',
    ...(options || {}),
  })
}

/** getAllUnreadCount GET /api/message/unread/count/all */
export async function getAllUnreadCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMessageCenterVO_>('/api/message/unread/count/all', {
    method: 'GET',
    ...(options || {}),
  })
}
