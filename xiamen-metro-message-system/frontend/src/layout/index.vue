<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '256px'" class="layout__aside">
      <div class="layout__logo">
        <img src="/logo.png" alt="厦门地铁" v-if="!isCollapse" />
        <img src="/logo-mini.png" alt="厦门地铁" v-else />
        <span v-if="!isCollapse">厦门地铁</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        :router="true"
        class="layout__menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Dashboard /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/devices">
          <el-icon><Monitor /></el-icon>
          <template #title>设备管理</template>
        </el-menu-item>
        <el-menu-item index="/messages">
          <el-icon><Message /></el-icon>
          <template #title>报文管理</template>
        </el-menu-item>
        <el-menu-item index="/analysis">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据分析</template>
        </el-menu-item>
        <el-menu-item index="/alerts">
          <el-icon><Bell /></el-icon>
          <template #title>告警管理</template>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><Folder /></el-icon>
          <template #title>文件管理</template>
        </el-menu-item>
        <el-menu-item index="/users" v-if="userStore.hasRole('ADMIN')">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/settings" v-if="userStore.hasRole('ADMIN')">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="layout__container">
      <!-- 顶部导航 -->
      <el-header class="layout__header">
        <div class="layout__header-left">
          <el-button
            type="text"
            @click="toggleCollapse"
            class="layout__collapse-btn"
          >
            <el-icon :size="20">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumbList"
              :key="item.path"
              :to="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="layout__header-right">
          <!-- 实时连接状态 -->
          <div class="layout__connection-status">
            <el-icon :color="isConnected ? '#52c41a' : '#f5222d'">
              <Connection />
            </el-icon>
            <span>{{ isConnected ? '已连接' : '未连接' }}</span>
          </div>

          <!-- 通知 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0">
            <el-button type="text" @click="showNotifications">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>

          <!-- 用户菜单 -->
          <el-dropdown @command="handleUserCommand">
            <div class="layout__user">
              <el-avatar :size="32">{{ userStore.userName.charAt(0) }}</el-avatar>
              <span class="layout__username">{{ userStore.userName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">个人设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主要内容 -->
      <el-main class="layout__main">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { realtimeService } from '@/utils/websocket'
import {
  Dashboard,
  Monitor,
  Message,
  DataAnalysis,
  Bell,
  Folder,
  User,
  Setting,
  Fold,
  Expand,
  Connection,
  ArrowDown
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 响应式数据
const isCollapse = ref(false)
const isConnected = ref(false)
const unreadCount = ref(0)

// 计算属性
const activeMenu = computed(() => route.path)

const breadcrumbList = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta.title as string
  }))
})

// 方法
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/user-settings')
      break
    case 'logout':
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      router.push('/login')
      break
  }
}

const showNotifications = () => {
  router.push('/notifications')
}

// WebSocket连接管理
const connectWebSocket = async () => {
  try {
    await realtimeService.connect()
    isConnected.value = true

    // 监听设备更新
    realtimeService.onMessage('device', (data) => {
      console.log('设备状态更新:', data)
    })

    // 监听告警
    realtimeService.onMessage('alert', (data) => {
      console.log('新告警:', data)
      unreadCount.value++
    })

    // 监听报文
    realtimeService.onMessage('message', (data) => {
      console.log('新报文:', data)
    })

    // 监听系统状态
    realtimeService.onMessage('system', (data) => {
      console.log('系统状态更新:', data)
    })
  } catch (error) {
    console.error('WebSocket连接失败:', error)
    isConnected.value = false
  }
}

// 断开WebSocket连接
const disconnectWebSocket = () => {
  realtimeService.disconnect()
  isConnected.value = false
}

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
})
</script>

<style scoped lang="scss">
.layout {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.layout__aside {
  background: #001529;
  transition: width 0.3s ease;
  overflow: hidden;
}

.layout__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  font-size: 18px;
  font-weight: 600;

  img {
    height: 32px;
    margin-right: 12px;
  }

  span {
    white-space: nowrap;
    overflow: hidden;
  }
}

.layout__menu {
  border-right: none;
  background: #001529;

  :deep(.el-menu-item) {
    color: rgba(255, 255, 255, 0.65);

    &:hover {
      background: #1890ff;
      color: white;
    }

    &.is-active {
      background: #1890ff;
      color: white;
    }
  }

  :deep(.el-sub-menu__title) {
    color: rgba(255, 255, 255, 0.65);

    &:hover {
      background: #1890ff;
      color: white;
    }
  }
}

.layout__container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.layout__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.layout__header-left {
  display: flex;
  align-items: center;
}

.layout__collapse-btn {
  margin-right: 20px;
}

.layout__header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.layout__connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.3s;

  &:hover {
    background: #f5f5f5;
  }
}

.layout__username {
  font-size: 14px;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout__main {
  flex: 1;
  padding: 0;
  overflow-y: auto;
  background: #f5f5f5;
}

@media (max-width: 768px) {
  .layout__aside {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s ease;

    &:not(.el-menu--collapse) {
      transform: translateX(0);
    }
  }

  .layout__container {
    margin-left: 0;
  }

  .layout__header {
    padding: 0 16px;
  }

  .layout__username {
    display: none;
  }

  .layout__connection-status {
    display: none;
  }
}
</style>