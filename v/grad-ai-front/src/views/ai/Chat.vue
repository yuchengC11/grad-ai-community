<template>
    <div class="ai-chat-page">
        <div class="chat-window" ref="chatRef">
            <div class="msg-item"
                 v-for="item in chatList"
                 :key="item.id"
                 :class="item.role">
                <div class="bubble">
                    {{ item.content }}
                </div>
            </div>

            <div v-if="loading" class="msg-item ai">
                <div class="bubble">AI思考中...</div>
            </div>
        </div>

        <!-- 场景选择 -->
        <div class="persona-bar">
            <span
                v-for="p in personas"
                :key="p.key"
                class="persona-tag"
                :class="{ active: currentPersona === p.key }"
                @click="currentPersona = p.key">
                {{ p.label }}
            </span>
        </div>

        <div class="input-bar">
            <el-input
                v-model="inputText"
                placeholder="输入你的问题..."
                @keyup.enter="send"
                :disabled="loading"
            />
            <el-button
                type="primary"
                @click="send"
                :loading="loading"
                :disabled="!inputText.trim()"
            >
                发送
            </el-button>
        </div>
    </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { aiChat } from '@/api/ai'

const chatRef = ref(null)
const inputText = ref('')
const loading = ref(false)
const currentPersona = ref('employment')

const personas = [
    { key: 'employment', label: '就业建议' },
    { key: 'postgraduate', label: '考研经验' },
    { key: 'civil', label: '考公备考' },
    { key: 'job', label: '求职感悟' }
]

const chatList = ref([
    { id: 1, role: 'ai', content: '你好呀！我是你的AI小助手，有什么问题可以问我哦～ 😊' }
])

const send = async () => {
    const text = inputText.value.trim()
    if (!text || loading.value) return

    // 1. 添加用户消息
    chatList.value.push({ id: Date.now(), role: 'user', content: text })
    inputText.value = ''
    loading.value = true

    nextTick(() => {
        chatRef.value.scrollTop = chatRef.value.scrollHeight
    })

    try {
        // 2. 普通POST拿到完整回答
        const res = await aiChat({
            question: text,
            history: buildHistory(),
            persona: currentPersona.value
        })

        const fullAnswer = res.data || '抱歉，AI没有返回内容'

        // 3. 插入一条空的AI消息
        const aiMsgId = Date.now()
        chatList.value.push({ id: aiMsgId, role: 'ai', content: '' })
        loading.value = false

        // 4. 打字机效果：逐字显示
        let index = 0
        const timer = setInterval(() => {
            if (index < fullAnswer.length) {
                const msg = chatList.value.find(item => item.id === aiMsgId)
                if (msg) {
                    msg.content = fullAnswer.substring(0, index + 1)
                }
                index++
                nextTick(() => {
                    chatRef.value.scrollTop = chatRef.value.scrollHeight
                })
            } else {
                clearInterval(timer)
            }
        }, 15)  // 每15ms显示一个字符

    } catch (e) {
        chatList.value.push({
            id: Date.now(),
            role: 'ai',
            content: 'AI服务繁忙，请稍后再试'
        })
        loading.value = false
    }
}

const buildHistory = () => {
    const history = []
    const recent = chatList.value.slice(-10)
    for (const msg of recent) {
        history.push({
            role: msg.role === 'ai' ? 'assistant' : 'user',
            content: msg.content
        })
    }
    return history
}
</script>

<style scoped>
.ai-chat-page {
    height: calc(100vh - 110px);
    display: flex;
    flex-direction: column;
    background: #f5f7fa;
}
.chat-window {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
}
.msg-item {
    margin-bottom: 16px;
}
.msg-item.user {
    text-align: right;
}
.bubble {
    display: inline-block;
    max-width: 75%;
    padding: 10px 14px;
    border-radius: 12px;
    font-size: 14px;
    line-height: 1.6;
    white-space: pre-wrap;
    word-break: break-word;
}
.ai .bubble {
    background: #fff;
    border-top-left-radius: 2px;
    color: #333;
}
.user .bubble {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 2px;
}
.persona-bar {
    display: flex;
    gap: 8px;
    padding: 8px 16px;
    background: #fff;
    border-top: 1px solid #eee;
    overflow-x: auto;
}
.persona-tag {
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 13px;
    color: #666;
    background: #f0f2f5;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.2s;
}
.persona-tag.active {
    background: #409eff;
    color: #fff;
}
.input-bar {
    display: flex;
    gap: 10px;
    padding: 12px 16px;
    background: #fff;
    border-top: 1px solid #eee;
}
</style>