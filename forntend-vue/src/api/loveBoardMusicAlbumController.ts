// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** createMusicAlbum POST /api/love-board/music-album/add */
export async function createMusicAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.createMusicAlbumUsingPOSTParams,
  body: API.LoveBoardMusicAlbum,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/love-board/music-album/add', {
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

/** deleteAudioFromAlbum POST /api/love-board/music-album/audio/delete */
export async function deleteAudioFromAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteAudioFromAlbumUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/audio/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** fetchAlbumAudios GET /api/love-board/music-album/audios */
export async function fetchAlbumAudiosUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.fetchAlbumAudiosUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListAudioFileVO_>('/api/love-board/music-album/audios', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** removeMusicAlbum POST /api/love-board/music-album/delete */
export async function removeMusicAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.removeMusicAlbumUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** fetchMusicAlbumById GET /api/love-board/music-album/get */
export async function fetchMusicAlbumByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.fetchMusicAlbumByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoveBoardMusicAlbum_>('/api/love-board/music-album/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** fetchMusicAlbums GET /api/love-board/music-album/list */
export async function fetchMusicAlbumsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.fetchMusicAlbumsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageLoveBoardMusicAlbum_>('/api/love-board/music-album/list', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** removeMusicAlbumPassword POST /api/love-board/music-album/password/remove */
export async function removeMusicAlbumPasswordUsingPost(
  body: API.MusicAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/password/remove', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** setMusicAlbumPassword POST /api/love-board/music-album/password/set */
export async function setMusicAlbumPasswordUsingPost(
  body: API.MusicAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/password/set', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** modifyMusicAlbumPassword POST /api/love-board/music-album/password/update */
export async function modifyMusicAlbumPasswordUsingPost(
  body: API.MusicAlbumPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/password/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** modifyMusicAlbum POST /api/love-board/music-album/update */
export async function modifyMusicAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.modifyMusicAlbumUsingPOSTParams,
  body: API.LoveBoardMusicAlbum,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/love-board/music-album/update', {
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

/** uploadMusicToAlbum POST /api/love-board/music-album/upload */
export async function uploadMusicToAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadMusicToAlbumUsingPOSTParams,
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData()

  if (file) {
    formData.append('file', file)
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele]

    if (item !== undefined && item !== null) {
      if (typeof item === 'object' && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ''))
        } else {
          formData.append(ele, JSON.stringify(item))
        }
      } else {
        formData.append(ele, item)
      }
    }
  })

  return request<API.BaseResponseAudioFileVO_>('/api/love-board/music-album/upload', {
    method: 'POST',
    params: {
      ...params,
    },
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}
