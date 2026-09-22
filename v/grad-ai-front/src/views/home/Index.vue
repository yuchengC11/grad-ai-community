<template>
    <div class="home-page">
        <div class="sub-cate" v-if="subCategory.length">
      <span
              v-for="item in subCategory"
              :key="item.id"
              class="cate-tag"
              :class="{ active: currentSub === item.id }"
              @click="changeCate(item.id)"
      >
        {{ item.name }}
      </span>
        </div>

        <div class="post-list">
            <div
                    class="post-card"
                    v-for="item in list"
                    :key="item.id"
                    @click="$router.push(`/post/detail/${item.id}`)"
            >

                <el-avatar :size="32" :src="item.avatar || ''" class="author-avatar">
                    {{ item.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="author">{{ item.nickname || '匿名' }}</span>
                <div class="post-title">{{ item.title }}</div>
                <div class="post-desc">{{ item.content?.slice(0, 60) }}...</div>
                <div class="post-footer">
                    <span class="author">{{ item.nickname || '匿名' }}</span>
                    <span>{{ item.createTime?.replace('T', ' ') }}</span>
                    <span class="like">❤️ {{ item.likeCount || 0 }}</span>
                    <span>💬 {{ item.commentCount || 0 }}</span>
                    <!-- ✅ 新增收藏按钮 -->
                    <span
                            class="fav-btn"
                            :class="{ active: item.isFavorited }"
                            @click.stop="handleFavorite(item)"
                    >
        <span class="star">{{ item.isFavorited ? '⭐' : '☆' }}</span>
        {{ item.isFavorited ? '已收藏' : '收藏' }}
    </span>
                </div>
            </div>
            <el-empty v-if="!list.length && !loading" description="暂无帖子" />
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

        <el-button
                v-if="isLoggedIn"
                class="fab-btn"
                type="primary"
                circle
                @click="openPostDialog"
        >
            <el-icon><Plus /></el-icon>
        </el-button>
    </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getPostList, addPost } from '@/api/post'
import { toggleFavorite } from '@/api/favorite'
const route = useRoute()
const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

// 当前选中的二级分类ID（null 表示全部）
const currentSub = ref(null)

// ========== 分类映射 ==========
// key 对应路由 query 中的 tab 值
const cateMap = {
    home: [],
    jiuye: [
        { id: 1, name: '内推信息' },
        { id: 2, name: '实习资讯' },
        { id: 3, name: '面试经验' }
    ],
    kaoyan: [
        { id: 4, name: '备考经验' },
        { id: 5, name: '复习资料' },
        { id: 6, name: '复试调剂' }
    ],
    gongkao: [
        { id: 7, name: '岗位报考' },
        { id: 8, name: '笔试备考' },
        { id: 9, name: '面试心得' }
    ],
    kaobian: [
        { id: 10, name: '考编资讯' },
        { id: 11, name: '笔试准备' },
        { id: 12, name: '面试技巧' }
    ]
}

const tabParentMap = {
    jiuye: 1,
    kaoyan: 2,
    gongkao: 3,
    kaobian: 4,
    home: null
}

const subCategory = computed(() => {
    const tab = route.query.tab || 'home'

    if (tab === 'home') {
        return [
            ...cateMap.jiuye,
            ...cateMap.kaoyan,
            ...cateMap.gongkao,
            ...cateMap.kaobian
        ]
    }
    return cateMap[tab] || []
})


const isLoggedIn = computed(() => !!localStorage.getItem('token'))

const getList = async () => {
    loading.value = true
    try {
        const tab = route.query.tab || 'home'
        const parentId = tabParentMap[tab] || null

        const params = {
            pageNum: pageNum.value,
            pageSize: pageSize.value
        }
        if (parentId !== null) {
            params.parentCategoryId = parentId
        }
        // 如果有二级分类筛选
        if (currentSub.value) {
            params.categoryId = currentSub.value
        }

        const res = await getPostList(params)
        list.value = res.data.records || []
        total.value = res.data.total || 0
    } catch (e) {
        console.error('加载帖子失败', e)
        list.value = []
        total.value = 0
    } finally {
        loading.value = false
    }
}

const changeCate = (id) => {
    if (currentSub.value === id) {
        currentSub.value = null
    } else {
        currentSub.value = id
    }
    pageNum.value = 1
    getList()
    router.replace({
        query: { ...route.query, sub: currentSub.value || undefined }
    })
}

watch(
    () => route.query.tab,
    () => {
        currentSub.value = null
        pageNum.value = 1
        getList()
    },
    { immediate: false }
)

const handleFavorite = async (post) => {
    try {
        const res = await toggleFavorite(post.id)
        post.isFavorited = !post.isFavorited
        ElMessage.success(res.msg || (post.isFavorited ? '收藏成功' : '取消收藏'))
    } catch (e) {
        ElMessage.error('操作失败')
    }
}
const openPostDialog = () => {
    if (!isLoggedIn.value) {
        ElMessage.warning('请先登录再发帖')
        return
    }
    router.push('/post/add')
}

onMounted(() => {
    const sub = route.query.sub
    if (sub) {
        currentSub.value = Number(sub)
    }
    getList()
})
</script>

<style scoped>
.fav-btn {
    cursor: pointer;
    user-select: none;
    display: flex;
    align-items: center;
    gap: 4px;
}
.fav-btn.active .star {
    color: #f7b731;
}
.home-page {
    padding: 12px 16px;
    background: #f5f7fa;
    min-height: 100vh;
}
.sub-cate {
    display: flex;
    gap: 10px;
    margin-bottom: 16px;
    overflow-x: auto;
    padding-bottom: 4px;
    flex-wrap: nowrap;
}
.cate-tag {
    flex-shrink: 0;
    padding: 6px 16px;
    background: #fff;
    border-radius: 20px;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    border: 1px solid #eee;
    transition: all 0.2s;
}
.cate-tag.active {
    background: #409eff;
    color: #fff;
    border-color: #409eff;
}
.post-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: box-shadow 0.2s;
}
.post-card:hover {
    box-shadow: 0 2px 12px rgba(0,0,0,0.08);
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
.fab-btn {
    position: fixed;
    right: 24px;
    bottom: 80px;
    width: 56px;
    height: 56px;
    font-size: 24px;
    box-shadow: 0 4px 12px rgba(64,158,255,0.4);
    z-index: 9;
}
.post-author {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}
.author-avatar {
    background: #409eff;
    color: #fff;
    font-weight: 600;
}

</style>