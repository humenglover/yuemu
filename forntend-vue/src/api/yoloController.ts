// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** detectObjects POST /api/yolo/detect */
export async function detectObjectsUsingPost(
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

  return request<API.BaseResponseYoloResponseDTO_>('/api/yolo/detect', {
    method: 'POST',
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}

/** detectObjectsByUrl POST /api/yolo/detect/url */
export async function detectObjectsByUrlUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.detectObjectsByUrlUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseYoloResponseDTO_>('/api/yolo/detect/url', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
