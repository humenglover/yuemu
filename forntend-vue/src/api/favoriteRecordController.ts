// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addFavoriteRecord POST /api/favorite-record/add */
export async function addFavoriteRecordUsingPost(
  body: API.FavoriteRecordAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/favorite-record/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** cancelFavorite POST /api/favorite-record/cancel */
export async function cancelFavoriteUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.cancelFavoriteUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/favorite-record/cancel', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** checkHasFavorited GET /api/favorite-record/check */
export async function checkHasFavoritedUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkHasFavoritedUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/favorite-record/check', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getFavoriteHistory POST /api/favorite-record/history */
export async function getFavoriteHistoryUsingPost(
  body: API.FavoriteRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFavoriteRecordVO_>('/api/favorite-record/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getMyFavoriteHistory POST /api/favorite-record/my/history */
export async function getMyFavoriteHistoryUsingPost(
  body: API.FavoriteRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageFavoriteRecordVO_>('/api/favorite-record/my/history', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getUnreadFavorites GET /api/favorite-record/unread */
export async function getUnreadFavoritesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListFavoriteRecordVO_>('/api/favorite-record/unread', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getUnreadFavoritesCount GET /api/favorite-record/unread/count */
export async function getUnreadFavoritesCountUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/favorite-record/unread/count', {
    method: 'GET',
    ...(options || {}),
  })
}
