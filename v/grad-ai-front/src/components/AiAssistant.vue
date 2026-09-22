<template>
    <div
        class="ai-assistant"
        :style="positionStyle"
        @mousedown="startDrag"
        @touchstart="startDragTouch"
    >
        <!-- 悬浮按钮 -->
        <div class="ai-float-btn" @click.stop="toggleChat" :class="{ active: isOpen }">
            <img src="/ai-avatar.png" alt="AI助手" class="ai-avatar" />
            <div class="ripple"></div>
            <span class="status-dot"></span>
        </div>

        <transition name="slide-up">

            <div v-if="isOpen" class="chat-panel" @mousedown.stop @touchstart.stop>
                <div class="chat-header">
                    <div class="header-left">
                        <span>🤖 AI 智能助手</span>
                        <span v-if="postContext && postContext.title" class="context-badge">
                         📄 已加载帖子上下文
                        </span>
                        <el-select
                            v-model="currentPersona"
                            size="small"
                            class="persona-select"
                            @change="onPersonaChange"
                        >
                            <el-option label="🎓 学长学姐" value="学长" />
                            <el-option label="💼 面试官" value="面试官" />
                            <el-option label="🧑‍🏫 导师" value="导师" />
                            <el-option label="🤖 默认" value="" />
                        </el-select>
                    </div>
                    <el-icon class="close-btn" @click="isOpen = false"><Close /></el-icon>
                </div>

                <!-- 快捷功能按钮 -->
                <div class="quick-actions">
                    <el-button size="small" round @click="quickSend('帮我总结一下考研经验')">📝 考研经验</el-button>
                    <el-button size="small" round @click="quickSend('有什么好的就业建议？')">💼 就业建议</el-button>
                    <el-button size="small" round @click="quickSend('考公如何备考？')">📚 考公备考</el-button>
                    <el-button size="small" round @click="quickSend('写一篇求职感悟')">✍️ 求职感悟</el-button>
                </div>

                <div class="chat-body" ref="chatBodyRef">
                    <div
                        v-for="(msg, index) in messages"
                        :key="index"
                        class="msg-item"
                        :class="msg.role"
                    >
                        <div class="bubble" v-html="renderMarkdown(msg.content)"></div>
                    </div>
                    <div v-if="loading" class="msg-item assistant">
                        <div class="bubble typing">思考中<span>.</span><span>.</span><span>.</span></div>
                    </div>
                </div>

                <div class="chat-footer">
                    <el-input
                        v-model="inputText"
                        placeholder="输入问题..."
                        size="large"
                        @keyup.enter="sendMessage"
                        clearable
                    >
                        <template #append>
                            <el-button type="primary" @click="sendMessage" :loading="loading">
                                发送
                            </el-button>
                        </template>
                    </el-input>
                </div>
            </div>
        </transition>
    </div>
</template>

<script setup>
import { ref, nextTick, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import { aiChat } from '@/api/ai'          // ✅ 改这里：aiChatStream → aiChat
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { inject } from 'vue'

// ✅ 新增：打字机定时器
let typeTimer = null

const currentPersona = ref('')
const postContext = inject('postContext', ref(null))
const onPersonaChange = (val) => {
    if (val) {
        const nameMap = { '学长': '学长学姐', '面试官': '面试官', '导师': '导师' }
        ElMessage.success(`已切换为「${nameMap[val] || val}」模式`)
    } else {
        ElMessage.info('已切换为「默认」模式')
    }
}

marked.setOptions({
    breaks: true,
    gfm: true
})

const position = ref({
    x: window.innerWidth - 100,
    y: window.innerHeight - 120
})
const isDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

const positionStyle = computed(() => ({
    left: position.value.x + 'px',
    top: position.value.y + 'px',
    transform: 'translate(-50%, -50%)'
}))

const startDrag = (e) => {
    if (e.target.closest('.chat-panel') || e.target.closest('.el-input')) return
    isDragging.value = true
    const rect = e.currentTarget.getBoundingClientRect()
    dragOffset.value = {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
    }
    document.addEventListener('mousemove', onDrag)
    document.addEventListener('mouseup', stopDrag)
    e.preventDefault()
}

const onDrag = (e) => {
    if (!isDragging.value) return
    position.value = {
        x: e.clientX - dragOffset.value.x,
        y: e.clientY - dragOffset.value.y
    }
    const el = document.querySelector('.ai-assistant')
    if (el) {
        const rect = el.getBoundingClientRect()
        const w = rect.width
        const h = rect.height
        if (position.value.x < 0) position.value.x = 0
        if (position.value.x + w > window.innerWidth) position.value.x = window.innerWidth - w
        if (position.value.y < 0) position.value.y = 0
        if (position.value.y + h > window.innerHeight) position.value.y = window.innerHeight - h
    }
}

const stopDrag = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onDrag)
    document.removeEventListener('mouseup', stopDrag)
}

const startDragTouch = (e) => {
    if (e.target.closest('.chat-panel') || e.target.closest('.el-input')) return
    const touch = e.touches[0]
    const rect = e.currentTarget.getBoundingClientRect()
    dragOffset.value = {
        x: touch.clientX - rect.left,
        y: touch.clientY - rect.top
    }
    document.addEventListener('touchmove', onDragTouch, { passive: false })
    document.addEventListener('touchend', stopDragTouch, { passive: false })
}

const onDragTouch = (e) => {
    e.preventDefault()
    const touch = e.touches[0]
    position.value = {
        x: touch.clientX - dragOffset.value.x,
        y: touch.clientY - dragOffset.value.y
    }
    const el = document.querySelector('.ai-assistant')
    if (el) {
        const rect = el.getBoundingClientRect()
        const w = rect.width
        const h = rect.height
        if (position.value.x < 0) position.value.x = 0
        if (position.value.x + w > window.innerWidth) position.value.x = window.innerWidth - w
        if (position.value.y < 0) position.value.y = 0
        if (position.value.y + h > window.innerHeight) position.value.y = window.innerHeight - h
    }
}

const stopDragTouch = () => {
    document.removeEventListener('touchmove', onDragTouch)
    document.removeEventListener('touchend', stopDragTouch)
}

const isOpen = ref(false)
const loading = ref(false)
const inputText = ref('')
const chatBodyRef = ref(null)

const messages = ref([
    { role: 'assistant', content: '你好呀！我是你的AI小助手，有什么问题可以问我哦～ 😊' }
])

const getHistory = () => {
    const history = []
    const start = Math.max(1, messages.value.length - 10)
    for (let i = start; i < messages.value.length; i++) {
        const msg = messages.value[i]
        if (msg.role !== 'user' && msg.role !== 'assistant') continue
        if (msg.content && (msg.content.includes('出错') || msg.content.includes('错误') || msg.content.includes('AI服务暂时不可用'))) {
            continue
        }
        history.push({
            role: msg.role === 'ai' ? 'assistant' : msg.role,
            content: msg.content || ''
        })
    }
    return history
}

const toggleChat = () => {
    isOpen.value = !isOpen.value
    if (isOpen.value) {
        nextTick(() => {
            scrollToBottom()
        })
    }
}

// ✅ 重写：普通 POST + 前端模拟打字效果
const sendMessage = async () => {
    const text = inputText.value.trim()
    if (!text) {
        ElMessage.warning('请输入问题')
        return
    }

    let finalQuestion = text
    if (postContext && postContext.value && postContext.value.title) {
        const context = `当前帖子标题：${postContext.value.title}\n帖子内容：${postContext.value.content}\n\n用户问题：`
        finalQuestion = context + text
    }

    messages.value.push({ role: 'user', content: text })
    inputText.value = ''
    scrollToBottom()

    const aiMsgIndex = messages.value.length
    messages.value.push({ role: 'assistant', content: '' })

    const history = getHistory()
    loading.value = true

    // 清除上一次的打字定时器，防止重叠
    if (typeTimer) {
        clearInterval(typeTimer)
        typeTimer = null
    }

    try {
        const res = await aiChat({
            question: finalQuestion,   // ✅ 把帖子上下文一起传给后端
            history: history,
            persona: currentPersona.value
        })

        // 兼容不同响应格式（字符串 / Result对象 / Axios响应）
        let fullAnswer = ''
        if (typeof res === 'string') {
            fullAnswer = res
        } else if (res?.data?.data) {
            fullAnswer = res.data.data
        } else if (res?.data) {
            fullAnswer = res.data
        } else {
            fullAnswer = '暂无回复'
        }

        let displayed = ''
        // 根据内容长度自动调整打字速度，总时长约 2 秒，每字最少 15ms
        const speed = Math.max(15, Math.min(50, 2000 / fullAnswer.length))

        typeTimer = setInterval(() => {
            if (displayed.length < fullAnswer.length) {
                displayed += fullAnswer[displayed.length]
                messages.value[aiMsgIndex].content = displayed
                scrollToBottom()
            } else {
                clearInterval(typeTimer)
                typeTimer = null
                loading.value = false
            }
        }, speed)

    } catch (error) {
        loading.value = false
        ElMessage.error('AI 服务暂时不可用，请稍后再试')
        if (!messages.value[aiMsgIndex].content) {
            messages.value[aiMsgIndex].content = '😅 哎呀，我好像出错了，稍后再试试吧～'
        }
        scrollToBottom()
    }
}

const quickSend = (question) => {
    inputText.value = question
    sendMessage()
}

// 新增：AI 回答格式清洗
const fixMarkdownFormat = (text) => {
    if (!text) return ''
    return text
        // 1. 句号/逗号后紧跟【标题】，中间加两个换行（让标题独占一行）
        .replace(/([。，；！？])\s*【/g, '$1\n\n【')
        // 2. 】后紧跟数字序号，中间加两个换行
        .replace(/】\s*(\d+\.)/g, '】\n\n$1')
        // 3. 】后紧跟普通文字，也加换行（防止标题和正文粘在一起）
        .replace(/】\s*([^\n\s【】\d])/g, '】\n\n$1')
        // 4. 序号后面没空格的，补一个空格（1.聚焦 → 1. 聚焦）
        .replace(/(\d+)\.(?=[^\s\d])/g, '$1. ')
        // 5. 多个连续换行合并为两个，避免太散
        .replace(/\n{3,}/g, '\n\n')
}
const renderMarkdown = (content) => {
    if (!content) return ''
    const cleaned = fixMarkdownFormat(content)
    const html = marked.parse(cleaned)
    return DOMPurify.sanitize(html)
}

const scrollToBottom = () => {
    if (chatBodyRef.value) {
        chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
}

watch(messages, () => {
    nextTick(scrollToBottom)
}, { deep: true })
</script>

<style scoped>
.context-badge {
    font-size: 12px;
    background: rgba(255, 255, 255, 0.25);
    padding: 2px 10px;
    border-radius: 12px;
    color: #fff;
    white-space: nowrap;
}
.ai-assistant {
    position: fixed;
    z-index: 999;
    cursor: grab;
    user-select: none;
    touch-action: none;
}
.ai-assistant:active {
    cursor: grabbing;
}

.ai-float-btn {
    width: 72px;
    height: 72px;
    border-radius: 50%;
    background: linear-gradient(135deg, #409eff, #66b1ff);
    box-shadow: 0 4px 20px rgba(64, 158, 255, 0.5);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    transition: all 0.3s ease;
}
.ai-float-btn:hover {
    transform: scale(1.05);
}
.ai-float-btn.active {
    box-shadow: 0 4px 30px rgba(64, 158, 255, 0.7);
}
.ai-avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    object-fit: contain;
    border: 2px solid #fff;
    animation: float 3s ease-in-out infinite;
    background: #fff;
}
.ripple {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    border: 2px solid rgba(64, 158, 255, 0.3);
    animation: ripple 2s ease-out infinite;
}
@keyframes ripple {
    0% { transform: scale(0.8); opacity: 1; }
    100% { transform: scale(1.5); opacity: 0; }
}
.status-dot {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 14px;
    height: 14px;
    background: #67c23a;
    border-radius: 50%;
    border: 2px solid #fff;
    animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.3); background: #f56c6c; }
}
@keyframes float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-6px); }
}

/* ===== 对话面板 ===== */
.chat-panel {
    position: absolute;
    bottom: 85px;
    right: 0;
    width: 380px;
    height: 520px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    border: 1px solid #e8e8e8;
    cursor: default;
}

/* ===== 头部 ===== */
.chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    height: 48px;
    background: #409eff;
    color: #fff;
    flex-shrink: 0;
}
.header-left {
    display: flex;
    align-items: center;
    gap: 12px;
}
.persona-select {
    width: 100px;
}
.persona-select :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    box-shadow: none;
}
.persona-select :deep(.el-input__inner) {
    color: #fff;
    font-size: 13px;
}
.persona-select :deep(.el-input__suffix) {
    color: rgba(255, 255, 255, 0.7);
}
.persona-select :deep(.el-select-dropdown__item) {
    font-size: 13px;
}
.close-btn {
    font-size: 20px;
    cursor: pointer;
    color: #fff;
    transition: color 0.2s;
}
.close-btn:hover {
    color: #ffd6d6;
}

/* ===== 快捷按钮 ===== */
.quick-actions {
    display: flex;
    gap: 8px;
    padding: 8px 12px;
    border-bottom: 1px solid #f0f0f0;
    flex-wrap: wrap;
    flex-shrink: 0;
    background: #fafafa;
}
.quick-actions .el-button {
    font-size: 12px;
    padding: 4px 12px;
    border-color: #d9d9d9;
    color: #555;
}
.quick-actions .el-button:hover {
    border-color: #409eff;
    color: #409eff;
}

/* ===== 聊天内容 ===== */
.chat-body {
    flex: 1;
    padding: 16px;
    overflow-y: auto;
    background: #f8f9fa;
}
.msg-item {
    margin-bottom: 12px;
    display: flex;
}
.msg-item.user {
    justify-content: flex-end;
}
.msg-item.assistant {
    justify-content: flex-start;
}
.bubble {
    max-width: 100%;
    padding: 8px 14px;
    border-radius: 16px;
    font-size: 14px;
    line-height: 1.5;
    word-wrap: break-word;
    overflow-wrap: break-word;
    word-break: break-word;
    overflow-x: auto;
}
.bubble pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    max-width: 100%;
    overflow-x: auto;
    background: #f6f8fa;
    padding: 12px;
    border-radius: 6px;
}
.bubble img {
    max-width: 100%;
    height: auto;
}
.bubble code {
    word-break: break-word;
    white-space: pre-wrap;
}
.msg-item.user .bubble {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 4px;
}
.msg-item.assistant .bubble {
    background: #fff;
    color: #333;
    border-top-left-radius: 4px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.typing span {
    animation: blink 1.4s infinite both;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
    0%, 80%, 100% { opacity: 0; }
    40% { opacity: 1; }
}

/* ===== 底部输入 ===== */
.chat-footer {
    padding: 12px 16px;
    border-top: 1px solid #eee;
    background: #fff;
    flex-shrink: 0;
}
.chat-footer :deep(.el-input-group__append) {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
}
.chat-footer :deep(.el-input-group__append .el-button) {
    color: #fff;
}

/* ===== 动画 ===== */
.slide-up-enter-active,
.slide-up-leave-active {
    transition: all 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
    opacity: 0;
    transform: translateY(30px) scale(0.9);
}
</style>