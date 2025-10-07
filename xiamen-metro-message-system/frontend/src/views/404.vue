<template>
  <div class="not-found">
    <div class="not-found__container">
      <div class="not-found__content">
        <h1 class="not-found__code">404</h1>
        <h2 class="not-found__title">页面不存在</h2>
        <p class="not-found__description">
          抱歉，您访问的页面不存在或已被移除。
        </p>
        <el-button type="primary" @click="goHome">
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped lang="scss">
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f5f5f5;
}

.not-found__container {
  text-align: center;
  padding: 40px;
}

.not-found__content {
  max-width: 500px;
}

.not-found__code {
  font-size: 120px;
  font-weight: bold;
  color: #ddd;
  margin: 0;
  line-height: 1;
}

.not-found__title {
  font-size: 32px;
  color: #333;
  margin: 20px 0 16px;
}

.not-found__description {
  font-size: 16px;
  color: #666;
  margin: 0 0 32px;
  line-height: 1.6;
}
</style>