<template>
  <div class="admins-page">
    <PageHeader title="管理员管理" subtitle="管理系统管理员与权限">
      <template #actions>
        <el-button type="primary" @click="openCreate" :icon="Plus">新增管理员</el-button>
      </template>
    </PageHeader>

    <!-- Filters -->
    <div class="filter-bar">
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px" @change="fetchData">
        <el-option v-for="(label, key) in ROLE_LABELS" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="fetchData">
        <el-option label="启用" value="1" />
        <el-option label="禁用" value="0" />
      </el-select>
      <SearchBox v-model="query.keyword" placeholder="搜索用户名/姓名..." @search="fetchData" />
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
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    >
      <template #username="{ row }">
        <span class="name-cell">{{ row.username }}</span>
        <span class="name-sub">{{ row.realName }}</span>
      </template>
      <template #role="{ row }">
        <el-tag :color="ROLE_COLORS[row.role]" effect="dark" size="small" style="border: none; color: #fff">
          {{ ROLE_LABELS[row.role] }}
        </el-tag>
      </template>
      <template #status="{ row }">
        <PulseBadge :type="row.status === 1 ? 'success' : 'error'">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </PulseBadge>
      </template>
      <template #lastLoginAt="{ row }">
        <span class="mono-text">{{ row.lastLoginAt || '从未登录' }}</span>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
        <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleStatus(row)">
          {{ row.status === 1 ? '禁用' : '启用' }}
        </el-button>
        <el-button link type="danger" size="small" @click="handleDelete(row)" v-if="row.role !== 'super_admin'">删除</el-button>
      </template>
    </DataTable>

    <!-- Permission Matrix -->
    <GlowCard color="cyan" class="permission-card">
      <template #header>
        <div class="perm-header">
          <span class="card-title">权限矩阵</span>
          <el-select v-model="selectedRole" size="small" style="width: 140px">
            <el-option v-for="(label, key) in ROLE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </div>
      </template>
      <div class="perm-matrix">
        <table class="perm-table">
          <thead>
            <tr>
              <th>模块</th>
              <th>查看</th>
              <th>新增</th>
              <th>编辑</th>
              <th>删除</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="mod in permModules" :key="mod.key">
              <td class="mod-name">{{ mod.label }}</td>
              <td v-for="action in ['view', 'create', 'edit', 'delete']" :key="action">
                <span class="perm-check" :class="{ allowed: hasPermission_(mod.key, action) }">
                  {{ hasPermission_(mod.key, action) ? '✓' : '—' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </GlowCard>

    <!-- Create/Edit Modal -->
    <el-dialog v-model="modalVisible" :title="isEdit ? '编辑管理员' : '新增管理员'" width="500px" :append-to-body="true">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option v-for="(label, key) in ROLE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import DataTable from '@/components/common/DataTable.vue'
import PulseBadge from '@/components/ui/PulseBadge.vue'
import SearchBox from '@/components/ui/SearchBox.vue'
import GlowCard from '@/components/ui/GlowCard.vue'
import { adminApi } from '@/api/admin'
import { ROLE_LABELS, ROLE_COLORS } from '@/types/admin'
import type { Admin, AdminQuery, AdminForm } from '@/types/admin'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Admin[]>([])
const total = ref(0)
const modalVisible = ref(false)
const isEdit = ref(false)
const selectedRole = ref('admin')
const currentAdmin = ref<Admin | null>(null)
const formRef = ref<FormInstance>()

const query = reactive<AdminQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  role: '',
  status: '',
})

const form = reactive<AdminForm>({
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: 'admin',
  password: '',
})

const formRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
}))

const columns = [
  { prop: 'username', label: '用户', minWidth: 150 },
  { prop: 'role', label: '角色', width: 120 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'lastLoginAt', label: '最后登录', width: 160 },
]

const permModules = [
  { key: 'students', label: '学生管理' },
  { key: 'teachers', label: '教师管理' },
  { key: 'admins', label: '管理员管理' },
  { key: 'logs', label: '系统日志' },
  { key: 'settings', label: '系统设置' },
  { key: 'shell', label: '终端' },
  { key: 'backup', label: '数据备份' },
]

const permMap: Record<string, Record<string, string[]>> = {
  super_admin: {
    view: ['students', 'teachers', 'admins', 'logs', 'settings', 'shell', 'backup'],
    create: ['students', 'teachers', 'admins', 'logs', 'settings', 'shell', 'backup'],
    edit: ['students', 'teachers', 'admins', 'logs', 'settings', 'shell', 'backup'],
    delete: ['students', 'teachers', 'admins', 'logs', 'settings', 'shell', 'backup'],
  },
  admin: {
    view: ['students', 'teachers', 'admins', 'logs', 'settings', 'shell', 'backup'],
    create: ['students', 'teachers', 'logs'],
    edit: ['students', 'teachers', 'logs', 'settings'],
    delete: ['students', 'teachers', 'logs'],
  },
  viewer: {
    view: ['students', 'teachers', 'logs'],
    create: [],
    edit: [],
    delete: [],
  },
}

function hasPermission_(mod: string, action: string): boolean {
  return permMap[selectedRole.value]?.[action]?.includes(mod) ?? false
}

async function fetchData() {
  loading.value = true
  try {
    const res = await adminApi.getList(query)
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

function openCreate() {
  isEdit.value = false
  Object.assign(form, { username: '', realName: '', email: '', phone: '', role: 'user_admin', password: '' })
  modalVisible.value = true
}

function openEdit(row: Admin) {
  isEdit.value = true
  currentAdmin.value = row
  Object.assign(form, { username: row.username, realName: row.realName, email: row.email, phone: row.phone, role: row.role, password: '' })
  modalVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && currentAdmin.value) {
      await adminApi.update(currentAdmin.value.adminId, form)
      ElMessage.success('更新成功')
    } else {
      await adminApi.create(form)
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch {} finally {
    submitting.value = false
  }
}

async function toggleStatus(row: Admin) {
  try {
    await adminApi.toggleStatus(row.adminId)
    ElMessage.success('状态已更新')
    fetchData()
  } catch {}
}

async function handleDelete(row: Admin) {
  try {
    await ElMessageBox.confirm(`确定删除管理员 ${row.username} 吗？`, '删除确认', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    })
    await adminApi.delete(row.adminId)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped>
.admins-page {
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
}

.mono-text {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
}

.permission-card {
  margin-top: 24px;
}

.perm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.perm-matrix {
  overflow-x: auto;
}

.perm-table {
  width: 100%;
  border-collapse: collapse;
}

.perm-table th,
.perm-table td {
  padding: 10px 16px;
  text-align: center;
  border-bottom: 1px solid var(--border-base);
}

.perm-table th {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--bg-elevated);
}

.perm-table th:first-child,
.perm-table td:first-child {
  text-align: left;
}

.mod-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.perm-check {
  font-size: 16px;
  color: var(--text-muted);
}

.perm-check.allowed {
  color: var(--color-success);
  text-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}
</style>
