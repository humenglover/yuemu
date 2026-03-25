// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** clearUnreadCount POST /api/private_chat/clear_unread/${param1}/${param0} */
export async function clearUnreadCountUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.clearUnreadCountUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { isSender: param0, targetUserId: param1, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/private_chat/clear_unread/${param1}/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** createOrUpdatePrivateChat POST /api/private_chat/create_update */
export async function createOrUpdatePrivateChatUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.createOrUpdatePrivateChatUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePrivateChat_>('/api/private_chat/create_update', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** deletePrivateChat POST /api/private_chat/delete/${param0} */
export async function deletePrivateChatUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePrivateChatUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { privateChatId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/private_chat/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listPrivateChatByPage POST /api/private_chat/list/page */
export async function listPrivateChatByPageUsingPost(
  body: API.PrivateChatQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePrivateChat_>('/api/private_chat/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateChatName POST /api/private_chat/update_name/${param0} */
export async function updateChatNameUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateChatNameUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { privateChatId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/private_chat/update_name/${param0}`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** updateChatType POST /api/private_chat/update_type/${param0} */
export async function updateChatTypeUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateChatTypeUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { targetUserId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/private_chat/update_type/${param0}`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}
