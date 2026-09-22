<template>
    <div class="post-detail">
        <!-- 加载状态 -->
        <el-skeleton v-if="loading && !postInfo" :rows="4" animated />

        <!-- 帖子内容 -->
        <div v-if="postInfo">
            <!-- ===== 作者信息（头像 + 昵称 + 时间） ===== -->
            <div class="post-author">
                <el-avatar
                    :size="36"
                    :src="postInfo.avatar || ''"
                    class="author-avatar"
                    @click="$router.push(`/user/${postInfo.userId}`)"
                >
                    {{ postInfo.nickname?.charAt(0) || 'U' }}
                </el-avatar>
                <span class="author-name" @click="$router.push(`/user/${postInfo.userId}`)">
          {{ postInfo.nickname || '匿名' }}
        </span>
                <!-- 发私信按钮 -->
                <el-button type="text" size="small" @click="goToChat" v-if="postInfo.userId !== currentUserId">
                    💬 私信
                </el-button>

                <span class="post-time">{{ postInfo.createTime?.replace('T', ' ') }}</span>
            </div>

            <h2>{{ postInfo.title }}</h2>
            <p class="post-content">{{ postInfo.content }}</p>

            <!-- ===== AI 总结区域 ===== -->
            <div class="ai-summary-section">
                <div class="ai-summary-header" @click="toggleSummary">
                    <span class="ai-icon">✨</span>
                    <span>AI 智能总结</span>
                    <el-icon class="arrow-icon" :class="{ expanded: showSummary }">
                        <ArrowDown />
                    </el-icon>
                </div>
                <div v-if="showSummary" class="ai-summary-body">
                    <div v-if="summaryLoading" class="summary-loading">
                        <el-icon class="is-loading"><Loading /></el-icon>
                        <span>AI 正在生成摘要...</span>
                    </div>
                    <div v-else-if="summaryContent" class="summary-content" v-html="renderMarkdown(summaryContent)"></div>
                    <div v-else-if="summaryError" class="summary-error">{{ summaryError }}</div>
                    <div v-else class="summary-placeholder">点击上方按钮，AI 将为你生成帖子摘要</div>
                </div>
            </div>

            <!-- ===== 操作栏：点赞 + 收藏 + 评论数 ===== -->
            <div class="action-bar">
        <span class="like-btn" :class="{ active: isLiked }" @click="handleLike">
          <span class="heart">{{ isLiked ? '❤️' : '♡' }}</span>
          {{ postInfo.likeCount || 0 }}
        </span>
                <span class="fav-btn" :class="{ active: isFavorited }" @click="handleFavorite">
          <span class="star">{{ isFavorited ? '⭐' : '☆' }}</span>
          收藏
        </span>
                <span class="comment-count">💬 {{ postInfo.commentCount || 0 }}</span>
            </div>
        </div>

        <!-- ===== 评论输入 ===== -->
        <div class="comment-box">
            <el-input
                v-model="commentText"
                type="textarea"
                :rows="3"
                placeholder="写下你的评论..."
                maxlength="200"
                show-word-limit
            />
            <el-button type="primary" @click="submitComment" :loading="commentSubmitting" style="margin-top: 10px;">
                发布评论
            </el-button>
        </div>

        <!-- ===== 评论列表 ===== -->
        <div class="comment-list">
            <div class="comment-item" v-for="item in commentList" :key="item.id">
                <div class="comment-user">{{ item.nickname || '用户' + item.userId }}</div>
                <div class="comment-text">{{ item.content }}</div>
                <div class="comment-time">{{ item.createTime?.replace('T', ' ') }}</div>
            </div>
            <el-empty v-if="!commentList.length && !commentLoading" description="暂无评论" />
            <el-skeleton v-if="commentLoading" :rows="2" animated />
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, Loading } from '@element-plus/icons-vue'

import { getPostDetail } from '@/api/post'
import { getCommentList, addComment } from '@/api/comment'
import { toggleLike } from '@/api/like'
import { toggleFavorite } from '@/api/favorite'
import { aiSummary } from '@/api/ai'

import { marked } from 'marked'
import DOMPurify from 'dompurify'

const route = useRoute()
const router = useRouter()
const postId = ref(route.params.id)

const currentUserId = computed(() => localStorage.getItem('userId'))

const goToProfile = () => {
    if (postInfo.value?.userId) {
        router.push(`/user/${postInfo.value.userId}`)
    }
}

const goToChat = () => {
    const userId = localStorage.getItem('userId')
    if (!userId) {
        ElMessage.warning('请先登录')
        return
    }
    if (postInfo.value?.userId) {
        router.push({ path: '/chat', query: { to: postInfo.value.userId } })
    }
}

const renderMarkdown = (content) => {
    if (!content) return ''
    const html = marked.parse(content)
    return DOMPurify.sanitize(html)
}

// ========== 数据 ==========
const loading = ref(false)
const postInfo = ref(null)

const commentList = ref([])
const commentLoading = ref(false)
const commentText = ref('')
const commentSubmitting = ref(false)

// 🆕 点赞状态（用 postInfo.hasLiked 初始化）
const isLiked = ref(false)
const likeLoading = ref(false)  // 防连点

const isFavorited = ref(false)
const showSummary = ref(false)
const summaryLoading = ref(false)
const summaryContent = ref('')
const summaryError = ref('')

// ========== AI 总结 ==========
const toggleSummary = () => {
    if (showSummary.value) {
        showSummary.value = false
        return
    }
    showSummary.value = true
    if (summaryContent.value || summaryError.value) return
    generateSummary()
}

const generateSummary = async () => {
    summaryLoading.value = true
    summaryError.value = ''
    try {
        const res = await aiSummary(postId.value)
        if (res.code === 200) {
            summaryContent.value = res.data
        } else {
            summaryError.value = res.msg || '生成摘要失败'
        }
    } catch (error) {
        summaryError.value = 'AI 服务暂时不可用，请稍后再试'
    } finally {
        summaryLoading.value = false
    }
}

// ========== 加载帖子 ==========
const loadPost = async () => {
    loading.value = true
    try {
        const res = await getPostDetail(postId.value)
        postInfo.value = res.data
        isLiked.value = res.data?.hasLiked || false
        isFavorited.value = res.data?.isFavorited || false  // ✅ 加这一行
    } catch (error) {
        ElMessage.error('加载帖子失败')
    } finally {
        loading.value = false
    }
}

// ========== 评论 ==========
const loadComments = async () => {
    commentLoading.value = true
    try {
        const res = await getCommentList(postId.value)
        commentList.value = res.data || []
    } catch (error) {
        ElMessage.error('加载评论失败')
    } finally {
        commentLoading.value = false
    }
}

const submitComment = async () => {
    const text = commentText.value.trim()
    if (!text) {
        ElMessage.warning('评论内容不能为空')
        return
    }
    commentSubmitting.value = true
    try {
        await addComment({
            postId: postId.value,
            content: text
        })
        ElMessage.success('评论发布成功')
        commentText.value = ''
        await loadComments()
        if (postInfo.value) {
            postInfo.value.commentCount = (postInfo.value.commentCount || 0) + 1
        }
    } catch (error) {
        ElMessage.error('评论失败，请重试')
    } finally {
        commentSubmitting.value = false
    }
}

// ========== 🆕 点赞（核心修改） ==========
const handleLike = async () => {
    // 防连点：如果正在处理，直接返回
    if (likeLoading.value) return
    likeLoading.value = true

    try {
        const res = await toggleLike(postId.value)
        if (res.code === 200) {
            // 判断后端返回的是点赞还是取消
            const msg = res.data || res.msg || ''
            const isCancel = msg.includes('取消')

            if (isCancel) {
                isLiked.value = false
                if (postInfo.value) postInfo.value.likeCount = (postInfo.value.likeCount || 1) - 1
                ElMessage.success('取消点赞')
                sessionStorage.setItem('like_cancel_' + postId.value, '1')
            } else {
                isLiked.value = true
                if (postInfo.value) postInfo.value.likeCount = (postInfo.value.likeCount || 0) + 1
                ElMessage.success('点赞成功')
                sessionStorage.removeItem('like_cancel_' + postId.value)
            }
        } else {
            ElMessage.error(res.msg || '操作失败')
        }
    } catch (error) {
        ElMessage.error('操作失败')
    } finally {
        likeLoading.value = false
    }
}

// ========== 收藏 ==========
const handleFavorite = async () => {
    try {
        const res = await toggleFavorite(postId.value)
        isFavorited.value = !isFavorited.value
        ElMessage.success(res.msg || '操作成功')
        sessionStorage.setItem('favorites_need_refresh', '1')
    } catch (error) {
        ElMessage.error('操作失败')
    }
}

onMounted(() => {
    loadPost()
    loadComments()
})
</script>

<style scoped>
.post-detail {
    padding: 16px;
    background: #fff;
    min-height: 100vh;
}


.post-author {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f0;
}
.author-avatar {
    cursor: pointer;
    background: #409eff;
    color: #fff;
    font-weight: 600;
    flex-shrink: 0;
}
.author-name {
    cursor: pointer;
    font-weight: 600;
    color: #409eff;
    font-size: 16px;
}
.author-name:hover {
    text-decoration: underline;
}
.post-time {
    color: #999;
    font-size: 13px;
    margin-left: auto;
}


.post-content {
    color: #333;
    line-height: 1.6;
    margin: 12px 0;
    white-space: pre-wrap;
}


.action-bar {
    display: flex;
    gap: 24px;
    margin: 16px 0;
    padding-bottom: 16px;
    border-bottom: 1px solid #eee;
    font-size: 14px;
    color: #666;
}
.like-btn,
.fav-btn {
    cursor: pointer;
    user-select: none;
    display: flex;
    align-items: center;
    gap: 4px;
    transition: transform 0.2s;
}
.like-btn:active,
.fav-btn:active {
    transform: scale(0.95);
}
.heart,
.star {
    font-size: 18px;
}
.like-btn.active .heart {
    color: #f56c6c;
}
.fav-btn.active .star {
    color: #f7b731;
}

.ai-summary-section {
    margin: 16px 0;
    border: 1px solid #e8e8e8;
    border-radius: 8px;
    overflow: hidden;
    background: #fafbfc;
}
.ai-summary-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    cursor: pointer;
    user-select: none;
    transition: background 0.2s;
    font-size: 14px;
    color: #333;
}
.ai-summary-header:hover {
    background: #f0f2f5;
}
.ai-icon {
    font-size: 18px;
}
.arrow-icon {
    margin-left: auto;
    transition: transform 0.3s;
    font-size: 16px;
}
.arrow-icon.expanded {
    transform: rotate(180deg);
}
.ai-summary-body {
    padding: 0 16px 16px 16px;
}
.summary-loading {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #999;
    font-size: 14px;
    padding: 8px 0;
}
.summary-content {
    font-size: 14px;
    line-height: 1.6;
    color: #333;
    padding: 8px 0;
}
.summary-content p {
    margin: 0 0 8px 0;
}
.summary-error {
    color: #f56c6c;
    font-size: 14px;
    padding: 8px 0;
}
.summary-placeholder {
    color: #bbb;
    font-size: 14px;
    padding: 8px 0;
}

/* ===== 评论 ===== */
.comment-box {
    margin-bottom: 24px;
}
.comment-list {
    margin-top: 20px;
}
.comment-item {
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;
}
.comment-user {
    font-weight: 600;
    color: #409eff;
    font-size: 14px;
    margin-bottom: 4px;
}
.comment-text {
    color: #333;
    font-size: 14px;
    margin-bottom: 4px;
}
.comment-time {
    font-size: 12px;
    color: #999;
}
</style>