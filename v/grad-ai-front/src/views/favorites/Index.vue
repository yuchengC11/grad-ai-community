<template>
    <div class="favorites-page">
        <div class="page-header">
            <h2>我的收藏</h2>
        </div>

        <div class="post-list">
            <div
                class="post-card"
                v-for="item in list"
                :key="item.id"
                @click="$router.push(`/post/detail/${item.id}`)"
            >
                <div class="post-title">{{ item.title }}</div>
                <div class="post-desc">{{ item.content?.slice(0, 60) }}...</div>
                <div class="post-footer">
                    <span class="author">{{ item.nickname || '匿名' }}</span>
                    <span>{{ item.createTime?.replace('T', ' ') }}</span>
                    <span class="like">❤️ {{ item.likeCount || 0 }}</span>
                    <span>💬 {{ item.commentCount || 0 }}</span>
                </div>
            </div>
            <el-empty v-if="!list.length && !loading" description="暂无收藏" />
        </div>

        <div class="pagination-wrap" v-if="total > 0">
            <el-pagination
                v-model:current-page="pageNum"
                v-model:page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="getList"
            />
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onActivated, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavorites } from '@/api/favorite'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const getList = async (force = false) => {
    // 如果不是强制刷新，且没有标记，且已有数据，就跳过（避免重复请求）
    if (!force && list.value.length > 0) return

    loading.value = true
    try {
        const res = await getFavorites({ pageNum: pageNum.value, pageSize: pageSize.value })
        list.value = res.data.records || []
        total.value = res.data.total || 0
        // 刷新成功后清除标记
        sessionStorage.removeItem('favorites_need_refresh')
    } catch (e) {
        ElMessage.error('加载收藏失败')
    } finally {
        loading.value = false
    }
}

// 页面挂载时刷新（首次进入、浏览器返回都会走这里）
onMounted(() => {
    getList(true)
    // 兜底：监听页面可见性变化（浏览器返回/切换标签页）
    document.addEventListener('visibilitychange', handleVisibilityChange)
})

// 如果被 keep-alive 缓存，返回时走这里
onActivated(() => {
    getList(true)
})

// 清理事件监听
onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
})

const handleVisibilityChange = () => {
    // 页面重新可见时，如果有标记就刷新
    if (!document.hidden && sessionStorage.getItem('favorites_need_refresh') === '1') {
        getList(true)
    }
}
</script>

<style scoped>
.favorites-page {
    padding: 16px;
    background: #f5f7fa;
    min-height: 100vh;
}
.page-header {
    margin-bottom: 16px;
}
.page-header h2 {
    font-size: 20px;
    color: #333;
}
.post-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 12px;
    cursor: pointer;
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
    line-height: 1.5;
    margin-bottom: 12px;
}
.post-footer {
    display: flex;
    gap: 16px;
    font-size: 12px;
    color: #999;
}
.author {
    color: #409eff;
}
.pagination-wrap {
    display: flex;
    justify-content: center;
    margin: 20px 0;
}
</style>