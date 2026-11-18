<template>
  <div class="records-page">
    <FamilyTopNav />

    <main class="page-wrap">
      <button type="button" class="mobile-back" @click="$router.push('/family/home')">
        <el-icon :size="15"><ArrowLeft /></el-icon>
        返回工作台
      </button>

      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">就医档案</h1>
          <p class="page-sub">按就诊时间查看家人的病历摘要与相关检查报告。</p>
        </div>
      </header>

      <div v-if="records.length > 0" class="records-grid">
        <article
          v-for="(record, index) in records"
          :key="record.id"
          class="record-card surface-card rv-rise"
          :style="{ '--i': index }"
        >
          <div class="card-header">
            <h2 class="record-title">{{ record.elderlyName || '就医记录' }}</h2>
            <span class="visit-date">{{ record.visitDate }}</span>
          </div>

          <div class="card-mid">
            <span class="department">{{ record.department || '未填写科室' }}</span>
          </div>

          <p class="diagnosis">{{ truncateText(record.diagnosis, 72) }}</p>

          <dl class="record-meta">
            <div>
              <dt>主诉</dt>
              <dd>{{ record.chiefComplaint || '未记录' }}</dd>
            </div>
            <div>
              <dt>用药</dt>
              <dd>{{ record.prescription || '未记录' }}</dd>
            </div>
          </dl>

          <div class="card-footer">
            <button
              type="button"
              class="detail-link"
              @click="$router.push(`/family/record/${record.id}`)"
            >
              查看详情
              <el-icon :size="14"><ArrowRight /></el-icon>
            </button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state surface-card rv-fade">
        <span class="empty-icon" aria-hidden="true">
          <el-icon :size="28"><Folder /></el-icon>
        </span>
        <h2 class="text-h2">暂无就医档案</h2>
        <p class="text-note">陪诊服务完成后，病历与检查报告会归档在这里。</p>
      </div>

      <div v-if="total > 0" class="records-footer">
        <span class="text-note">共 {{ total }} 条就医记录</span>
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          background
          layout="prev, pager, next"
          :pager-count="5"
          @current-change="fetchRecords"
        />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, ArrowRight, Folder } from '@element-plus/icons-vue'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'

const records = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

const truncateText = (text, maxLen) => {
  if (!text) return '-'
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

const fetchRecords = async () => {
  try {
    const res = await api.get('/record/page', {
      params: { current: pageNo.value, size: pageSize.value }
    })
    if (res.code === 200 && res.data) {
      records.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('Failed to load records:', e)
  }
}

onMounted(fetchRecords)
</script>

<style scoped>
.records-page {
  min-height: 100vh;
  background: var(--bg);
  padding-bottom: 40px;
}

.page-wrap {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 28px 24px;
}

.mobile-back {
  display: none;
  align-items: center;
  gap: 4px;
  margin-bottom: 14px;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  color: var(--muted);
  cursor: pointer;
}

.page-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.page-heading {
  margin: 0;
  font-size: clamp(24px, 2.6vw, 30px);
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.03em;
  color: var(--ink);
}

.page-sub {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--muted);
}

.records-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
  align-items: stretch;
}

.record-card {
  display: flex;
  flex-direction: column;
  padding: 22px;
  cursor: default;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.record-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: 0 10px 28px -16px rgba(16, 24, 40, 0.22);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.record-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--ink);
}

.visit-date {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--muted-2);
  font-variant-numeric: tabular-nums;
}

.card-mid {
  margin-top: 12px;
}

.department {
  display: inline-flex;
  padding: 3px 8px;
  background: var(--teal-soft);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--teal-deep);
}

.diagnosis {
  margin: 14px 0 0;
  font-size: 13.5px;
  line-height: 1.7;
  color: var(--ink-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.record-meta {
  display: grid;
  gap: 8px;
  margin: 14px 0 0;
  padding: 10px 12px;
  background: var(--surface-2);
  border-radius: 6px;
}

.record-meta > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-meta dt {
  flex-shrink: 0;
  font-size: 11.5px;
  color: var(--muted);
}

.record-meta dd {
  margin: 0;
  max-width: 68%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--ink-3);
  text-align: right;
}

.card-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px dashed var(--line);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--teal-deep);
  cursor: pointer;
}

.detail-link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.records-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 72px 24px;
  text-align: center;
}

.empty-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  border: 1px solid var(--line);
  border-radius: 12px;
  color: var(--muted);
  margin-bottom: 4px;
}

@media (max-width: 1000px) {
  .page-wrap {
    padding: 20px 16px 40px;
  }

  .mobile-back {
    display: inline-flex;
  }

  .record-card {
    padding: 18px;
  }
}

@media (max-width: 700px) {
  .records-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
