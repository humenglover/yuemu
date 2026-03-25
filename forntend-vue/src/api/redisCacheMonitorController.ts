// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getRedisInfo GET /api/monitor/cache/info */
export async function getRedisInfoUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject_>('/api/monitor/cache/info', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getKeysStatistics GET /api/monitor/cache/keys */
export async function getKeysStatisticsUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject_>('/api/monitor/cache/keys', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getMemoryInfo GET /api/monitor/cache/memory */
export async function getMemoryInfoUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringObject_>('/api/monitor/cache/memory', {
    method: 'GET',
    ...(options || {}),
  })
}
