// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addActivity POST /api/activity/add */
export async function addActivityUsingPost(
  body: API.ActivityAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/activity/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteActivity POST /api/activity/delete */
export async function deleteActivityUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteActivityUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/activity/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getActivityById GET /api/activity/get */
export async function getActivityByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getActivityByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseActivity_>('/api/activity/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listCarouselActivities POST /api/activity/list/carousel */
export async function listCarouselActivitiesUsingPost(
  body: API.ActivityQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageActivity_>('/api/activity/list/carousel', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listActivityByPage POST /api/activity/list/page */
export async function listActivityByPageUsingPost(
  body: API.ActivityQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageActivity_>('/api/activity/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** reviewActivity POST /api/activity/review */
export async function reviewActivityUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.reviewActivityUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/activity/review', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
