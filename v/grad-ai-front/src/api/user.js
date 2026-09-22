import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/user/login',
        method: 'post',
        data
    })
}

export function register(data) {
    return request({
        url: '/user/register',
        method: 'post',
        data
    })
}

export function getUserInfo() {
    return request({
        url: '/user/info',
        method: 'get'
    })
}

export function updateUserInfo(data) {
    return request({
        url: '/user/update',
        method: 'post',
        data
    })
}

export function uploadAvatar(formData) {
    return request({
        url: '/user/avatar',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export function getUserById(id) {
    return request({
        url: `/user/${id}`,
        method: 'get'
    })
}

export function searchUsers(keyword) {
    return request({
        url: '/user/search',
        method: 'get',
        params: { keyword }
    })
}