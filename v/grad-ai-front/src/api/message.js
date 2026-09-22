import request from '@/utils/request'

export function getMessageList(type = 'all') {
    return request({
        url: '/message/list',
        method: 'get',
        params: { type }
    })
}