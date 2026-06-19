import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (response.status === 409) {
        return res
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 409) {
      return error.response.data
    }
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
