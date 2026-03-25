// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addUserFollows POST /api/userfollows/adduserfollows */
export async function addUserFollowsUsingPost(
  body: API.UserFollowsAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/userfollows/adduserfollows', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** findIsFollow POST /api/userfollows/findisfollow */
export async function findIsFollowUsingPost(
  body: API.UserFollowsIsFollowsRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/userfollows/findisfollow', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getFollowAndFansCount POST /api/userfollows/getfollowandfanscount/${param0} */
export async function getFollowAndFansCountUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getFollowAndFansCountUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseFollowersAndFansVO_>(
    `/api/userfollows/getfollowandfanscount/${param0}`,
    {
      method: 'POST',
      params: { ...queryParams },
      ...(options || {}),
    }
  )
}

/** getFollowOrFanList POST /api/userfollows/getfolloworfanlist */
export async function getFollowOrFanListUsingPost(
  body: API.UserfollowsQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>('/api/userfollows/getfolloworfanlist', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getFollowOrFanPicture POST /api/userfollows/getfolloworfanpicture */
export async function getFollowOrFanPictureUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/userfollows/getfolloworfanpicture', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
