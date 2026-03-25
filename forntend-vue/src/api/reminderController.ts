// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addReminder POST /api/reminder/add */
export async function addReminderUsingPost(
  body: API.ReminderAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/reminder/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteReminder POST /api/reminder/delete/${param0} */
export async function deleteReminderUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteReminderUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/reminder/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** toggleImportant POST /api/reminder/important/${param0} */
export async function toggleImportantUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleImportantUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/reminder/important/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listReminderByPage POST /api/reminder/list/page */
export async function listReminderByPageUsingPost(
  body: API.ReminderQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReminderVO_>('/api/reminder/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** toggleStarred POST /api/reminder/star/${param0} */
export async function toggleStarredUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleStarredUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/reminder/star/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getTodayReminders POST /api/reminder/today */
export async function getTodayRemindersUsingPost(
  body: API.ReminderQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageReminderVO_>('/api/reminder/today', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** toggleReminder POST /api/reminder/toggle/${param0} */
export async function toggleReminderUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.toggleReminderUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/reminder/toggle/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}
