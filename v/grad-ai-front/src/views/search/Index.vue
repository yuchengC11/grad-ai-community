<template>
    <div class="search-page">
        <div class="search-results">
            <div class="post-card" v-for="item in list" :key="item.id" @click="$router.push(`/post/detail/${item.id}`)">
                <div class="post-title">{{ item.title }}</div>
                <div class="post-desc">{{ item.content?.slice(0, 80) }}...</div>
                <div class="post-footer">
                    <span class="author">{{ item.nickname || '匿名' }}</span>
                    <span>{{ item.createTime?.replace('T', ' ') }}</span>
                    <span class="like">❤️ {{ item.likeCount || 0 }}</span>
                    <span>💬 {{ item.commentCount || 0 }}</span>
                </div>
            </div>
            <el-empty v-if="!list.length && !loading" description="暂无搜索结果" />
            <div v-if="loading" class="loading-text">搜索中...</div>
        </div>

        <div class="pagination-wrap" v-if="total > 0">
            <el-pagination
                    v-model:current-page="pageNum"
                    v-model:page-size="pageSize"
                    :total="total"
                    layout="prev, pager, next"
                    @current-change="doSearch"
            />
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { searchPosts } from '@/api/post'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const doSearch = async () => {
    const kw = keyword.value.trim()
    if (!kw) {
        ElMessage.warning('请输入搜索关键词')
        return
    }
    router.replace({ query: { keyword: kw } })
    loading.value = true
    try {
        const res = await searchPosts({ keyword: kw, pageNum: pageNum.value, pageSize: pageSize.value })
        list.value = res.data.records || []
        total.value = res.data.total || 0
    } catch (e) {
        ElMessage.error('搜索失败')
        list.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const clearSearch = () => {
    keyword.value = ''
    list.value = []
    total.value = 0
    router.replace({ query: {} })
}

onMounted(() => {
    const kw = route.query.keyword
    if (kw) {
        keyword.value = kw
        doSearch()
    }
})

watch(() => route.query.keyword, (newKw) => {
    if (newKw) {
        keyword.value = newKw
        doSearch()
    } else {
        clearSearch()
    }
})
</script>

<style scoped>
.search-page {
    padding: 16px;
    background: #f5f7fa;
    min-height: 100vh;
}
.search-header {
    margin-bottom: 20px;
}
.search-header .el-input {
    flex: 1;
}
.search-results {
    background: #fff;
    border-radius: 8px;
    padding: 12px;
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
    font-size: 16px;
    font-weight: 600;
    color: #333;
}
.post-desc {
    color: #666;
    font-size: 14px;
    margin: 6px 0;
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
    margin-top: 20px;
}
.loading-text {
    text-align: center;
    color: #999;
    padding: 20px;
}
</style>