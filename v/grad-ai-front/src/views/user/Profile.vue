<template>
    <div class="user-profile">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="userInfo">
            <div class="user-card">
                <el-avatar :size="80" class="avatar" :src="userInfo.avatar || ''">
                    {{ userInfo.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <div class="user-detail">
                    <h2>{{ userInfo.nickname }}</h2>
                    <p>{{ userInfo.school || '未设置' }} · {{ userInfo.major || '未设置' }}</p>
                    <div class="actions">
<!--                        <el-button-->
<!--                            v-if="!isSelf"-->
<!--                            type="primary"-->
<!--                            size="small"-->
<!--                            @click="handleAddFriend"-->
<!--                            :loading="friendLoading"-->
<!--                            :disabled="friendStatus === 'accepted'"-->
<!--                        >-->
<!--                            {{ friendButtonText }}-->
<!--                        </el-button>-->

                        <template v-if="!isSelf">
                            <el-button
                                v-if="friendStatus === 'accepted'"
                                type="danger"
                                size="small"
                                @click="handleDeleteFriend"
                            >
                                删除好友
                            </el-button>
                            <el-button
                                v-else
                                type="primary"
                                size="small"
                                @click="handleAddFriend"
                                :loading="friendLoading"
                                :disabled="friendStatus === 'pending'"
                            >
                                {{ friendButtonText }}
                            </el-button>
                        </template>
                        <el-button v-else type="info" size="small" disabled>这是你自己</el-button>
                    </div>
                </div>
            </div>
            <div class="user-posts">
                <h3>帖子</h3>
                <div class="post-card" v-for="p in posts" :key="p.id" @click="$router.push(`/post/detail/${p.id}`)">
                    <div class="post-title">{{ p.title }}</div>
                    <div class="post-desc">{{ p.content?.slice(0, 60) }}...</div>
                    <div class="post-footer">
                        <span>❤️ {{ p.likeCount || 0 }}</span>
                        <span>💬 {{ p.commentCount || 0 }}</span>
                    </div>
                </div>
                <el-empty v-if="!posts.length" description="暂无帖子" />
            </div>
        </div>
        <el-empty v-else description="用户不存在" />
    </div>


</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserById } from '@/api/user'
import { addFriend, getFriendStatus } from '@/api/friend'
import { getPostsByUser } from '@/api/post'
import { deleteFriend } from '@/api/friend'  // 如果后端接口不同，等会改

const route = useRoute()
const router = useRouter()
const userId = ref(route.params.id)
const userInfo = ref(null)
const posts = ref([])
const loading = ref(false)
const friendLoading = ref(false)
const friendStatus = ref('')

const currentUserId = localStorage.getItem('userId')
const isSelf = computed(() => currentUserId && Number(currentUserId) === Number(userId.value))

const handleDeleteFriend = async () => {
    try {
        await ElMessageBox.confirm('确定删除好友？删除后将无法继续聊天', '提示', {
            confirmButtonText: '删除',
            cancelButtonText: '取消',
            type: 'warning'
        })
        await deleteFriend(userId.value)
        ElMessage.success('已删除好友')
        friendStatus.value = ''  // 状态重置
    } catch (e) {
        if (e !== 'cancel') {
            ElMessage.error(e.message || '删除失败')
        }
    }
}


const friendButtonText = computed(() => {
    if (friendStatus.value === 'accepted') return '已是好友'
    if (friendStatus.value === 'pending') return '等待对方确认'
    if (friendStatus.value === 'rejected') return '已拒绝'
    return '加好友'
})

const loadProfile = async () => {
    loading.value = true
    try {
        // 1. 加载用户信息（这个成功）
        const userRes = await getUserById(userId.value)
        userInfo.value = userRes.data

        // 2. 加载帖子（单独 try-catch，失败不影响页面）
        try {
            const postRes = await getPostsByUser(userId.value)
            posts.value = postRes.data?.records || postRes.data?.list || postRes.data || []
        } catch (postErr) {
            console.error('帖子接口报错:', postErr)
            posts.value = []
        }

        // 3. 查询好友状态
        if (currentUserId && !isSelf.value) {
            const statusRes = await getFriendStatus(userId.value)
            friendStatus.value = statusRes.data || ''
        }
    } catch (e) {
        console.error('用户信息加载失败:', e)
        const errMsg = e?.message || ''
        // 未登录被后端拦截时，引导登录而不是误报“用户不存在”
        if (errMsg.includes('未登录')) {
            ElMessage.warning('请先登录后查看用户主页')
            router.push({ path: '/login', query: { redirect: route.fullPath } })
            return
        }
        ElMessage.error('加载用户信息失败')
    } finally {
        loading.value = false
    }
}
const handleAddFriend = async () => {
    if (friendStatus.value === 'accepted') {
        ElMessage.info('你们已经是好友了')
        return
    }
    if (friendStatus.value === 'pending') {
        ElMessage.info('已发送好友请求，请等待对方确认')
        return
    }
    friendLoading.value = true
    try {
        const res = await addFriend(userId.value)
        ElMessage.success(res.msg || '好友请求已发送')
        friendStatus.value = 'pending'
    } catch (e) {
        ElMessage.error(e.message || '操作失败')
    } finally {
        friendLoading.value = false
    }
}

onMounted(loadProfile)
</script>

<style scoped>
.user-profile {
    padding: 16px;
    background: #f5f7fa;
    min-height: 100vh;
    max-width: 900px;   /* 最宽 900px */
    margin: 0 auto;     /* 居中 */

}
.user-card {
    background: #fff;
    border-radius: 12px;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 20px;
}
.avatar {
    flex-shrink: 0;
}
.user-detail h2 {
    margin: 0 0 4px 0;
}
.user-detail p {
    color: #666;
    margin: 0 0 12px 0;
}
.user-posts {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
}
.user-posts h3 {
    margin-top: 0;
}
.post-card {
    padding: 12px 0;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
}
.post-card:last-child {
    border-bottom: none;
}
.post-title {
    font-weight: 600;
}
.post-desc {
    color: #666;
    font-size: 14px;
    margin: 4px 0;
}
.post-footer {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #999;
}
</style>