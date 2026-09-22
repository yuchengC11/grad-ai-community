<template>
    <div class="login-page">
        <div class="login-card">
            <h2 class="title">毕业生AI社区</h2>
            <p class="sub-title">就业 · 考研 · 考公 一站式交流平台</p>
            <el-form ref="loginRef" :model="form" :rules="rules" label-width="0">
                <el-form-item prop="username">
                    <el-input v-model="form.username" placeholder="请输入账号" size="large">
                        <template #prefix><el-icon><User /></el-icon></template>
                    </el-input>
                </el-form-item>
                <el-form-item prop="password">
                    <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
                        <template #prefix><el-icon><Lock /></el-icon></template>
                    </el-input>
                </el-form-item>
                <el-button type="primary" size="large" class="submit-btn" @click="handleLogin" :loading="loading">登录</el-button>
            </el-form>
            <div class="footer-tip">
                还没有账号？<span class="text" @click="$router.push('/register')">立即注册</span>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/user'

const router = useRouter()
const route = useRoute()

const loginRef = ref(null)
const loading = ref(false)

const form = ref({
    username: '',
    password: ''
})

const rules = {
    username: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 4, max: 16, message: '账号长度4-16位', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
    ]
}

const handleLogin = () => {
    loginRef.value.validate(async valid => {
        if (!valid) return
        loading.value = true
        try {
            const res = await login(form.value)
            const user = res.data.userInfo
            localStorage.setItem('token', res.data.token)
            localStorage.setItem('userId', user.id)
            localStorage.setItem('userInfo', JSON.stringify(user))
            ElMessage.success('登录成功')

            const redirect = route.query.redirect || '/home'
            router.push(redirect)
        } finally {
            loading.value = false
        }
    })
}
</script>

<style scoped>
.login-page {
    width: 100vw;
    height: 100vh;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
}
.login-card {
    width: 380px;
    background: #fff;
    padding: 40px 32px;
    border-radius: 12px;
    box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}
.title {
    text-align: center;
    font-size: 24px;
    color: #333;
    margin: 0 0 8px;
}
.sub-title {
    text-align: center;
    color: #999;
    font-size: 14px;
    margin-bottom: 30px;
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
</style>