<template>
    <div class="main-layout">
        <header class="top-nav">
            <div class="nav-container">
                <div class="nav-left">
                    <img src="/logo.png" alt="Logo" class="logo" />
                    <span class="brand">毕业生互助社区</span>
                    <span
                        v-for="tab in tabs"
                        :key="tab.key"
                        class="nav-link"
                        :class="{ active: currentTab === tab.key }"
                        @click="switchTab(tab.key)"
                    >
                        {{ tab.label }}
                    </span>
                </div>

                <div class="nav-center">
                    <el-input
                        v-model="searchKeyword"
                        placeholder="搜索"
                        size="large"
                        clearable
                        @keyup.enter="handleSearch"
                        class="search-input"
                    >
                        <template #suffix>
                            <el-icon class="search-btn" @click="handleSearch">
                                <Search />
                            </el-icon>
                        </template>
                    </el-input>
                </div>

                <div class="nav-right">
                    <el-icon class="icon-btn" @click="$router.push('/favorites')" title="我的收藏">
                        <Star />
                    </el-icon>
                    <el-icon class="icon-btn" @click="$router.push('/message')" title="消息">
                        <Bell />
                    </el-icon>

                    <div class="user-area">
                        <template v-if="userInfo">
                            <div class="user-profile" @click="$router.push('/mine')">
                                <el-avatar :size="32" class="user-avatar"
                                           :src="userInfo?.avatar ? 'http://localhost:8081' + userInfo.avatar : undefined">
                                    {{ userInfo?.nickname?.charAt(0) || 'U' }}
                                </el-avatar>
                                <span class="username">{{ userInfo?.nickname }}</span>
                            </div>
                            <el-button size="small" link @click.stop="logout" class="logout-btn">
                                退出
                            </el-button>
                        </template>
                        <template v-else>
                            <el-button size="small" @click="$router.push('/login')">登录</el-button>
                            <el-button size="small" type="primary" @click="$router.push('/register')">注册</el-button>
                        </template>
                    </div>
                </div>
            </div>
        </header>

        <main class="page-body">
            <router-view />
        </main>
        <AiAssistant />
    </div>
</template>

<script setup>
import AiAssistant from '@/components/AiAssistant.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Star, Search } from '@element-plus/icons-vue'
import { getUserInfo } from '@/api/user'

const router = useRouter()
const route = useRoute()

const tabs = [
    { key: 'home', label: '首页' },
    { key: 'kaoyan', label: '考研' },
    { key: 'gongkao', label: '考公' },
    { key: 'kaobian', label: '考编' },
    { key: 'jiuye', label: '就业' }
]

const currentTab = computed({
    get: () => route.query.tab || 'home',
    set: (val) => {
        router.push({ path: '/home', query: { tab: val } })
    }
})

const switchTab = (key) => {
    currentTab.value = key
}

const searchKeyword = ref('')
const handleSearch = () => {
    const keyword = searchKeyword.value.trim()
    if (!keyword) {
        ElMessage.warning('请输入搜索关键词')
        return
    }
    router.push({ path: '/search', query: { keyword } })
}

const userInfo = ref(null)

const fetchUserInfo = async () => {
    const token = localStorage.getItem('token')
    if (token) {
        try {
            const res = await getUserInfo()
            userInfo.value = res.data
        } catch {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            userInfo.value = null
        }
    } else {
        userInfo.value = null
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

onMounted(() => {
    fetchUserInfo()
    if (route.path === '/' || route.path === '') {
        router.replace({ path: '/home', query: { tab: 'home' } })
    }
})
</script>

<style scoped>
.main-layout {
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f5f7fa;
}

.top-nav {
    height: 64px;
    background: #ffffff;
    border-bottom: 1px solid #e8e8e8;
    flex-shrink: 0;
    position: sticky;
    top: 0;
    z-index: 100;
    display: flex;
    justify-content: center;
    padding: 0 20px;
}

.nav-container {
    width: 100%;
    max-width: 100%;
    display: flex;
    align-items: center;
    gap: 16px;
}

.nav-left {
    display: flex;
    align-items: center;
    gap: 20px;
    flex-shrink: 0;
}

.logo {
    height: 32px;
    width: auto;
}

.brand {
    font-size: 18px;
    font-weight: 600;
    color: #409eff;
    white-space: nowrap;
    margin-right: 4px;
}

.nav-link {
    font-size: 15px;
    color: #666;
    cursor: pointer;
    padding: 4px 0;
    border-bottom: 3px solid transparent;
    transition: all 0.2s;
    white-space: nowrap;
}
.nav-link.active {
    color: #409eff;
    font-weight: 600;
    border-bottom-color: #409eff;
}
.nav-link:hover {
    color: #409eff;
}

.nav-center {
    flex: 1;
    max-width: 600px;
    min-width: 120px;
    margin-left: 200px;
}
.search-input {
    width: 100%;
}
.search-btn {
    font-size: 18px;
    cursor: pointer;
    color: #909399;
    transition: color 0.2s;
}
.search-btn:hover {
    color: #409eff;
}

.nav-right {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    margin-left: auto;
}

.icon-btn {
    font-size: 22px;
    color: #666;
    cursor: pointer;
    transition: color 0.2s;
}
.icon-btn:hover {
    color: #409eff;
}

.user-area {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: 14px;
}

.user-profile {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 20px;
    transition: background 0.2s;
}
.user-profile:hover {
    background: #f0f2f5;
}
.user-avatar {
    background: #409eff;
    color: #fff;
    font-weight: 600;
    font-size: 14px;
}
.username {
    font-size: 14px;
    color: #333;
    max-width: 60px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.logout-btn {
    color: #999;
}

.page-body {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 0;
}

@media (max-width: 900px) {
    .nav-link {
        font-size: 13px;
    }
    .brand {
        font-size: 16px;
    }
    .nav-left {
        gap: 12px;
    }
}
@media (max-width: 700px) {
}
</style>