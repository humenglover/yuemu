// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addTag POST /api/tag/add */
export async function addTagUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.addTagUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/tag/add', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteTag POST /api/tag/delete */
export async function deleteTagUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteTagUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/tag/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listTagVOByPage POST /api/tag/list/page/vo */
export async function listTagVoByPageUsingPost(
  body: API.PageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageTagVO_>('/api/tag/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** searchTag POST /api/tag/search */
export async function searchTagUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.searchTagUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListTagVO_>('/api/tag/search', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
