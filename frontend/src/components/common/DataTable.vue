<template>
  <div class="data-table-wrapper">
    <el-table
      :data="data ?? []"
      :stripe="false"
      :border="false"
      v-loading="loading"
      @row-click="$emit('row-click', $event)"
      @selection-change="$emit('selection-change', $event)"
      style="width: 100%"
      :row-class-name="rowClassName"
      highlight-current-row
    >
      <el-table-column v-if="selectable" type="selection" width="44" />
      <el-table-column v-if="showIndex" type="index" label="#" width="50" />

      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :sortable="col.sortable"
        :fixed="col.fixed"
      >
        <template #default="scope">
          <slot :name="col.prop" :row="scope.row" :value="scope.row[col.prop]">
            {{ scope.row[col.prop] }}
          </slot>
        </template>
      </el-table-column>

      <el-table-column v-if="$slots.actions" label="操作" :width="actionWidth" fixed="right">
        <template #default="scope">
          <slot name="actions" :row="scope.row"></slot>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="currentPageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="$emit('page-change', currentPage)"
        @size-change="$emit('size-change', currentPageSize)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface TableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  sortable?: boolean | string
  fixed?: boolean | string
}

const props = withDefaults(defineProps<{
  data: any[]
  columns: TableColumn[]
  total?: number
  page?: number
  pageSize?: number
  loading?: boolean
  selectable?: boolean
  showIndex?: boolean
  actionWidth?: number | string
}>(), {
  total: 0,
  page: 1,
  pageSize: 10,
  loading: false,
  selectable: false,
  showIndex: false,
  actionWidth: 180,
})

const emit = defineEmits(['row-click', 'selection-change', 'page-change', 'size-change', 'update:page', 'update:pageSize'])

const currentPage = computed({
  get: () => props.page,
  set: (val) => emit('update:page', val),
})

const currentPageSize = computed({
  get: () => props.pageSize,
  set: (val) => emit('update:pageSize', val),
})

function rowClassName() {
  return 'cyber-row'
}
</script>

<style scoped>
.data-table-wrapper {
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-base);
  overflow: hidden;
}

.table-pagination {
  padding: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-base);
}
</style>
