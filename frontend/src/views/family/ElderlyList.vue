<template>
  <div class="elderly-list-page">
    <FamilyTopNav />

    <main class="page-wrap">
      <button type="button" class="mobile-back" @click="$router.push('/family/home')">
        <el-icon :size="15"><ArrowLeft /></el-icon>
        返回工作台
      </button>

      <header class="page-intro rv-fade">
        <div>
          <h1 class="page-heading">老人档案</h1>
          <p class="page-sub">管理家人的基础信息与行动能力，预约陪诊时可直接选择。</p>
        </div>
        <el-button type="primary" @click="$router.push('/family/elderly/add')">
          <el-icon :size="16"><Plus /></el-icon>
          新增老人档案
        </el-button>
      </header>

      <div v-if="elderlyList.length > 0" class="elderly-grid">
        <article
          v-for="(elderly, index) in elderlyList"
          :key="elderly.id"
          class="elderly-card surface-card rv-rise"
          :style="{ '--i': index }"
        >
          <div class="card-head">
            <div class="avatar">{{ elderly.name?.charAt(0) || '老' }}</div>
            <div class="name-block">
              <div class="name-row">
                <h2 class="name">{{ elderly.name }}</h2>
                <el-tag type="info" size="small" class="mini-tag">
                  {{ genderLabel(elderly.gender) }}
                </el-tag>
                <span class="age">{{ computedAge(elderly.birthDate) }}岁</span>
              </div>
              <span class="mobility">{{ getMobilityLabel(elderly.mobility) }}</span>
            </div>
          </div>

          <dl class="meta-list">
            <div>
              <dt>联系电话</dt>
              <dd>{{ elderly.phone || '未填写' }}</dd>
            </div>
            <div>
              <dt>出生日期</dt>
              <dd>{{ elderly.birthDate || '未填写' }}</dd>
            </div>
          </dl>

          <div class="card-actions">
            <el-button plain @click="$router.push(`/family/elderly/${elderly.id}`)">
              编辑
            </el-button>
            <el-button type="danger" plain @click="handleDelete(elderly)">
              删除
            </el-button>
          </div>
        </article>
      </div>

      <div v-else class="empty-state surface-card rv-fade">
        <span class="empty-icon" aria-hidden="true">
          <el-icon :size="28"><User /></el-icon>
        </span>
        <h2 class="text-h2">还没有老人档案</h2>
        <p class="text-note">先添加一位家人的基础信息，之后就可以为 TA 预约陪诊。</p>
        <el-button type="primary" @click="$router.push('/family/elderly/add')">
          新增老人档案
        </el-button>
      </div>

      <div v-if="total > size" class="records-footer">
        <span class="text-note">共 {{ total }} 位老人</span>
        <el-pagination
          v-model:current-page="current"
          :page-size="size"
          :total="total"
          background
          layout="prev, pager, next"
          :pager-count="5"
          @current-change="fetchList"
        />
      </div>

      <section
        v-if="elderlyList.length > 0"
        class="tips-strip surface-card rv-rise"
        style="--i: 4"
      >
        <div class="tips-title">
          <h2 class="text-h2">档案使用小贴士</h2>
          <p class="text-note">完善信息，陪诊服务更顺畅</p>
        </div>
        <ul class="tips-items">
          <li>完善行动能力，方便陪诊师提前准备轮椅或搀扶。</li>
          <li>就诊前确认老人常用药、过敏史与紧急联系人。</li>
          <li>档案仅用于服务履约，敏感信息按角色脱敏展示。</li>
        </ul>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft, Plus, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/request'
import FamilyTopNav from '@/components/FamilyTopNav.vue'

const elderlyList = ref([])
const current = ref(1)
const size = ref(10)
const total = ref(0)

const computedAge = (birthDate) => {
  if (!birthDate) return '-'
  return new Date().getFullYear() - new Date(birthDate).getFullYear()
}

const getMobilityLabel = (mobility) => {
  const map = {
    WALK: '自主行走',
    ASSIST: '需搀扶',
    WHEELCHAIR: '轮椅'
  }
  return map[mobility] || mobility || '未填写'
}

const genderLabel = (gender) => {
  if (gender === 'MALE') return '男'
  if (gender === 'FEMALE') return '女'
  return '未填写'
}

const fetchList = async () => {
  try {
    const res = await api.get('/elderly/page', {
      params: { current: current.value, size: size.value }
    })
    if (res.code === 200 && res.data) {
      elderlyList.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载老人档案失败')
  }
}

const handleDelete = async (elderly) => {
  try {
    await ElMessageBox.confirm(
      `确定删除"${elderly.name}"的档案吗？删除后无法恢复。`,
      '确认删除',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await api.delete(`/elderly/${elderly.id}`)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (elderlyList.value.length === 1 && current.value > 1) current.value--
      fetchList()
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      if (e?.response?.status === 409 || e?.code === 409) {
        ElMessage.error('存在进行中的陪诊订单，无法删除')
      } else if (e !== 'cancel') {
        ElMessage.error('删除失败')
      }
    }
  }
}

onMounted(fetchList)
</script>

<style scoped>
.records-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 18px 0;
}

.elderly-list-page {
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

.elderly-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  align-items: stretch;
}

.elderly-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  transition: transform 0.18s ease, border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.elderly-card:hover {
  transform: translateY(-2px);
  border-color: var(--teal-light);
  box-shadow: 0 10px 28px -16px rgba(16, 24, 40, 0.22);
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.avatar {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 10px;
  font-size: 22px;
  font-weight: 700;
  color: var(--teal-deep);
}

.name-block {
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--ink);
}

.mini-tag {
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  font-size: 11px;
}

.age {
  font-size: 12.5px;
  color: var(--muted);
}

.mobility {
  display: inline-flex;
  margin-top: 8px;
  padding: 3px 8px;
  background: var(--teal-soft);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--teal-deep);
}

.meta-list {
  display: grid;
  gap: 10px;
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}

.meta-list > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.meta-list dt {
  font-size: 12.5px;
  color: var(--muted);
}

.meta-list dd {
  margin: 0;
  font-size: 13px;
  color: var(--ink-3);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
  padding-top: 18px;
}

.card-actions .el-button {
  flex: 1;
  height: 38px;
  font-size: 13px;
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
  background: var(--teal-soft);
  border: 1px solid var(--teal-light);
  border-radius: 12px;
  color: var(--teal-deep);
  margin-bottom: 4px;
}

.empty-state p {
  margin-bottom: 10px;
}

.tips-strip {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 2fr;
  gap: 20px;
  align-items: start;
  margin-top: 20px;
  padding: 18px 22px;
}

.tips-title .text-note {
  margin-top: 3px;
}

.tips-items {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.tips-items li {
  position: relative;
  padding-left: 12px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--muted);
}

.tips-items li::before {
  content: "";
  position: absolute;
  top: 7px;
  left: 0;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--teal);
}

@media (max-width: 1000px) {
  .page-wrap {
    padding: 20px 16px 40px;
  }

  .mobile-back {
    display: inline-flex;
  }

  .page-intro {
    align-items: flex-start;
  }

  .card-actions .el-button {
    height: 44px;
  }

  .tips-strip {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .tips-items {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}

@media (max-width: 700px) {
  .page-intro {
    flex-direction: column;
  }

  .page-intro .el-button {
    width: 100%;
    height: 44px;
  }
}
</style>
