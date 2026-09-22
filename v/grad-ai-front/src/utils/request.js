import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api',
    timeout: 60000,
    headers: { 'Content-Type': 'application/json' }
})

service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = token
        }
        return config
    },
    error => Promise.reject(error)
)

service.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.msg || '请求失败')
            if (res.code === 401) {
                localStorage.removeItem('token')
                localStorage.removeItem('userInfo')
            }
            return Promise.reject(new Error(res.msg || 'Error'))
        }
        return res
    },
    error => {
        ElMessage.error(error.message || '网络异常')
        return Promise.reject(error)
    }
)

export default service