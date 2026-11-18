<template>
  <div class="admin-evaluations">
    <div class="admin-header">
    <el-button @click="$router.push('/admin/dashboard')">← 返回看板</el-button>
    <h2 class="page-title" style="margin:0">评价管理</h2>
  </div>

    <!-- Table -->
    <div class="table-card surface-card">
      <el-table
        :data="records"
        v-loading="loading"
        stripe
        style="width: 100%;"
      >
        <el-table-column prop="orderNo" label="订单编号" width="160" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }">
            <el-rate
              :model-value="row.score"
              disabled
              show-score
              text-color="var(--star)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="评价时间" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :class="row.isHidden ? 'status-tag status-tag--danger' : 'status-tag status-tag--done'">
              {{ row.isHidden ? '已隐藏' : '可见' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.isHidden"
              type="danger"
              plain
              size="small"
              @click="handleHide(row)"
            >
              隐藏
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-block">
            <span class="empty-icon"><el-icon :size="24"><Star /></el-icon></span>
            <h4>暂无用户评价</h4>
            <p>后续用户提交评价后，会在这里展示，方便您了解服务口碑。</p>
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

    <!-- Hide Reason Dialog -->
    <el-dialog v-model="hideVisible" title="隐藏评价" width="450px" destroy-on-close>
      <p>确认隐藏订单 "{{ hideTarget?.orderNo }}" 的评价吗？</p>
      <el-input
        v-model="hideReason"
        type="textarea"
        :rows="3"
        placeholder="请输入隐藏原因（选填）"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="hideVisible = false">取消</el-button>
        <el-button type="danger" :loading="hiding" @click="submitHide">
          确认隐藏
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import api from '@/api/request'

const loading = ref(false)
const hiding = ref(false)
const records = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

// Hide dialog
const hideVisible = ref(false)
const hideTarget = ref(null)
const hideReason = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await api.get('/admin/evaluation/page', {
      params: { current: page.value, size: size.value }
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('获取评价列表失败')
  } finally {
    loading.value = false
  }
}

function handleHide(row) {
  hideTarget.value = row
  hideReason.value = ''
  hideVisible.value = true
}

async function submitHide() {
  hiding.value = true
  try {
    await api.put(`/admin/evaluation/${hideTarget.value.id}/hide`, null, {
      params: { reason: hideReason.value }
    })
    ElMessage.success('评价已隐藏')
    hideVisible.value = false
    await fetchData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    hiding.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.admin-evaluations {
  min-height: 100vh;
  background: var(--bg);
  padding: 24px;
  max-width: 1280px;
  margin: 0 auto;
}
.admin-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>