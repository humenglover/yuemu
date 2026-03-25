// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addBugReport POST /api/bugReport/add */
export async function addBugReportUsingPost(
  body: API.BugReportAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/bugReport/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** solveBugReport POST /api/bugReport/admin/solve */
export async function solveBugReportUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.solveBugReportUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/bugReport/admin/solve', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteBugReport POST /api/bugReport/delete */
export async function deleteBugReportUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/bugReport/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getBugReportById GET /api/bugReport/get */
export async function getBugReportByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getBugReportByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBugReportVO_>('/api/bugReport/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listBugReportByPage POST /api/bugReport/list/page */
export async function listBugReportByPageUsingPost(
  body: API.BugReportQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageBugReportVO_>('/api/bugReport/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listMyBugReportByPage POST /api/bugReport/my/list/page */
export async function listMyBugReportByPageUsingPost(
  body: API.BugReportQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageBugReportVO_>('/api/bugReport/my/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateBugReport POST /api/bugReport/update */
export async function updateBugReportUsingPost(
  body: API.BugReportUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/bugReport/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
