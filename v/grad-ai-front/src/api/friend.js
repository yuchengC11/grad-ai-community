import request from '@/utils/request'

export const deleteFriend = (friendId) => request.delete(`/friend/${friendId}`)
export function addFriend(friendId) {
    return request({
        url: '/friend/add',
        method: 'post',
        params: { friendId }
    })
}
export function checkFriendStatus(friendId) {
    return request({
        url: '/friend/status',
        method: 'get',
        params: { friendId }
    })
}
export function acceptFriend(friendId) {
    return request({
        url: '/friend/accept',
        method: 'post',
        params: { friendId }
    })
}

export function rejectFriend(friendId) {
    return request({
        url: '/friend/reject',
        method: 'post',
        params: { friendId }
    })
}

export function getFriendList() {
    return request({
        url: '/friend/list',
        method: 'get'
    })
}

export function getPendingRequests() {
    return request({
        url: '/friend/requests',
        method: 'get'
    })
}
export function getFriendStatus(friendId) {
    return request({
        url: '/friend/status',
        method: 'get',
        params: { friendId }
    })
}