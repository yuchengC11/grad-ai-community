<template>
    <div class="chat-page">
        <div class="chat-sidebar">
            <h3>消息</h3>
            <div class="search-user">
                <el-input
                    v-model="searchKeyword"
                    placeholder="搜索用户..."
                    size="small"
                    clearable
                    @keyup.enter="searchUser"
                >
                    <template #append>
                        <el-button @click="searchUser">搜索</el-button>
                    </template>
                </el-input>
            </div>
            <div v-for="contact in contactList" :key="contact.id" class="contact-item" @click="selectContact(contact)">
                <el-avatar :size="36" :src="contact.avatar">{{ contact.nickname?.charAt(0) }}</el-avatar>
                <span class="contact-name">{{ contact.nickname }}</span>
                <el-badge v-if="contact.unreadCount" :value="contact.unreadCount" class="badge" />
            </div>
            <el-empty v-if="!contactList.length && !loading" description="暂无联系人" />
        </div>

        <div class="chat-main" v-if="currentContact">
            <div class="chat-header">
                <div class="header-left" @click="goToProfile" style="cursor:pointer">
                    <el-avatar :size="32" :src="currentContact.avatar">{{ currentContact.nickname?.charAt(0) }}</el-avatar>
                    <span class="contact-name">{{ currentContact.nickname }}</span>
                </div>
                <div>
                    <el-button type="text" @click="goToProfile">查看主页</el-button>
                    <el-button type="text" @click="addFriendFromChat" v-if="!isFriend && !isSelf">➕ 加好友</el-button>
                </div>
            </div>
            <div class="messages" ref="messagesRef">
                <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="msg"
                    :class="Number(msg.fromUserId) === currentUserId ? 'self' : 'other'"
                >
                    <div class="bubble">{{ msg.content }}</div>
                    <div class="time">{{ msg.createTime?.replace('T', ' ') }}</div>
                </div>
                <div v-if="loadingMessages" class="loading-text">加载消息中...</div>
                <el-empty v-if="!messages.length && !loadingMessages" description="暂无消息，开始聊天吧" />
            </div>
            <div class="input-area">
                <el-input
                    v-model="inputText"
                    placeholder="输入消息..."
                    @keyup.enter="sendMessage"
                    maxlength="500"
                    show-word-limit
                />
                <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
            </div>
        </div>
        <el-empty v-else description="选择联系人或搜索用户开始聊天" />
    </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFriendList, addFriend, getFriendStatus } from '@/api/friend'
import { getConversation, sendMessage as sendMsg, markAsRead, getChatList } from '@/api/chat'
import { getUserById, searchUsers } from '@/api/user'

const route = useRoute()
const router = useRouter()

const currentUserId = Number(localStorage.getItem('userId')) || 0

const contactList = ref([])
const currentContact = ref(null)
const messages = ref([])
const inputText = ref('')
const messagesRef = ref(null)
const loading = ref(false)
const loadingMessages = ref(false)
const sending = ref(false)
const searchKeyword = ref('')
const isFriend = ref(false)
const isSelf = computed(() => currentContact.value?.id === currentUserId)

const goToProfile = () => {
    if (currentContact.value && currentContact.value.id) {
        router.push(`/user/${currentContact.value.id}`)
    } else {
        ElMessage.warning('用户信息不完整')
    }
}

const loadContacts = async () => {
    loading.value = true
    try {
        const res = await getChatList()
        contactList.value = res.data || []
        if (!contactList.value.length) {
            const friendRes = await getFriendList()
            contactList.value = friendRes.data || []
        }
    } catch (e) {
        ElMessage.error('加载联系人失败')
    } finally {
        loading.value = false
    }
}

const selectContact = async (contact) => {
    if (!contact || !contact.id) {
        ElMessage.warning('联系人信息无效')
        return
    }
    currentContact.value = contact
    try {
        const res = await getFriendStatus(contact.id)
        isFriend.value = res.data === 'accepted'
    } catch {
        isFriend.value = false
    }
    await markAsRead(contact.id)
    await loadMessages()
    const item = contactList.value.find(c => c.id === contact.id)
    if (item) item.unreadCount = 0
}

const loadMessages = async () => {
    if (!currentContact.value) return
    loadingMessages.value = true
    try {
        const res = await getConversation(currentContact.value.id, { pageNum: 1, pageSize: 50 })
        messages.value = (res.data.records || []).sort((a, b) => {
            return new Date(a.createTime) - new Date(b.createTime)
        })
        nextTick(() => {
            if (messagesRef.value) {
                messagesRef.value.scrollTop = messagesRef.value.scrollHeight
            }
        })
    } catch (e) {
        ElMessage.error('加载消息失败')
    } finally {
        loadingMessages.value = false
    }
}

const sendMessage = async () => {
    const text = inputText.value.trim()
    if (!text || !currentContact.value) return
    sending.value = true
    try {
        await sendMsg({ toUserId: currentContact.value.id, content: text })
        inputText.value = ''
        messages.value.push({
            fromUserId: currentUserId,
            content: text,
            createTime: new Date().toISOString()
        })
        const item = contactList.value.find(c => c.id === currentContact.value.id)
        if (item) {
            item.lastContent = text
            item.lastTime = new Date().toISOString()
        }
        nextTick(() => {
            if (messagesRef.value) {
                messagesRef.value.scrollTop = messagesRef.value.scrollHeight
            }
        })
    } catch (e) {
        ElMessage.error(e.message || '发送失败')
    } finally {
        sending.value = false
    }
}

const searchUser = async () => {
    const keyword = searchKeyword.value.trim()
    if (!keyword) {
        await loadContacts()
        return
    }
    try {
        const res = await searchUsers(keyword)
        const users = res.data || []
        if (users.length === 0) {
            ElMessage.info('未找到用户')
            return
        }
        const existingIds = contactList.value.map(c => c.id)
        for (const user of users) {
            if (!existingIds.includes(user.id)) {
                contactList.value.unshift(user)
            }
        }
        selectContact(users[0])
    } catch (e) {
        ElMessage.error('搜索失败')
    }
}

const addFriendFromChat = async () => {
    if (!currentContact.value) return
    try {
        const res = await addFriend(currentContact.value.id)
        ElMessage.success(res.msg || '好友请求已发送')
        isFriend.value = true
    } catch (e) {
        ElMessage.error(e.message || '操作失败')
    }
}

const initFromQuery = async () => {
    const to = route.query.to
    if (!to) return
    try {
        const res = await getUserById(to)
        const user = res.data
        if (!user || !user.id) {
            ElMessage.error('用户不存在')
            return
        }
        const exists = contactList.value.find(c => c.id === user.id)
        if (!exists) {
            contactList.value.unshift(user)
        }
        await selectContact(user)
    } catch (e) {
        ElMessage.error('用户不存在')
    }
}

watch(
    () => route.query.to,
    (newVal) => {
        if (newVal) {
            initFromQuery()
        }
    }
)

onMounted(async () => {
    await loadContacts()
    await initFromQuery()
})
</script>

<style scoped>
/* 确保 marked 生成的 HTML 有正确排版 */
.ai-response {
    line-height: 1.8;
    font-size: 14px;
    color: #333;
}
.ai-response h3, .ai-response h4 {
    color: #409eff;
    margin: 12px 0 8px;
}
.ai-response ol, .ai-response ul {
    padding-left: 20px;
    margin: 8px 0;
}
.ai-response li {
    margin: 6px 0;
}
.ai-response p {
    margin: 8px 0;
}
.chat-page {
    display: flex;
    height: calc(100vh - 64px);
    background: #f5f7fa;
}

.chat-sidebar {
    width: 280px;
    background: #fff;
    border-right: 1px solid #eee;
    padding: 12px;
    overflow-y: auto;
    flex-shrink: 0;
}
.chat-sidebar h3 {
    margin: 0 0 12px 0;
    font-size: 16px;
}
.search-user {
    margin-bottom: 12px;
    display: flex;
    gap: 8px;
}
.search-user .el-input {
    flex: 1;
}
.contact-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px;
    cursor: pointer;
    border-radius: 6px;
    transition: background 0.2s;
    position: relative;
}
.contact-item:hover {
    background: #f0f2f5;
}
.contact-item .contact-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.badge {
    margin-left: auto;
}

.chat-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: #fff;
    margin: 12px;
    border-radius: 12px;
    overflow: hidden;
    min-width: 0;
}
.chat-header {
    padding: 12px 16px;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
}
.header-left {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
}
.header-left .contact-name {
    font-weight: 600;
}
.messages {
    flex: 1;
    padding: 16px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
}
.msg {
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
    max-width: 70%;
}
.msg.self {
    align-self: flex-end;
}
.msg.other {
    align-self: flex-start;
}
.bubble {
    padding: 8px 14px;
    border-radius: 16px;
    font-size: 14px;
    word-break: break-word;
}
.msg.self .bubble {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 4px;
}
.msg.other .bubble {
    background: #f0f0f0;
    color: #333;
    border-top-left-radius: 4px;
}
.time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
    padding: 0 8px;
}
.input-area {
    display: flex;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #eee;
    background: #fafafa;
    flex-shrink: 0;
}
.input-area .el-input {
    flex: 1;
}
.loading-text {
    text-align: center;
    color: #999;
    padding: 20px;
}
</style>