import axios from "axios";
import {message} from "ant-design-vue";

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: "",
  timeout: 150000,
  withCredentials: true,
});

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      // 不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    // 限流
    else if (data.code === 42900) {
      message.warning(data.message || '请求过于频繁，请稍后再试')
    }
    // 图像审核失败
    else if (data.code === 50001 && data.message && data.message.includes('图像审核')) {
      message.error('图像审核未通过')
    }
    // 图像审核失败
    else if (data.code === 50000 && data.message && data.message.includes('图像审核')) {
      message.error('图像审核未通过')
    }


    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with response error
    // 检查是否有响应数据
    if (error.response && error.response.data) {
      const { data } = error.response;
      if (data.code === 42900) {
        message.warning(data.message || '请求过于频繁，请稍后再试')
      }
    }
    return Promise.reject(error)
  },
)

export default myAxios;
