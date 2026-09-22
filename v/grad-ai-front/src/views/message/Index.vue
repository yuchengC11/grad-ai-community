<template>
    <div class="message-page">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
            <el-tab-pane label="全部" name="all"></el-tab-pane>
            <el-tab-pane label="私信" name="chat"></el-tab-pane>
            <el-tab-pane label="好友请求" name="requests"></el-tab-pane>
            <el-tab-pane label="未读" name="unread"></el-tab-pane>
        </el-tabs>

        <div class="msg-list" v-if="activeTab === 'all' || activeTab === 'unread'">
            <div class="msg-item" v-for="item in messageList" :key="item.id">
                <div class="avatar">
                    <el-avatar :size="44">{{ item.fromNickname?.charAt(0) || '系' }}</el-avatar>
                </div>
                <div class="msg-content">
                    <div class="msg-top">
                        <span class="name">{{ item.fromNickname || '系统消息' }}</span>
                        <span class="time">{{ item.createTime?.replace('T', ' ') }}</span>
                    </div>
                    <div class="msg-text">{{ item.content?.slice(0, 30) }}{{ item.content?.length > 30 ? '...' : '' }}</div>
                </div>
                <div v-if="item.isRead === 0" class="dot"></div>
            </div>
            <el-empty v-if="!messageList.length" :description="activeTab === 'unread' ? '暂无未读消息' : '暂无消息'" />
        </div>

        <div class="msg-list" v-if="activeTab === 'chat'">
            <div class="msg-item" v-for="item in chatList" :key="item.id" @click="goToChat(item)">
                <div class="avatar">
                    <el-avatar :size="44" :src="item.avatar">{{ item.nickname?.charAt(0) }}</el-avatar>
                </div>
                <div class="msg-content">
                    <div class="msg-top">
                        <span class="name">{{ item.nickname }}</span>
                        <span class="time">{{ item.lastTime?.replace('T', ' ') || '' }}</span>
                    </div>
                    <div class="msg-text">{{ item.lastContent?.slice(0, 30) || '暂无消息' }}</div>
                </div>
                <div v-if="item.unreadCount > 0" class="badge">{{ item.unreadCount }}</div>
            </div>
            <el-empty v-if="!chatList.length" description="暂无私信" />
        </div>

        <div class="msg-list" v-if="activeTab === 'requests'">
            <div class="request-item" v-for="item in requests" :key="item.id">
                <div class="avatar">
                    <el-avatar :size="44" :src="item.avatar">{{ item.nickname?.charAt(0) }}</el-avatar>
                </div>
                <div class="request-content">
                    <div class="request-name">{{ item.nickname }}</div>
                    <div class="request-time">{{ item.createTime?.replace('T', ' ') }}</div>
                </div>
                <div class="request-actions">
                    <el-button size="small" type="success" @click="acceptRequest(item.id)">同意</el-button>
                    <el-button size="small" type="danger" @click="rejectRequest(item.id)">拒绝</el-button>
                </div>
            </div>
            <el-empty v-if="!requests.length" description="暂无好友请求" />
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMessageList } from '@/api/message'
import { getPendingRequests, acceptFriend, rejectFriend } from '@/api/friend'
import { getChatList } from '@/api/chat'

const router = useRouter()
const activeTab = ref('all')
const messageList = ref([])
const chatList = ref([])
const requests = ref([])

const handleTabChange = (tab) => {
    if (tab === 'all' || tab === 'unread') {
        loadMessages()
    } else if (tab === 'chat') {
        loadChatList()
    } else if (tab === 'requests') {
        loadRequests()
    }
}

const loadMessages = async () => {
    try {
        const res = await getMessageList({ type: activeTab.value })
        messageList.value = res.data || []
    } catch {
        messageList.value = []
    }
}

const loadChatList = async () => {
    try {
        const res = await getChatList()
        chatList.value = res.data || []
    } catch {
        chatList.value = []
    }
}

const loadRequests = async () => {
    try {
        const res = await getPendingRequests()
        requests.value = res.data || []
    } catch {
        requests.value = []
    }
}

const acceptRequest = async (friendId) => {
    try {
        await acceptFriend(friendId)
        ElMessage.success('已接受好友请求')
        loadRequests()
    } catch {
        ElMessage.error('操作失败')
    }
}

const rejectRequest = async (friendId) => {
    try {
        await rejectFriend(friendId)
        ElMessage.success('已拒绝')
        loadRequests()
    } catch {
        ElMessage.error('操作失败')
    }
}

const goToChat = (item) => {
    router.push({ path: '/chat', query: { to: item.id } })
}

onMounted(() => {
    loadMessages()
})
</script>

<style scoped>
.message-page {
    background: #fff;
    min-height: 100%;
    padding-bottom: 20px;
}
.msg-list {
    padding: 0 16px;
}
.msg-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid #f0f0f0;
    position: relative;
    cursor: pointer;
}
.msg-item:hover {
    background: #f8f9fa;
}
.msg-content {
    flex: 1;
    min-width: 0;
}
.msg-top {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
}
.name {
    font-weight: 600;
    color: #333;
    font-size: 15px;
}
.time {
    font-size: 12px;
    color: #999;
}
.msg-text {
    font-size: 14px;
    color: #666;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #f56c6c;
    flex-shrink: 0;
}
.badge {
    background: #f56c6c;
    color: #fff;
    border-radius: 50%;
    padding: 2px 8px;
    font-size: 12px;
    min-width: 20px;
    text-align: center;
}

/* 好友请求 */
.request-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 0;
    border-bottom: 1px solid #f0f0f0;
}
.request-content {
    flex: 1;
}
.request-name {
    font-weight: 600;
    color: #333;
}
.request-time {
    font-size: 12px;
    color: #999;
}
.request-actions {
    display: flex;
    gap: 8px;
}
</style>