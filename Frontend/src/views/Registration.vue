<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>号源查询</span>
          <el-button :icon="Refresh" @click="loadSchedules">刷新</el-button>
        </div>
      </template>

      <el-table :data="filteredSchedules" v-loading="loading" stripe border>
        <el-table-column prop="departmentName" label="科室" min-width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.departmentName || '未知科室' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scheduleDate" label="出诊日期" min-width="120" />
        <el-table-column prop="timeSlot" label="时间段" min-width="100">
          <template #default="{ row }">
            <el-tag :type="slotType(row.timeSlot)" effect="light">{{ row.timeSlot }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余号源" min-width="110">
          <template #default="{ row }">
            <span :class="{ 'no-quota': remaining(row) <= 0 }">
              {{ remaining(row) }} / {{ row.totalQuota }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="remaining(row) <= 0"
              :loading="bookingId === row.id"
              @click="handleBook(row)"
            >
              {{ remaining(row) <= 0 ? '已约满' : '预约' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 支付二维码弹窗 -->
    <el-dialog
      v-model="payVisible"
      title="扫码支付"
      width="360px"
      :close-on-click-modal="false"
      align-center
    >
      <div class="pay-box">
        <img v-if="qrDataUrl" :src="qrDataUrl" alt="支付二维码" class="qr-img" />
        <div v-else class="qr-placeholder">二维码加载中…</div>
        <div class="pay-info">
          <div><span>科室：</span>{{ payInfo.departmentName }}</div>
          <div><span>日期：</span>{{ payInfo.scheduleDate }}</div>
          <div><span>时段：</span>{{ payInfo.timeSlot }}</div>
          <div class="pay-order"><span>订单号：</span>{{ payInfo.orderNo }}</div>
        </div>
        <p class="pay-tip">请使用支付宝扫描二维码完成支付</p>
        <el-button type="primary" style="width: 100%" :loading="confirming" @click="handlePaid">
          我已完成支付，生成挂号单
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { queryDepartment, generateOrder, addAppointment } from '../api'

const loading = ref(false)
const schedules = ref([])
const bookingId = ref(null)

const payVisible = ref(false)
const qrDataUrl = ref('')
const confirming = ref(false)
const payInfo = ref({})

const slotType = (slot) => {
  if (slot === '上午') return 'success'
  if (slot === '下午') return 'warning'
  return 'info'
}

const remaining = (row) => (row.totalQuota || 0) - (row.usedQuota || 0)

const filteredSchedules = computed(() => {
  return [...schedules.value].sort((a, b) => {
    if (a.scheduleDate !== b.scheduleDate) return a.scheduleDate.localeCompare(b.scheduleDate)
    const order = { 上午: 0, 下午: 1, 晚上: 2 }
    return (order[a.timeSlot] ?? 0) - (order[b.timeSlot] ?? 0)
  })
})

const loadSchedules = async () => {
  loading.value = true
  try {
    schedules.value = (await queryDepartment()) || []
  } catch {
    schedules.value = []
  } finally {
    loading.value = false
  }
}

const handleBook = async (row) => {
  bookingId.value = row.id
  try {
    const result = await generateOrder(row)
    if (result && result.error) {
      ElMessage.warning(result.error)
      return
    }
    if (!result || !result.qrCode) {
      ElMessage.error('生成订单失败')
      return
    }
    payInfo.value = { ...row, orderNo: result.orderNo }
    qrDataUrl.value = ''
    payVisible.value = true
    try {
      qrDataUrl.value = await QRCode.toDataURL(result.qrCode, { width: 220, margin: 1 })
    } catch {
      qrDataUrl.value = ''
    }
  } catch {
    // 错误提示已在拦截器中处理
  } finally {
    bookingId.value = null
  }
}

const handlePaid = async () => {
  confirming.value = true
  try {
    await addAppointment(payInfo.value.orderNo)
    ElMessage.success('挂号成功')
    payVisible.value = false
    loadSchedules()
  } catch {
    // 错误提示已在拦截器中处理
  } finally {
    confirming.value = false
  }
}

onMounted(loadSchedules)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.no-quota {
  color: #f56c6c;
  font-weight: 600;
}

.pay-box {
  text-align: center;
}

.qr-img {
  width: 220px;
  height: 220px;
  border: 1px solid #eef0f3;
  border-radius: 8px;
}

.qr-placeholder {
  width: 220px;
  height: 220px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a8abb2;
  background: #f7f8fa;
  border-radius: 8px;
}

.pay-info {
  margin: 16px 0;
  text-align: left;
  font-size: 14px;
  line-height: 2;
  padding: 0 12px;
}

.pay-info span {
  color: #909399;
}

.pay-order {
  word-break: break-all;
}

.pay-tip {
  margin: 0 0 16px;
  color: #e6a23c;
  font-size: 13px;
}
</style>
