import request from '@/utils/request'
export function getPostList(params) {
    return request({
        url: '/post/list',
        method: 'get',
        params
    })
}
export function getPostDetail(id) {
    return request({
        url: `/post/${id}`,
        method: 'get'
    })
}

export function getLikedPosts(params) {
    return request({
        url: '/post/liked/list',
        method: 'get',
        params
    })
}
export function addPost(data) {
    return request({
        url: '/post/add',
        method: 'post',
        data
    })
}
export function getMyPosts(params) {
    return request({
        url: '/post/my',
        method: 'get',
        params
    })
}

export const deletePost = (postId) => request({ url: `/post/${postId}`, method: 'delete' })

export function searchPosts(params) {
    return request({
        url: '/post/search',
        method: 'get',
        params
    })
}

// 新增接口
export function getPostsByUser(userId) {
    return request({
        url: `/post/user/${userId}`,
        method: 'get'
    })
}