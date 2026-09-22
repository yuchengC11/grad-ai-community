import request from '@/utils/request'

export function toggleLike(postId) {
    return request({
        url: '/post/like/toggle',
        method: 'post',
        params: { postId }
    })
}
// 🆕 新增：查询当前用户是否点赞了该帖子
export function getLikeStatus(postId) {
    return request({
        url: '/post/like/status',
        method: 'get',
        params: { postId }
    })
}