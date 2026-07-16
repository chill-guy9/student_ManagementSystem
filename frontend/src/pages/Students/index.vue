<template>
  <div class="students-page">
    <PageHeader title="学生管理" subtitle="管理所有学生信息">
      <template #actions>
        <el-button type="primary" @click="openCreate" :icon="Plus">新增学生</el-button>
        <el-button @click="handleExport" :icon="Download">导出</el-button>
      </template>
    </PageHeader>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="query.grade" placeholder="年级" clearable style="width: 120px" @change="fetchData">
        <el-option v-for="g in GRADES" :key="g.value" :label="g.label" :value="g.value" />
      </el-select>
      <el-select v-model="query.major" placeholder="专业" clearable style="width: 160px" @change="fetchData">
        <el-option v-for="m in MAJORS" :key="m.value" :label="m.label" :value="m.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option v-for="s in STUDENT_STATUS" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <SearchBox v-model="query.keyword" placeholder="搜索学号/姓名..." @search="fetchData" />
    </div>

    <!-- Table -->
    <DataTable
      :data="tableData"
      :columns="columns"
      :total="total"
      :page="query.page"
      :page-size="query.pageSize"
      :loading="loading"
      show-index
      @row-click="openDetail"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <template #name="{ row }">
        <span class="name-cell">{{ row.name }}</span>
        <span class="name-sub">{{ row.studentId }}</span>
      </template>
      <template #status="{ row }">
        <PulseBadge :type="statusType(row.status)">{{ row.statusLabel || statusLabel(row.status) }}</PulseBadge>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
        <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
      </template>
    </DataTable>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" title="学生详情" size="480px" :append-to-body="true">
      <div class="detail-section" v-if="currentStudent">
        <div class="detail-header">
          <div class="detail-avatar">{{ currentStudent.name?.charAt(0) }}</div>
          <div class="detail-identity">
            <h3>{{ currentStudent.name }}</h3>
            <span class="detail-no">{{ currentStudent.studentId }}</span>
          </div>
          <PulseBadge :type="statusType(currentStudent.status)">{{ currentStudent.statusLabel || statusLabel(currentStudent.status) }}</PulseBadge>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">性别</span>
            <span class="detail-value">{{ currentStudent.genderLabel }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">年级</span>
            <span class="detail-value">{{ currentStudent.grade }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">专业</span>
            <span class="detail-value">{{ currentStudent.major }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">班级</span>
            <span class="detail-value">{{ currentStudent.className }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">入学日期</span>
            <span class="detail-value">{{ currentStudent.enrollmentDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">电话</span>
            <span class="detail-value">{{ currentStudent.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">邮箱</span>
            <span class="detail-value">{{ currentStudent.email }}</span>
          </div>
          <div class="detail-item full-width">
            <span class="detail-label">地址</span>
            <span class="detail-value">{{ currentStudent.address }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- Create/Edit Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑学生' : '新增学生'" width="680px" :append-to-body="true">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="form.name" placeholder="请输入姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-select v-model="form.gender" style="width: 100%">
                    <el-option label="男" :value="1" />
                    <el-option label="女" :value="0" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="学业信息" name="academic">
          <el-form :model="form" label-width="80px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="年级">
                  <el-select v-model="form.grade" style="width: 100%">
                    <el-option v-for="g in GRADES" :key="g.value" :label="g.label" :value="g.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="专业">
                  <el-select v-model="form.major" style="width: 100%">
                    <el-option v-for="m in MAJORS" :key="m.value" :label="m.label" :value="m.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="班级">
                  <el-input v-model="form.className" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="状态">
                  <el-select v-model="form.status" style="width: 100%">
                    <el-option v-for="s in STUDENT_STATUS" :key="s.value" :label="s.label" :value="s.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="入学日期">
                  <el-date-picker v-model="form.enrollmentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="联系信息" name="contact">
          <el-form :model="form" label-width="80px">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="电话">
                  <el-input v-model="form.phone" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="地址">
                  <el-input v-model="form.address" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import DataTable from '@/components/common/DataTable.vue'
import PulseBadge from '@/components/ui/PulseBadge.vue'
import SearchBox from '@/components/ui/SearchBox.vue'
import { studentApi } from '@/api/student'
import { useExport } from '@/composables/useExport'
import { GRADES, MAJORS, STUDENT_STATUS } from '@/utils/constants'
import type { Student, StudentQuery, StudentForm } from '@/types/student'

const { exportStudents } = useExport()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Student[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const modalVisible = ref(false)
const isEdit = ref(false)
const activeTab = ref('basic')
const currentStudent = ref<Student | null>(null)
const formRef = ref<FormInstance>()

const query = reactive<StudentQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  grade: '',
  major: '',
  status: '',
})

const form = reactive<StudentForm>({
  name: '',
  gender: 1,
  birthDate: '',
  phone: '',
  email: '',
  grade: '',
  major: '',
  className: '',
  status: 'active',
  enrollmentDate: '',
  address: '',
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

const columns = [
  { prop: 'name', label: '姓名', minWidth: 140 },
  { prop: 'grade', label: '年级', width: 90 },
  { prop: 'major', label: '专业', minWidth: 140 },
  { prop: 'className', label: '班级', width: 100 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'phone', label: '电话', width: 130 },
]

function statusType(status: string): 'success' | 'warning' | 'error' | 'info' | 'cyan' {
  const map: Record<string, 'success' | 'warning' | 'error' | 'info' | 'cyan'> = {
    active: 'success',
    suspended: 'warning',
    graduated: 'info',
    dropped: 'error',
  }
  return map[status] || 'cyan'
}

function statusLabel(status: string): string {
  const map: Record<string, string> = { active: '在读', suspended: '休学', graduated: '毕业', dropped: '退学' }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res = await studentApi.getList(query)
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  query.page = page
  fetchData()
}

function handleSizeChange(size: number) {
  query.pageSize = size
  query.page = 1
  fetchData()
}

function openDetail(row: Student) {
  currentStudent.value = row
  drawerVisible.value = true
}

function openCreate() {
  isEdit.value = false
  activeTab.value = 'basic'
  Object.assign(form, {
    name: '', gender: 1, birthDate: '', phone: '', email: '',
    grade: '', major: '', className: '', status: 'active', enrollmentDate: '', address: '',
  })
  modalVisible.value = true
}

function openEdit(row: Student) {
  isEdit.value = true
  activeTab.value = 'basic'
  Object.assign(form, {
    name: row.name, gender: row.gender, birthDate: row.birthDate,
    phone: row.phone, email: row.email, grade: row.grade, major: row.major,
    className: row.className, status: row.status, enrollmentDate: row.enrollmentDate,
    address: row.address,
  })
  currentStudent.value = row
  modalVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && currentStudent.value) {
      await studentApi.update(currentStudent.value.studentId, form)
      ElMessage.success('更新成功')
    } else {
      await studentApi.create(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: Student) {
  try {
    await ElMessageBox.confirm(`确定删除学生 ${row.name} 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await studentApi.delete(row.studentId)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

function handleExport() {
  exportStudents(query)
}

onMounted(fetchData)
</script>

<style scoped>
.students-page {
  animation: fade-in-up 0.4s ease-out;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.name-cell {
  display: block;
  font-weight: 500;
  color: var(--text-primary);
}

.name-sub {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.gpa-value {
  font-family: var(--font-mono);
  font-weight: 600;
}

.gpa-high { color: var(--color-success); }
.gpa-mid { color: var(--color-warning); }
.gpa-low { color: var(--color-error); }

/* Detail Drawer */
.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-base);
}

.detail-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: var(--accent-cyan-dim);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-size: 20px;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-identity {
  flex: 1;
}

.detail-identity h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.detail-no {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 14px;
  color: var(--text-primary);
}
</style>
