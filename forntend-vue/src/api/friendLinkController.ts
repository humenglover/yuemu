// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addFriendLink POST /api/friend-link/add */
export async function addFriendLinkUsingPost(
  body: API.FriendLink,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/friend-link/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** increaseClickCount POST /api/friend-link/click/${param0} */
export async function increaseClickCountUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.increaseClickCountUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/friend-link/click/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** countByType GET /api/friend-link/count/type */
export async function countByTypeUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.countByTypeUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/friend-link/count/type', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteFriendLink POST /api/friend-link/delete/${param0} */
export async function deleteFriendLinkUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteFriendLinkUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/friend-link/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getFriendLinkById GET /api/friend-link/get/${param0} */
export async function getFriendLinkByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getFriendLinkByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseFriendLink_>(`/api/friend-link/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listFriendLinksByPage GET /api/friend-link/list/page */
export async function listFriendLinksByPageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listFriendLinksByPageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFriendLink_>('/api/friend-link/list/page', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listRecommendFriendLinks GET /api/friend-link/recommend */
export async function listRecommendFriendLinksUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listRecommendFriendLinksUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListFriendLink_>('/api/friend-link/recommend', {
    method: 'GET',
    params: {
      // limit has a default value: 10
      limit: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** refreshCache POST /api/friend-link/refresh-cache */
export async function refreshCacheUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/friend-link/refresh-cache', {
    method: 'POST',
    ...(options || {}),
  })
}

/** reviewFriendLink POST /api/friend-link/review/${param0} */
export async function reviewFriendLinkUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.reviewFriendLinkUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/friend-link/review/${param0}`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** listSiteTypes GET /api/friend-link/site-types */
export async function listSiteTypesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListMapStringString_>('/api/friend-link/site-types', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateFriendLink POST /api/friend-link/update */
export async function updateFriendLinkUsingPost(
  body: API.FriendLink,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/friend-link/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateWeight POST /api/friend-link/weight/${param0} */
export async function updateWeightUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateWeightUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/friend-link/weight/${param0}`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}
