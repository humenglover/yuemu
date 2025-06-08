// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addWeiYan POST /api/weiyan/add */
export async function addWeiYanUsingPost(body: API.WeiYan, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/weiyan/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteWeiYan POST /api/weiyan/delete */
export async function deleteWeiYanUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteWeiYanUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/weiyan/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** likeWeiYan POST /api/weiyan/like */
export async function likeWeiYanUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likeWeiYanUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/weiyan/like', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listWeiYan GET /api/weiyan/list */
export async function listWeiYanUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listWeiYanUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageWeiYan_>('/api/weiyan/list', {
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

/** updateWeiYan POST /api/weiyan/update */
export async function updateWeiYanUsingPost(body: API.WeiYan, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/weiyan/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
