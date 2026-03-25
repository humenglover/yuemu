// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** check GET /api/wx/check */
export async function checkUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<string>('/api/wx/check', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** handleMessage POST /api/wx/check */
export async function handleMessageUsingPost(options?: { [key: string]: any }) {
  return request<string>('/api/wx/check', {
    method: 'POST',
    ...(options || {}),
  })
}
