<template>
    <div class="liked-posts">
        <div class="page-header">
            <h2>我的点赞</h2>
        </div>

        <div v-loading="loading" class="post-list">
            <div
                v-for="item in list"
                :key="item.id"
                class="post-item"
                @click="goToDetail(item.id)"
            >
                <div class="post-title">{{ item.title }}</div>
                <div class="post-desc">{{ item.content }}</div>

                <div class="post-meta">
                    <span class="author">{{ item.nickname || item.username || '匿名' }}</span>
                    <span class="time">{{ formatTime(item.createTime) }}</span>
                    <span class="stat">❤️ {{ item.likeCount || 0 }}</span>
                    <span class="stat">💬 {{ item.commentCount || 0 }}</span>
                </div>
            </div>

            <el-empty v-if="!loading && list.length === 0" description="暂无点赞的帖子" />
        </div>

        <el-pagination
            v-if="total > 0"
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="getList"
        />
    </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getLikedPosts } from '@/api/post'
import { ElMessage } from 'element-plus'

const router = useRouter()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const formatTime = (time) => {
    if (!time) return ''
    return time.replace('T', ' ').substring(0, 19)
}

const goToDetail = (id) => {
    router.push('/post/detail/' + id)
}

const getList = async () => {
    console.log('【调试】getList 被调用了', Date.now())
    loading.value = true
    try {
        const res = await getLikedPosts({
            pageNum: pageNum.value,
            pageSize: pageSize.value
        })
        console.log('【调试】接口返回条数:', res.data?.records?.length)

        // 先拿到原始数据
        let records = res.data.records || []
        total.value = res.data.total || 0

        // 用后端实时 hasLiked 状态过滤已取消点赞的帖子（避免 MySQL 同步延迟导致的残留显示）
        records = records.filter(item => item.hasLiked !== false)

        list.value = records
    } catch (e) {
        ElMessage.error('加载失败')
    } finally {
        loading.value = false
    }
}
onActivated(getList)
onMounted(getList)
</script>

<style scoped>
.liked-posts {
    padding: 20px;
    max-width: 900px;
    margin: 0 auto;
}
.page-header {
    margin-bottom: 20px;
}
.page-header h2 {
    margin: 0;
    font-size: 20px;
    color: #333;
    font-weight: 600;
}
.post-list {
    background: #fff;
    border-radius: 8px;
}
.post-item {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background 0.2s;
}
.post-item:last-child {
    border-bottom: none;
}
.post-item:hover {
    background: #fafafa;
}
.post-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 8px;
}
.post-desc {
    color: #666;
    font-size: 14px;
    margin-bottom: 10px;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.5;
}
.post-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    color: #999;
}
.author {
    color: #409eff;
    font-weight: 500;
}
.stat {
    display: flex;
    align-items: center;
    gap: 4px;
}
</style>