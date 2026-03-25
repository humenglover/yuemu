// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addViewRecord POST /api/view-record/add */
export async function addViewRecordUsingPost(
  body: API.ViewRecordAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/view-record/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** checkHasViewed GET /api/view-record/check */
export async function checkHasViewedUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkHasViewedUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/view-record/check', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deleteViewRecord POST /api/view-record/delete */
export async function deleteViewRecordUsingPost(body: string[], options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/view-record/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listMyViewRecordByPage POST /api/view-record/my/list/page */
export async function listMyViewRecordByPageUsingPost(
  body: API.ViewRecordQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageViewRecordVO_>('/api/view-record/my/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** reportViewDuration POST /api/view-record/report-duration */
export async function reportViewDurationUsingPost(
  body: API.ReportViewDurationRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/view-record/report-duration', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
