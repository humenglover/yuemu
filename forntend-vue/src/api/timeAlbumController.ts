// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addTimeAlbum POST /api/timeAlbum/add */
export async function addTimeAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.addTimeAlbumUsingPOSTParams,
  body: API.TimeAlbum,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/timeAlbum/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteTimeAlbum POST /api/timeAlbum/delete */
export async function deleteTimeAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteTimeAlbumUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getTimeAlbumById GET /api/timeAlbum/get */
export async function getTimeAlbumByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTimeAlbumByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseTimeAlbum_>('/api/timeAlbum/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteHeartWallPicture POST /api/timeAlbum/heart-wall/delete */
export async function deleteHeartWallPictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteHeartWallPictureUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/heart-wall/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getHeartWallPictures GET /api/timeAlbum/heart-wall/list */
export async function getHeartWallPicturesUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHeartWallPicturesUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPicture_>('/api/timeAlbum/heart-wall/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** uploadHeartWallPictures POST /api/timeAlbum/heart-wall/upload */
export async function uploadHeartWallPicturesUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadHeartWallPicturesUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPictureVO_>('/api/timeAlbum/heart-wall/upload', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listTimeAlbum GET /api/timeAlbum/list */
export async function listTimeAlbumUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listTimeAlbumUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageTimeAlbum_>('/api/timeAlbum/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** removeAlbumPassword POST /api/timeAlbum/password/remove */
export async function removeAlbumPasswordUsingPost(
  body: API.TimeAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/password/remove', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** setAlbumPassword POST /api/timeAlbum/password/set */
export async function setAlbumPasswordUsingPost(
  body: API.TimeAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/password/set', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateAlbumPassword POST /api/timeAlbum/password/update */
export async function updateAlbumPasswordUsingPost(
  body: API.TimeAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/password/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateTimeAlbum POST /api/timeAlbum/update */
export async function updateTimeAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateTimeAlbumUsingPOSTParams,
  body: API.TimeAlbum,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/timeAlbum/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: {
      ...params,
    },
    data: body,
    ...(options || {}),
  })
}
