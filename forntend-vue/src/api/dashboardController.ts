// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getChartStats GET /api/dashboard/charts */
export async function getChartStatsUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseChartVO_>('/api/dashboard/charts', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getDashboardStats GET /api/dashboard/stats */
export async function getDashboardStatsUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseDashboardVO_>('/api/dashboard/stats', {
    method: 'GET',
    ...(options || {}),
  })
}
