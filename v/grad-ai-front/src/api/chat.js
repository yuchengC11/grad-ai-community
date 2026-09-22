import request from '@/utils/request'

export function sendMessage(data) {
    return request({
        url: '/message/send',
        method: 'post',
        data
    })
}

export function getConversation(userId, params) {
    return request({
        url: `/message/conversation/${userId}`,
        method: 'get',
        params
    })
}
export function getUnreadCount() {
    return request({
        url: '/message/unread',
        method: 'get'
    })
}


export function markAsRead(userId) {
    return request({
        url: '/message/read',
        method: 'post',
        params: { userId }
    })
}


export function getChatList() {
    return request({
        url: '/message/chat/list',
        method: 'get'
    })
}