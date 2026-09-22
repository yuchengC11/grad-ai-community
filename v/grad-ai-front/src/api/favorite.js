import request from '@/utils/request'

export function toggleFavorite(postId) {
    return request({
        url: '/favorite/toggle',
        method: 'post',
        params: { postId }
    })
}

export function getFavorites(params) {
    return request({
        url: '/favorite/list',
        method: 'get',
        params
    })
}