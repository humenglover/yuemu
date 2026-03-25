// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addUser POST /api/user/add */
export async function addUserUsingPost(body: API.UserAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong_>('/api/user/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** addUserSignIn POST /api/user/add/sign_in */
export async function addUserSignInUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/user/add/sign_in', {
    method: 'POST',
    ...(options || {}),
  })
}

/** banOrUnbanUser POST /api/user/ban */
export async function banOrUnbanUserUsingPost(
  body: API.UserUnbanRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/ban', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** batchDeleteUser POST /api/user/batchDelete */
export async function batchDeleteUserUsingPost(body: number[], options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/user/batchDelete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userBindWx POST /api/user/bind/wx */
export async function userBindWxUsingPost(
  body: API.UserWxBindRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/bind/wx', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** checkWxBindStatus GET /api/user/bind/wx/check_status */
export async function checkWxBindStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkWxBindStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/bind/wx/check_status', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** reqWxBindCode GET /api/user/bind/wx/req_code */
export async function reqWxBindCodeUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringString_>('/api/user/bind/wx/req_code', {
    method: 'GET',
    ...(options || {}),
  })
}

/** changeEmail POST /api/user/change/email */
export async function changeEmailUsingPost(
  body: API.UserChangeEmailRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/change/email', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** changePassword POST /api/user/changePassword */
export async function changePasswordUsingPost(
  body: API.UserModifyPassWord,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/changePassword', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteUser POST /api/user/delete */
export async function deleteUserUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteBatchUser POST /api/user/delete/batch */
export async function deleteBatchUserUsingPost(
  body: API.DeleteRequest[],
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/delete/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userDestroy POST /api/user/destroy */
export async function userDestroyUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/destroy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userDestroySecure POST /api/user/destroy_secure */
export async function userDestroySecureUsingPost(
  body: API.UserDestroySecureRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/destroy_secure', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** exportUsers POST /api/user/export */
export async function exportUsersUsingPost(
  body: API.UserExportRequest,
  options?: { [key: string]: any }
) {
  return request<any>('/api/user/export', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getUserById GET /api/user/get */
export async function getUserByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUser_>('/api/user/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getDestroyCode POST /api/user/get_destroy_code */
export async function getDestroyCodeUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseString_>('/api/user/get_destroy_code', {
    method: 'POST',
    ...(options || {}),
  })
}

/** getEmailCode POST /api/user/get_emailcode */
export async function getEmailCodeUsingPost(
  body: API.EmailCodeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString_>('/api/user/get_emailcode', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getLoginUser GET /api/user/get/login */
export async function getLoginUserUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/get/login', {
    method: 'GET',
    ...(options || {}),
  })
}

/** getUserMultiDeviceLogin GET /api/user/get/multi_device_login */
export async function getUserMultiDeviceLoginUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserMultiDeviceLoginUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInt_>('/api/user/get/multi_device_login', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserPermissions GET /api/user/get/permissions */
export async function getUserPermissionsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserPermissionsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserUpdatePermissionsRequest_>('/api/user/get/permissions', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserPublicInfo GET /api/user/get/public */
export async function getUserPublicInfoUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserPublicInfoUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserPublicVO_>('/api/user/get/public', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserSignInRecord GET /api/user/get/sign_in */
export async function getUserSignInRecordUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserSignInRecordUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListInt_>('/api/user/get/sign_in', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getUserVOById GET /api/user/get/vo */
export async function getUserVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getUserVOByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseUserVO_>('/api/user/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getCode GET /api/user/getcode */
export async function getCodeUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringString_>('/api/user/getcode', {
    method: 'GET',
    ...(options || {}),
  })
}

/** listUserVOByPage POST /api/user/list/page/vo */
export async function listUserVoByPageUsingPost(
  body: API.UserQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserVO_>('/api/user/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userLogin POST /api/user/login */
export async function userLoginUsingPost(
  body: API.UserLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userLoginByWxCode POST /api/user/login/wx_code */
export async function userLoginByWxCodeUsingPost(
  body: API.UserWxLoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/login/wx_code', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** checkWxLoginStatus GET /api/user/login/wx/check_status */
export async function checkWxLoginStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkWxLoginStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLoginUserVO_>('/api/user/login/wx/check_status', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** reqWxLoginCode GET /api/user/login/wx/req_code */
export async function reqWxLoginCodeUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringString_>('/api/user/login/wx/req_code', {
    method: 'GET',
    ...(options || {}),
  })
}

/** userLogout POST /api/user/logout */
export async function userLogoutUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/user/logout', {
    method: 'POST',
    ...(options || {}),
  })
}

/** userRegister POST /api/user/register */
export async function userRegisterUsingPost(
  body: API.UserRegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/user/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** resetPassword POST /api/user/reset/password */
export async function resetPasswordUsingPost(
  body: API.UserResetPasswordRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/reset/password', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** userUnbindWx POST /api/user/unbind/wx */
export async function userUnbindWxUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/user/unbind/wx', {
    method: 'POST',
    ...(options || {}),
  })
}

/** checkWxUnbindStatus GET /api/user/unbind/wx/check_status */
export async function checkWxUnbindStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.checkWxUnbindStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/unbind/wx/check_status', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** reqWxUnbindCode GET /api/user/unbind/wx/req_code */
export async function reqWxUnbindCodeUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseMapStringString_>('/api/user/unbind/wx/req_code', {
    method: 'GET',
    ...(options || {}),
  })
}

/** updateUser POST /api/user/update */
export async function updateUserUsingPost(
  body: API.UserUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateUserAvatar POST /api/user/update/avatar */
export async function updateUserAvatarUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateUserAvatarUsingPOSTParams,
  body: {},
  multipartFile?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData()

  if (multipartFile) {
    formData.append('multipartFile', multipartFile)
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele]

    if (item !== undefined && item !== null) {
      if (typeof item === 'object' && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ''))
        } else {
          formData.append(ele, JSON.stringify(item))
        }
      } else {
        formData.append(ele, item)
      }
    }
  })

  return request<API.BaseResponseString_>('/api/user/update/avatar', {
    method: 'POST',
    params: {
      ...params,
    },
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}

/** updateUserMultiDeviceLogin POST /api/user/update/multi_device_login */
export async function updateUserMultiDeviceLoginUsingPost(
  body: API.UserMultiDeviceLoginUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/update/multi_device_login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateUserPermissions POST /api/user/update/permissions */
export async function updateUserPermissionsUsingPost(
  body: API.UserUpdatePermissionsRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/user/update/permissions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
