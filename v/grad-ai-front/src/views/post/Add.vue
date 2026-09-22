<template>
    <div class="publish-page">
        <el-card class="publish-card" shadow="never">
            <template #header>
                <div class="card-header">
                    <span class="header-icon">✍️</span>
                    <span>发布新帖子</span>
                </div>
            </template>

            <el-form :model="form" label-position="top" @submit.prevent="handleSubmit">
                <el-form-item label="标题">
                    <el-input
                            v-model="form.title"
                            placeholder="请输入标题，让更多人看到..."
                            size="large"
                            maxlength="100"
                            show-word-limit
                            clearable
                    />
                </el-form-item>

                <el-form-item label="分类">
                    <el-select
                            v-model="form.categoryId"
                            placeholder="选择帖子分类"
                            size="large"
                            style="width: 240px"
                    >
                        <el-option
                                v-for="cat in categories"
                                :key="cat.id"
                                :label="cat.name"
                                :value="cat.id"
                        />
                    </el-select>
                </el-form-item>

                <el-form-item label="正文内容">
                    <el-input
                            v-model="form.content"
                            type="textarea"
                            :rows="12"
                            placeholder="分享你的经验、问题或想法..."
                            maxlength="5000"
                            show-word-limit
                            resize="none"
                    />
                </el-form-item>

                <el-form-item>
                    <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
                        🚀 立即发布
                    </el-button>
                    <el-button size="large" @click="$router.back()">取 消</el-button>
                </el-form-item>
            </el-form>
        </el-card>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { addPost } from '@/api/post'
import { ElMessage } from 'element-plus'

const router = useRouter()
const form = ref({
    title: '',
    categoryId: null,
    content: ''
})
const submitting = ref(false)

// 写死分类，不用调接口
const categories = ref([
    { id: 1, name: '内推信息' },
    { id: 2, name: '实习资讯' },
    { id: 3, name: '面试经验' },
    { id: 4, name: '备考经验' },
    { id: 5, name: '复习资料' },
    { id: 6, name: '复试调剂' },
    { id: 7, name: '岗位报考' },
    { id: 8, name: '笔试备考' },
    { id: 9, name: '面试心得' },
    { id: 10, name: '考编资讯' },
    { id: 11, name: '笔试准备' },
    { id: 12, name: '面试技巧' }
])

const handleSubmit = async () => {
    if (!form.value.title.trim()) {
        ElMessage.warning('请输入标题')
        return
    }
    if (!form.value.categoryId) {
        ElMessage.warning('请选择分类')
        return
    }
    if (!form.value.content.trim()) {
        ElMessage.warning('请输入正文内容')
        return
    }

    submitting.value = true
    try {
        const res = await addPost(form.value)
        if (res.code === 200) {
            ElMessage.success('🎉 发布成功！')
            router.push('/my-posts')
        } else {
            ElMessage.error(res.msg || '发布失败')
        }
    } catch (e) {
        ElMessage.error('发布失败，请检查网络')
    } finally {
        submitting.value = false
    }
}
</script>

<style scoped>
.publish-page {
    max-width: 800px;
    margin: 24px auto;
    padding: 0 20px;
}
.publish-card {
    border-radius: 12px;
    border: none;
}
.card-header {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    display: flex;
    align-items: center;
    gap: 8px;
}
.header-icon {
    font-size: 22px;
}
:deep(.el-form-item__label) {
    font-weight: 500;
    color: #555;
    padding-bottom: 4px;
}
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
    border-radius: 8px;
}
</style>