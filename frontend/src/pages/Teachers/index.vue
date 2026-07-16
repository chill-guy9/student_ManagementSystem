<template>
  <div class="teachers-page">
    <PageHeader title="教师管理" subtitle="管理所有教师信息">
      <template #actions>
        <el-button type="primary" @click="openCreate" :icon="Plus">新增教师</el-button>
        <el-button @click="handleExport" :icon="Download">导出</el-button>
      </template>
    </PageHeader>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="query.department" placeholder="院系" clearable style="width: 140px" @change="fetchData">
        <el-option v-for="d in DEPARTMENTS" :key="d.value" :label="d.label" :value="d.value" />
      </el-select>
      <el-select v-model="query.title" placeholder="职称" clearable style="width: 140px" @change="fetchData">
        <el-option v-for="t in TEACHER_TITLES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="在职" value="1" />
        <el-option label="离职" value="0" />
      </el-select>
      <SearchBox v-model="query.keyword" placeholder="搜索工号/姓名..." @search="fetchData" />
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
        <span class="name-sub">{{ row.teacherId }}</span>
      </template>
      <template #status="{ row }">
        <PulseBadge :type="statusType(row.status)">{{ statusLabel(row.status) }}</PulseBadge>
      </template>      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click.stop="openEdit(row)">编辑</el-button>
        <el-button link type="danger" size="small" @click.stop="handleDelete(row)">删除</el-button>
      </template>
    </DataTable>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" title="教师详情" size="520px" :append-to-body="true">
      <div class="detail-section" v-if="currentTeacher">
        <div class="detail-header">
          <div class="detail-avatar">{{ currentTeacher.name?.charAt(0) }}</div>
          <div class="detail-identity">
            <h3>{{ currentTeacher.name }}</h3>
            <span class="detail-no">{{ currentTeacher.teacherId }}</span>
          </div>
          <PulseBadge :type="statusType(currentTeacher.status)">{{ statusLabel(currentTeacher.status) }}</PulseBadge>
        </div>

        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">性别</span>
            <span class="detail-value">{{ currentTeacher.genderLabel }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">院系</span>
            <span class="detail-value">{{ currentTeacher.department }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">职称</span>
            <span class="detail-value">{{ currentTeacher.title }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">电话</span>
            <span class="detail-value">{{ currentTeacher.phone }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">邮箱</span>
            <span class="detail-value">{{ currentTeacher.email }}</span>
          </div>
        </div>

        <!-- Courses -->
        <div class="courses-section">
          <h4 class="section-title">授课列表</h4>
          <div class="course-list" v-if="currentTeacher.courses?.length">
            <div v-for="course in currentTeacher.courses" :key="course.id" class="course-item">
              <div class="course-info">
                <span class="course-name">{{ course.courseName }}</span>
                <span class="course-code">{{ course.courseCode }}</span>
              </div>
              <div class="course-meta">
                <span>{{ course.semester }}</span>
                <span>{{ course.hours }}学时</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-courses">暂无授课信息</div>
        </div>
      </div>
    </el-drawer>

    <!-- Create/Edit Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑教师' : '新增教师'" width="600px" :append-to-body="true">
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
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="院系">
              <el-select v-model="form.department" style="width: 100%">
                <el-option v-for="d in DEPARTMENTS" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称">
              <el-select v-model="form.title" style="width: 100%">
                <el-option v-for="t in TEACHER_TITLES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="在职" :value="1" />
                <el-option label="离职" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
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
        </el-row>
      </el-form>
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
import { teacherApi } from '@/api/teacher'
import { useExport } from '@/composables/useExport'
import { DEPARTMENTS, TEACHER_TITLES } from '@/utils/constants'
import type { Teacher, TeacherQuery, TeacherForm } from '@/types/teacher'

const { exportTeachers } = useExport()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Teacher[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const modalVisible = ref(false)
const isEdit = ref(false)
const currentTeacher = ref<Teacher | null>(null)
const formRef = ref<FormInstance>()

const query = reactive<TeacherQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  department: '',
  title: '',
  status: '',
})

const form = reactive<TeacherForm>({
  name: '',
  gender: 1,
  birthDate: '',
  phone: '',
  email: '',
  department: '',
  title: '',
  status: 1,
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

const columns = [
  { prop: 'name', label: '姓名', minWidth: 140 },
  { prop: 'department', label: '院系', width: 120 },
  { prop: 'title', label: '职称', width: 100 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'phone', label: '电话', width: 130 },
  { prop: 'email', label: '邮箱', minWidth: 160 },
]

function statusType(status: number): 'success' | 'warning' | 'info' | 'cyan' {
  const map: Record<number, 'success' | 'warning' | 'info' | 'cyan'> = {
    1: 'success',
    0: 'warning',
  }
  return map[status] || 'cyan'
}

function statusLabel(status: number): string {
  const map: Record<number, string> = { 1: '在职', 0: '离职' }
  return map[status] || String(status)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await teacherApi.getList(query)
    tableData.value = res.data?.records ?? []
    total.value = res.data?.total ?? 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) { query.page = page; fetchData() }
function handleSizeChange(size: number) { query.pageSize = size; query.page = 1; fetchData() }

function openDetail(row: Teacher) {
  currentTeacher.value = row
  drawerVisible.value = true
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    name: '', gender: 1, birthDate: '', phone: '', email: '',
    department: '', title: '', status: 1,
  })
  modalVisible.value = true
}

function openEdit(row: Teacher) {
  isEdit.value = true
  Object.assign(form, {
    name: row.name, gender: row.gender, birthDate: row.birthDate,
    phone: row.phone, email: row.email, department: row.department, title: row.title,
    status: row.status,
  })
  currentTeacher.value = row
  modalVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentTeacher.value) {
      await teacherApi.update(currentTeacher.value.teacherId, form)
      ElMessage.success('更新成功')
    } else {
      await teacherApi.create(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function handleDelete(row: Teacher) {
  try {
    await ElMessageBox.confirm(`确定删除教师 ${row.name} 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    await teacherApi.delete(row.teacherId)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

function handleExport() { exportTeachers(query) }

onMounted(fetchData)
</script>

<style scoped>
.teachers-page {
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

.detail-identity { flex: 1; }
.detail-identity h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
.detail-no { font-family: var(--font-mono); font-size: 12px; color: var(--text-muted); }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
.detail-value { font-size: 14px; color: var(--text-primary); }

.courses-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border-base);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.course-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: var(--bg-elevated);
  border: 1px solid var(--border-base);
  border-radius: 8px;
}

.course-info { display: flex; flex-direction: column; }
.course-name { font-size: 13px; font-weight: 500; color: var(--text-primary); }
.course-code { font-size: 11px; color: var(--text-muted); font-family: var(--font-mono); }

.course-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.empty-courses {
  text-align: center;
  padding: 24px;
  color: var(--text-muted);
  font-size: 13px;
}
</style>
