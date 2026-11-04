<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的挂号记录</span>
          <el-button :icon="Refresh" @click="loadAppointments">刷新</el-button>
        </div>
      </template>

      <el-table :data="appointments" v-loading="loading" stripe border>
        <el-table-column prop="patientName" label="患者姓名" min-width="110" />
        <el-table-column prop="patientPhone" label="手机号" min-width="130" />
        <el-table-column label="就诊时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.appointmentTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">{{ row.status || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              plain
              :loading="cancellingId === row.id"
              @click="handleCancel(row)"
            >
              取消预约
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !appointments.length" description="暂无挂号记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { queryAppointment, cancelAppointment } from '../api'

const loading = ref(false)
const appointments = ref([])
const cancellingId = ref(null)

const statusType = (status) => {
  if (status === '已预约') return 'success'
  if (status === '已取消') return 'info'
  if (status === '已完成') return 'warning'
  return 'info'
}

const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const loadAppointments = async () => {
  loading.value = true
  try {
    appointments.value = (await queryAppointment()) || []
  } catch {
    appointments.value = []
  } finally {
    loading.value = false
  }
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('取消预约将进行退款，确定继续吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }

  cancellingId.value = row.id
  try {
    // 订单号已由后端持久化到挂号记录，退款时后端从数据库读取，前端仅透传
    const msg = await cancelAppointment({
      appointmentId: row.id,
      orderNo: row.orderNo || '',
      status: 'success'
    })
    ElMessage.success(msg || '操作成功')
    loadAppointments()
  } catch {
    // 错误提示已在拦截器中处理
  } finally {
    cancellingId.value = null
  }
}

onMounted(loadAppointments)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
