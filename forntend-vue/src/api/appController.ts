// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** downloadApp GET /api/app/download */
export async function downloadAppUsingGet(options?: { [key: string]: any }) {
  return request<any>('/api/app/download', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getVersionHistory GET /api/app/history */
export async function getVersionHistoryUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getVersionHistoryUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageAppVersion_>('/api/app/history', {
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

/** uploadNewVersion POST /api/app/upload */
export async function uploadNewVersionUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadNewVersionUsingPOSTParams,
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

  return request<API.BaseResponseBoolean_>('/api/app/upload', {
    method: 'POST',
    params: {
      ...params,
    },
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}

/** getLatestVersion GET /api/app/version */
export async function getLatestVersionUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject_>('/api/app/version', {
    method: 'GET',
    ...(options || {}),
  })
}
