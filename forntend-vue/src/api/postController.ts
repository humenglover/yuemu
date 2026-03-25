// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addPost POST /api/post/add */
export async function addPostUsingPost(body: API.PostAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/post/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deletePost POST /api/post/delete/${param0} */
export async function deletePostUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePostUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/post/delete/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** deletePostDraft DELETE /api/post/draft/${param0} */
export async function deletePostDraftUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePostDraftUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { draftId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/post/draft/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getPostLatestDraft GET /api/post/draft/latest */
export async function getPostLatestDraftUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponsePostVO_>('/api/post/draft/latest', {
    method: 'GET',
    ...(options || {}),
  })
}

/** listPostDrafts GET /api/post/draft/list */
export async function listPostDraftsUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPostVO_>('/api/post/draft/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** savePostDraft POST /api/post/draft/save */
export async function savePostDraftUsingPost(
  body: API.PostAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/post/draft/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getFollowPosts POST /api/post/follow */
export async function getFollowPostsUsingPost(
  body: API.PostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePostVO_>('/api/post/follow', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getPostById GET /api/post/get/${param0} */
export async function getPostByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPostByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponsePostVO_>(`/api/post/get/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** likePost POST /api/post/like/${param0} */
export async function likePostUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likePostUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/post/like/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listPostByPage POST /api/post/list/page */
export async function listPostByPageUsingPost(
  body: API.PostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePostVO_>('/api/post/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listMyPosts POST /api/post/my/list */
export async function listMyPostsUsingPost(
  body: API.PostQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePostVO_>('/api/post/my/list', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** checkPostPermission GET /api/post/permission/check */
export async function checkPostPermissionUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkPostPermissionUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/post/permission/check', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** setPostPermission POST /api/post/permission/set */
export async function setPostPermissionUsingPost(
  body: API.PostPermissionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/post/permission/set', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** reviewPost POST /api/post/review/${param0} */
export async function reviewPostUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.reviewPostUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/post/review/${param0}`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** listPostTagCategory GET /api/post/tag_category */
export async function listPostTagCategoryUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponsePostTagCategory_>('/api/post/tag_category', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getTop100Post GET /api/post/top100/${param0} */
export async function getTop100PostUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTop100PostUsingGETParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseListPostVO_>(`/api/post/top100/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** updatePost POST /api/post/update */
export async function updatePostUsingPost(body: API.Post, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/post/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
