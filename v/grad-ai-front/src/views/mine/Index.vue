<template>
    <div class="mine-page">
       
        <div class="user-card">
          
            <el-avatar :size="64" class="avatar" @click="openAvatarUpload">
                <!-- 头像 -->
                <img
                    :src="userInfo.avatar ? 'http://localhost:8081' + userInfo.avatar : ''"
                    v-if="userInfo.avatar"
                    style="width: 60px; height: 60px; border-radius: 50%; object-fit: cover;"
                />
                <!-- 没有头像时显示默认文字 -->
                <span v-else style="width: 60px; height: 60px; border-radius: 50%; background: #ddd; display: inline-flex; align-items: center; justify-content: center; color: #666;">
    暂无头像
</span>
            </el-avatar>
            <div class="user-info">
                <div class="nickname">{{ userInfo.nickname || '未设置昵称' }}</div>
                <div class="school">{{ userInfo.school || '未设置' }} · {{ userInfo.major || '未设置' }}</div>
            </div>
            <el-icon class="edit-icon" @click="openEditDialog"><Edit /></el-icon>
        </div>

        <div class="menu-list">
            <div class="menu-item" @click="$router.push('/my-posts')">
                <el-icon><Document /></el-icon>
                <span>我的帖子</span>
                <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
            <div class="menu-item" @click="$router.push('/likes')">
            <span class="icon">❤️</span>
            <span>我的点赞</span>
            <el-icon><ArrowRight /></el-icon>
        </div>
            <div class="menu-item" @click="logout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
                <el-icon class="arrow"><ArrowRight /></el-icon>
            </div>
        </div>

        <el-dialog v-model="editVisible" title="编辑资料" width="90%">
            <el-form ref="editRef" :model="editForm" :rules="editRules" label-width="80px">
                <el-form-item label="昵称" prop="nickname">
                    <el-input v-model="editForm.nickname" />
                </el-form-item>
                <el-form-item label="学校" prop="school">
                    <el-input v-model="editForm.school" placeholder="请输入学校名称" />
                </el-form-item>
                <el-form-item label="专业" prop="major">
                    <el-input v-model="editForm.major" placeholder="请输入专业名称" />
                </el-form-item>
                <el-form-item label="兴趣方向" prop="interestTag">
                    <el-radio-group v-model="editForm.interestTag">
                        <el-radio :label="1">就业</el-radio>
                        <el-radio :label="2">考研</el-radio>
                        <el-radio :label="3">考公</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editVisible = false">取消</el-button>
                <el-button type="primary" @click="saveEdit" :loading="saveLoading">保存</el-button>
            </template>
        </el-dialog>

        <input
            type="file"
            ref="avatarInput"
            accept="image/*"
            style="display: none"
            @change="handleAvatarUpload"
        />
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    Edit,
    MagicStick,
    Document,
    Star,
    SwitchButton,
    ArrowRight
} from '@element-plus/icons-vue'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user'

const router = useRouter()
const userInfo = ref({})
const editVisible = ref(false)
const saveLoading = ref(false)
const editRef = ref(null)
const avatarInput = ref(null)

const editForm = ref({
    nickname: '',
    school: '',
    major: '',
    interestTag: 1
})

const editRules = {
    nickname: [
        { required: true, message: '昵称不能为空', trigger: 'blur' },
        { min: 2, max: 10, message: '2-10位', trigger: 'blur' }
    ]
}


const fetchUserInfo = async () => {
    try {
        const res = await getUserInfo()
        userInfo.value = res.data
        editForm.value = { ...res.data }
    } catch (err) {
        const local = localStorage.getItem('userInfo')
        if (local) userInfo.value = JSON.parse(local)
    }
}


const openEditDialog = () => {
    editForm.value = { ...userInfo.value }
    editVisible.value = true
}

const saveEdit = () => {
    editRef.value.validate(async (valid) => {
        if (!valid) return
        saveLoading.value = true
        try {
            await updateUserInfo(editForm.value)
            ElMessage.success('保存成功')
            editVisible.value = false
            fetchUserInfo()
        } finally {
            saveLoading.value = false
        }
    })
}


const openAvatarUpload = () => {
    avatarInput.value?.click()
}

const handleAvatarUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return

    // 校验文件类型
    if (!file.type.startsWith('image/')) {
        ElMessage.error('请上传图片文件')
        return
    }
    // 校验文件大小（2MB）
    if (file.size > 2 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过2MB')
        return
    }

    try {
        const formData = new FormData()
        formData.append('avatar', file)
        const res = await uploadAvatar(formData)
        ElMessage.success('头像更新成功')
        userInfo.value.avatar = res.data
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
        window.location.reload()
    } catch (error) {
        ElMessage.error('头像上传失败，请重试')
    }
}

const logout = () => {
    ElMessageBox.confirm('确定退出吗？', '提示').then(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        userInfo.value = null
        router.push('/login')
    }).catch(() => {})
}

onMounted(fetchUserInfo)
</script>

<style scoped>
.mine-page {
    padding-bottom: 20px;
}
.user-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 24px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    color: #fff;
    position: relative;
}
.avatar {
    cursor: pointer;
    border: 2px solid rgba(255, 255, 255, 0.3);
    flex-shrink: 0;
    background: rgba(255, 255, 255, 0.2);
}
.avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}
.user-info {
    flex: 1;
}
.nickname {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 6px;
}
.school {
    font-size: 14px;
    opacity: 0.9;
}
.edit-icon {
    font-size: 20px;
    cursor: pointer;
    padding: 4px;
}
.edit-icon:hover {
    opacity: 0.8;
}
.menu-list {
    margin: 12px 16px;
    background: #fff;
    border-radius: 8px;
    overflow: hidden;
}
.menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    font-size: 15px;
    color: #333;
    transition: background 0.2s;
}
.menu-item:hover {
    background: #f5f7fa;
}
.menu-item:last-child {
    border-bottom: none;
}
.menu-item .el-icon {
    font-size: 20px;
    color: #409eff;
}
.arrow {
    margin-left: auto;
    color: #ccc !important;
    font-size: 16px !important;
}
</style>