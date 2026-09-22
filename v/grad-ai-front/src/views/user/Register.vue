<template>
    <div class="reg-page">
        <div class="reg-card">
            <h2 class="title">注册账号</h2>
            <el-form ref="regRef" :model="form" :rules="rules" label-width="0">
                <el-form-item prop="username">
                    <el-input v-model="form.username" placeholder="账号（4-16位）" size="large"></el-input>
                </el-form-item>
                <el-form-item prop="nickname">
                    <el-input v-model="form.nickname" placeholder="昵称（2-10位）" size="large"></el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="密码（6-20位）" size="large" show-password></el-input>
                </el-form-item>
                <el-form-item prop="interestTag">
                    <el-radio-group v-model="form.interestTag" class="full-radio">
                        <el-radio label="1">就业</el-radio>
                        <el-radio label="2">考研</el-radio>
                        <el-radio label="3">考公</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-button type="primary" size="large" class="submit-btn" @click="handleReg" :loading="loading">注册</el-button>
            </el-form>
            <div class="footer-tip">
                已有账号？<span class="text" @click="$router.push('/login')">去登录</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'

const router = useRouter()
const regRef = ref(null)
const loading = ref(false)

const form = ref({
    username: '',
    nickname: '',
    password: '',
    interestTag: '1'
})

const rules = {
    username: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 4, max: 16, message: '账号4-16位', trigger: 'blur' }
    ],
    nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' },
        { min: 2, max: 10, message: '昵称2-10位', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码6-20位', trigger: 'blur' }
    ],
    interestTag: [
        { required: true, message: '请选择兴趣方向', trigger: 'change' }
    ]
}

const handleReg = () => {
    regRef.value.validate(async valid => {
        if (!valid) return
        loading.value = true
        try {
            await register(form.value)
            ElMessage.success('注册成功，请登录')
            router.push('/login')
        } finally {
            loading.value = false
        }
    })
}
</script>

<style scoped>
.reg-page {
    width: 100vw;
    height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
}
.reg-card {
    width: 380px;
    background: #fff;
    padding: 40px 32px;
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}
.title {
    text-align: center;
    font-size: 22px;
    margin-bottom: 24px;
}
.submit-btn {
    width: 100%;
    margin-top: 10px;
}
.footer-tip {
    text-align: center;
    margin-top: 20px;
    font-size: 14px;
    color: #666;
}
.link {
    color: #409eff;
    cursor: pointer;
}
.full-radio {
    width: 100%;
    display: flex;
    justify-content: space-between;
}
</style>