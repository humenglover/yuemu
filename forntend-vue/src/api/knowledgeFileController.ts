// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** batchDeleteKnowledgeFiles POST /api/knowledgeFile/batch-delete */
export async function batchDeleteKnowledgeFilesUsingPost(
  body: number[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/knowledgeFile/batch-delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getKnowledgeFileVOById GET /api/knowledgeFile/get/vo */
export async function getKnowledgeFileVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getKnowledgeFileVOByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseKnowledgeFileVO_>('/api/knowledgeFile/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listKnowledgeFileVOByPage POST /api/knowledgeFile/list/page/vo */
export async function listKnowledgeFileVoByPageUsingPost(
  body: API.KnowledgeFileQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageKnowledgeFileVO_>('/api/knowledgeFile/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** syncKnowledgeFiles POST /api/knowledgeFile/sync */
export async function syncKnowledgeFilesUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/knowledgeFile/sync', {
    method: 'POST',
    ...(options || {}),
  })
}

/** uploadKnowledgeFile POST /api/knowledgeFile/upload */
export async function uploadKnowledgeFileUsingPost(
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

  return request<API.BaseResponseKnowledgeFile_>('/api/knowledgeFile/upload', {
    method: 'POST',
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}
