import request from '@/utils/request'

export function aiSummary(postId) {
    return request({
        url: '/ai/post/summary',
        method: 'get',
        params: { postId }
    })
}

export function aiComment(postId, prompt) {
    return request({
        url: '/ai/post/genComment',
        method: 'get',
        params: { postId, prompt }
    })
}

export function aiChat(data) {
    return request({
        url: '/ai/chat',
        method: 'post',
        data
    })
}