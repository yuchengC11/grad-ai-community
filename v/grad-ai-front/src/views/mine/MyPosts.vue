<template>
    <div class="my-posts">
        <div class="page-header">
            <h2>我的帖子</h2>
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
                    <span>❤️ {{ item.likeCount || 0 }}</span>
                    <span>💬 {{ item.commentCount || 0 }}</span>
                    <span>{{ item.createTime?.replace('T', ' ') }}</span>
                    <!-- ✅ 新增收藏按钮 -->
                    <span
                        class="fav-btn"
                        :class="{ active: item.isFavorited }"
                        @click.stop="handleFavorite(item)"
                    >
                    <span class="star">{{ item.isFavorited ? '⭐' : '☆' }}</span>
                         {{ item.isFavorited ? '已收藏' : '收藏' }}
                     </span>
                    <!-- 删除按钮：只在"我的帖子"页面显示，阻止冒泡防止触发卡片跳转 -->
                    <span
                        class="delete-btn"
                        @click.stop="handleDelete(item.id)"
                    >
                        🗑️ 删除
                    </span>
                </div>
            </div>
            <el-empty v-if="!list.length && !loading" description="暂无帖子" />
        </div>

        <!-- 分页 -->
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
import { ref, onMounted } from 'vue'
import { getMyPosts,deletePost } from '@/api/post'
import { toggleFavorite } from '@/api/favorite'
import { ElMessage, ElMessageBox } from 'element-plus'
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const getList = async () => {
    loading.value = true
    try {
        const res = await getMyPosts({
            pageNum: pageNum.value,
            pageSize: pageSize.value
        })
        list.value = res.data.records || []
        total.value = res.data.total || 0
    } catch (e) {
        ElMessage.error('加载我的帖子失败')
    } finally {
        loading.value = false
    }
}
const handleFavorite = async (post) => {
    try {
        const res = await toggleFavorite(post.id)
        post.isFavorited = !post.isFavorited
        ElMessage.success(res.msg || (post.isFavorited ? '收藏成功' : '取消收藏'))
    } catch (e) {
        ElMessage.error('操作失败')
    }
}
// ✅ 新增：删除帖子
// ✅ 修复后的删除逻辑
const handleDelete = async (postId) => {
    // 第一步：弹确认框，用户点取消直接 return，不执行后面
    try {
        await ElMessageBox.confirm('确定要删除这条帖子吗？删除后不可恢复！', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        })
    } catch {
        // 用户点了取消/关闭/ESC，静默退出
        return
    }

    // 第二步：发请求
    try {
        const res = await deletePost(postId)
        if (res.code === 200) {
            ElMessage.success('删除成功')
            list.value = list.value.filter(item => item.id !== postId)
            total.value--
            // 如果删完当前页空了，且不是第一页，刷新列表
            if (list.value.length === 0 && pageNum.value > 1) {
                pageNum.value--
                getList()
            }
        } else {
            ElMessage.error(res.msg || '删除失败')
        }
    } catch (err) {
        // 这里只可能是请求报错（404/500/网络错误）
        console.error('删除请求报错:', err)
        ElMessage.error(err.response?.data?.msg || '删除失败，请检查网络或后端接口')
    }
}


onMounted(getList)
</script>

<style scoped>

/* ✅ 新增：删除按钮样式 */
.delete-btn {
    color: #f56c6c;
    cursor: pointer;
    margin-left: auto;  /* 靠右对齐 */
    transition: color 0.2s;
}
.delete-btn:hover {
    color: #ff4d4f;
}
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
.my-posts {
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
    font-weight: 600;
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
.pagination-wrap {
    display: flex;
    justify-content: center;
    margin: 20px 0;
}
</style>