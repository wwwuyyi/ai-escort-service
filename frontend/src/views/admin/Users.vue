<template>
  <div class="admin-users">
    <div class="page-header">
      <el-button @click="$router.push('/admin/dashboard')">← 返回看板</el-button>
      <h2 class="page-title">用户管理</h2>
      <div style="width: 100px;"></div>
    </div>

    <!-- Search -->
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称"
        clearable
        style="width: 300px;"
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="handleSearch" style="margin-left: 12px;">
        搜索
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-card surface-card">
      <el-table
        :data="records"
        v-loading="loading"
        stripe
        style="width: 100%;"
      >
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="nickname" label="昵称" min-width="180" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :class="row.role === 'ADMIN' ? 'status-tag status-tag--progress' : 'status-tag status-tag--neutral'">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :class="row.status === 'ENABLED' ? 'status-tag status-tag--done' : 'status-tag status-tag--danger'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 'ENABLED' ? 'danger' : 'primary'"
              :plain="row.status !== 'ENABLED'"
              size="small"
              :loading="togglingId === row.id"
              @click="handleToggle(row)"
            >
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-block">
            <span class="empty-icon"><el-icon :size="24"><User /></el-icon></span>
            <h4>暂无匹配的用户</h4>
            <p>调整搜索关键词后重试，或等待新的家属和陪诊师注册。</p>
          </div>
        </template>
      </el-table>

      <div class="table-footer" v-if="total > 0">
        <span class="text-note">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, User } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const togglingId = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const params = { current: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await api.get('/admin/user/page', { params })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

async function handleToggle(row) {
  const oppositeStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionLabel = oppositeStatus === 'DISABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${actionLabel}用户 "${row.nickname || row.username}" 吗？`,
      `${actionLabel}用户`,
      {
        confirmButtonText: `确认${actionLabel}`,
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  togglingId.value = row.id
  try {
    await api.put(`/admin/user/${row.id}/status`, null, { params: { status: oppositeStatus } })
    ElMessage.success(`${actionLabel}成功`)
    await fetchData()
  } catch (e) {
    if (e?.response?.status === 400) {
      ElMessage.warning('禁止禁用管理员')
    } else {
      ElMessage.error(e?.response?.data?.message || '操作失败')
    }
  } finally {
    togglingId.value = null
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-users {
  min-height: 100vh;
  background: var(--bg);
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
}
.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>