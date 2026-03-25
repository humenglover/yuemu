// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getSpaceCategoryAnalyze POST /api/space/analyze/category */
export async function getSpaceCategoryAnalyzeUsingPost(
  body: API.SpaceCategoryAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceCategoryAnalyzeResponse_>('/api/space/analyze/category', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceRankAnalyze POST /api/space/analyze/rank */
export async function getSpaceRankAnalyzeUsingPost(
  body: API.SpaceRankAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpace_>('/api/space/analyze/rank', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceSizeAnalyze POST /api/space/analyze/size */
export async function getSpaceSizeAnalyzeUsingPost(
  body: API.SpaceSizeAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceSizeAnalyzeResponse_>('/api/space/analyze/size', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceTagAnalyze POST /api/space/analyze/tag */
export async function getSpaceTagAnalyzeUsingPost(
  body: API.SpaceTagAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceTagAnalyzeResponse_>('/api/space/analyze/tag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceUsageAnalyze POST /api/space/analyze/usage */
export async function getSpaceUsageAnalyzeUsingPost(
  body: API.SpaceUsageAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpaceUsageAnalyzeResponse_>('/api/space/analyze/usage', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceUserAnalyze POST /api/space/analyze/user */
export async function getSpaceUserAnalyzeUsingPost(
  body: API.SpaceUserAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceUserAnalyzeResponse_>('/api/space/analyze/user', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
