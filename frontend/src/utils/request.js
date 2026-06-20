import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

const extractErrorMessage = (error) => {
  if (error?.response?.data?.message) {
    return error.response.data.message
  }
  if (error?.message) {
    return error.message
  }
  return '网络异常'
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 409 || response.status === 409) {
        return res
      }
      const msg = res.message || '请求失败'
      const err = new Error(msg)
      err.response = { data: res }
      return Promise.reject(err)
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 409) {
      return error.response.data
    }
    const msg = extractErrorMessage(error)
    const err = new Error(msg)
    if (error.response) {
      err.response = error.response
    }
    return Promise.reject(err)
  }
)

export default request
