// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** searchAll POST /api/search/all */
export async function searchAllUsingPost(
  body: API.SearchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageObject_1>('/api/search/all', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getGuessYouWantToSearch GET /api/search/guess */
export async function getGuessYouWantToSearchUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getGuessYouWantToSearchUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListHotSearchVO_>('/api/search/guess', {
    method: 'GET',
    params: {
      // size has a default value: 10
      size: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserSearchHistory GET /api/search/history */
export async function getUserSearchHistoryUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserSearchHistoryUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListUserSearchRecord_>('/api/search/history', {
    method: 'GET',
    params: {
      // size has a default value: 10
      size: '10',

      ...params,
    },
    ...(options || {}),
  })
}

/** deleteUserSearchHistory DELETE /api/search/history */
export async function deleteUserSearchHistoryUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteUserSearchHistoryUsingDELETEParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/search/history', {
    method: 'DELETE',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getHotSearchKeywords GET /api/search/hot */
export async function getHotSearchKeywordsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHotSearchKeywordsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListHotSearchVO_>('/api/search/hot', {
    method: 'GET',
    params: {
      // size has a default value: 9
      size: '9',
      ...params,
    },
    ...(options || {}),
  })
}
