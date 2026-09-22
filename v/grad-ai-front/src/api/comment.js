import request from '@/utils/request'

export function getCommentList(postId) {
    return request({
        url: '/comment/list',
        method: 'get',
        params: { postId }
    })
}

export function addComment(data) {
    return request({
        url: '/comment/add',
        method: 'post',
        data
    })
}