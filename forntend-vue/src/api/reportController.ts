// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addReport POST /api/report/add */
export async function addReportUsingPost(
  body: API.ReportAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/report/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteReport POST /api/report/delete */
export async function deleteReportUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/report/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getReportById GET /api/report/get */
export async function getReportByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getReportByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseReportVO_>('/api/report/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listReportByPage POST /api/report/list/page */
export async function listReportByPageUsingPost(
  body: API.ReportQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReportVO_>('/api/report/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listMyReportByPage POST /api/report/my/list/page */
export async function listMyReportByPageUsingPost(
  body: API.ReportQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReportVO_>('/api/report/my/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateReport POST /api/report/update */
export async function updateReportUsingPost(
  body: API.ReportUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/report/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
