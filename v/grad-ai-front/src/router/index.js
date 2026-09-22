import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/user/Login.vue')
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/user/Register.vue')
    },
    {
        path: '/',
        component: MainLayout,
        redirect: '/home',
        children: [
            { path: 'home', name: 'Home', component: () => import('@/views/home/Index.vue') },
            { path: 'message', name: 'Message', component: () => import('@/views/message/Index.vue') },
            { path: 'favorites', name: 'Favorites', component: () => import('@/views/favorites/Index.vue') },
            { path: 'likes', name: 'Likes', component: () => import('@/views/likes/Index.vue') },
            { path: 'mine', name: 'Mine', component: () => import('@/views/mine/Index.vue') },
            { path: 'post/detail/:id', name: 'PostDetail', component: () => import('@/views/post/Detail.vue'), props: true },
            { path: 'post/add', name: 'PostAdd', component: () => import('@/views/post/Add.vue'), meta: { requireAuth: true } },  // ✅ 放这里
            { path: 'ai/chat', name: 'AiChat', component: () => import('@/views/ai/Chat.vue') },
            { path: 'my-posts', name: 'MyPosts', component: () => import('@/views/mine/MyPosts.vue') },
            { path: 'search', name: 'Search', component: () => import('@/views/search/Index.vue') },
            { path: 'user/:id', name: 'UserProfile', component: () => import('@/views/user/Profile.vue'), props: true },
            { path: 'chat', name: 'Chat', component: () => import('@/views/chat/Index.vue') }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.path === '/login' || to.path === '/register') {
        if (token) {
            next('/home')
        } else {
            next()
        }
        return
    }
    // ✅ 加上 /post/add，未登录不能发布
    const needAuth = ['/mine', '/message', '/favorites', '/likes', '/my-posts', '/chat', '/post/add']
    if (needAuth.includes(to.path)) {
        if (token) {
            next()
        } else {
            next({ path: '/login', query: { redirect: to.fullPath } })
        }
        return
    }
    next()
})

export default router