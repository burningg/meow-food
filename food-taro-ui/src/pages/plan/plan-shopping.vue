<template>
  <view class="plan-shopping-page">
    <view v-if="shopping" class="page-shell shopping-shell">


      <view class="shopping-top-nav-card">
        <view class="shopping-nav-spacer"></view>
        <text class="shopping-page-title">采购清单</text>
        <button class="shopping-reset-shell" @tap="resetShopping">重新开始</button>
      </view>

      <section class="shopping-hero">
        <view class="shopping-hero-top">
          <view class="shopping-hero-copy">
            <text class="shopping-hero-eyebrow">{{ shopping.planTitle }} · 共享采购</text>
            <text class="shopping-hero-title">
              {{ shopping.shoppingStarted ? '大家一起采买这顿饭' : '还没开始采购' }}
            </text>
          </view>
          <view class="shopping-status-chip">
            <text class="shopping-status-chip-text">{{ shopping.shoppingStarted ? '进行中' : '未开始' }}</text>
          </view>
        </view>

        <view class="shopping-summary-row">
          <view class="shopping-summary-card">
            <text class="shopping-summary-label">食材总数</text>
            <text class="shopping-summary-value">{{ shopping.totalItemCount }} 项</text>
          </view>
          <view class="shopping-summary-card">
            <text class="shopping-summary-label">已买</text>
            <text class="shopping-summary-value">{{ shopping.purchasedItemCount }} 项</text>
          </view>
          <view class="shopping-summary-card">
            <text class="shopping-summary-label">待买</text>
            <text class="shopping-summary-value">{{ pendingItemCount }} 项</text>
          </view>
        </view>
      </section>

      <section class="shopping-items-section">
        <view class="shopping-section-head">
          <text class="shopping-section-title">按食材合并的采购项</text>
          <text class="shopping-section-count">{{ shopping.items.length }} 项</text>
        </view>

        <view v-if="shopping.shoppingStarted && shopping.items.length" class="shopping-list">
          <view v-for="item in shopping.items" :key="item.id" class="shopping-card">
            <button class="shopping-card-head" @tap="toggleItem(item.id)">
              <view class="shopping-card-left">
                <view :class="['shopping-checkbox', { active: item.purchased }]">
                  <text v-if="item.purchased" class="shopping-checkbox-check">✓</text>
                </view>
                <view class="shopping-card-copy">
                  <text class="shopping-item-title">{{ item.ingredientName }}</text>
                  <text :class="['shopping-item-status', { active: item.purchased }]">
                    {{ item.purchased ? `${item.purchasedByNickname || '圈内成员'}已采买` : '待采买' }}
                  </text>
                </view>
              </view>

              <view :class="['shopping-item-state', { active: item.purchased }]">
                <text class="shopping-item-state-text">{{ item.purchased ? '已完成' : '待购买' }}</text>
              </view>
            </button>

            <view class="shopping-sources-card">
              <text class="shopping-sources-title">来源菜谱与用量</text>
              <button
                v-for="source in item.sources"
                :key="`${item.id}-${source.dishId}-${source.amount}`"
                class="shopping-source-row"
                @tap="openDish(source.dishId)"
              >
                <text class="shopping-source-dish">{{ source.dishName }}</text>
                <text class="shopping-source-amount">{{ source.amount }}</text>
              </button>
            </view>
          </view>
        </view>

        <view v-else class="empty-card shopping-empty-card">
          <text class="shopping-empty-title">当前还没有采购清单</text>
          <text class="shopping-empty-desc">点击下方按钮后，会按计划里的菜谱自动合并成共享采购项。</text>
        </view>
      </section>

    </view>

    <view v-else class="page-shell shopping-loading">正在加载采购清单...</view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, push, replace } from '@/lib/navigation'
import { PlanService, type PlanShoppingList } from '@/services/plan-service'

const params = getRouteParams<{ id?: string }>()
const planService = new PlanService()
const shopping = ref<PlanShoppingList | null>(null)

const pendingItemCount = computed(() => {
  if (!shopping.value) return 0
  return Math.max(shopping.value.totalItemCount - shopping.value.purchasedItemCount, 0)
})

onMounted(async () => {
  if (!(await requireAuth('plan-shopping'))) return
  if (!params.id) {
    replace('plan')
    return
  }
  await loadShopping()
})

async function loadShopping() {
  try {
    const { data } = await planService.getShoppingList(String(params.id))
    shopping.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单加载失败')
    replace('plan')
  }
}

async function startOrReloadShopping() {
  if (!params.id) return
  try {
    const { data } = await planService.startShoppingList(String(params.id))
    shopping.value = data
    Message.success(data.shoppingStarted ? '采购清单已准备好' : '采购清单已刷新')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单启动失败')
  }
}

async function toggleItem(itemId: string) {
  if (!params.id || !shopping.value?.shoppingStarted) return
  try {
    const { data } = await planService.toggleShoppingItem(String(params.id), itemId)
    shopping.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购项更新失败')
  }
}

async function resetShopping() {
  if (!params.id || !shopping.value?.shoppingStarted) {
    Message.info('这份计划还没开始采购')
    return
  }
  try {
    await confirmDialog({
      title: '重新开始采购',
      message: '会按当前计划里的菜谱重新生成采购单，并清空现有勾选状态，确认继续吗？',
    })
    const { data } = await planService.resetShoppingList(String(params.id))
    shopping.value = data
    Message.success('采购清单已重置')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '采购清单重置失败')
  }
}

function openDish(dishId: string) {
  push({ name: 'dish-detail', params: { id: dishId } })
}
</script>

<style>
.plan-shopping-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.shopping-shell {
  padding: 0 20px 24px;
  background: #f7f6f3;
}

.shopping-status-bar,
.shopping-status-right,
.shopping-top-nav-card,
.shopping-hero-top,
.shopping-summary-row,
.shopping-section-head,
.shopping-card-head,
.shopping-card-left,
.shopping-sources-card,
.shopping-source-row {
  display: flex;
}

.shopping-status-bar,
.shopping-top-nav-card,
.shopping-hero-top,
.shopping-card-head,
.shopping-source-row {
  align-items: center;
  justify-content: space-between;
}

.shopping-status-bar {
  height: 54px;
  padding: 14px 4px 0;
}

.shopping-status-time {
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.shopping-status-right {
  gap: 6px;
}

.shopping-status-dot {
  height: 10px;
  border-radius: 999px;
  background: #151515;
}

.shopping-status-dot.short {
  width: 16px;
}

.shopping-status-dot.long {
  width: 24px;
}

.shopping-top-nav-card {
  margin-top: 10px;
  padding: 16px;
  border-radius: 12px;
  background: #ffffff;
}

.shopping-nav-spacer {
  width: 73px;
  height: 33px;
}

.shopping-page-title {
  color: #151515;
  font-size: 16px;
  font-weight: 600;
}

.shopping-reset-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 33px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f7ecea;
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.shopping-hero {
  margin-top: 16px;
  padding: 18px;
  border-radius: 16px;
  background: #1b3a2d;
}

.shopping-hero-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.shopping-hero-eyebrow {
  color: #ddeadf;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.shopping-hero-title {
  color: #ffffff;
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 600;
}

.shopping-status-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #335b47;
}

.shopping-status-chip-text {
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
}

.shopping-summary-row {
  gap: 10px;
  margin-top: 12px;
}

.shopping-summary-card {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  background: #335b47;
}

.shopping-summary-label {
  display: block;
  color: #ddeadf;
  font-size: 11px;
  font-weight: 600;
}

.shopping-summary-value {
  display: block;
  margin-top: 4px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
}

.shopping-items-section {
  margin-top: 16px;
}

.shopping-section-head {
  align-items: flex-end;
}

.shopping-section-title {
  color: #151515;
  font-size: 15px;
  font-weight: 700;
}

.shopping-section-count {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
}

.shopping-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.shopping-card {
  padding: 16px;
  border-radius: 14px;
  background: #ffffff;
}

.shopping-card-left {
  gap: 12px;
}

.shopping-checkbox {
  display: flex;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: #f3eee7;
}

.shopping-checkbox.active {
  background: #edf3ec;
}

.shopping-checkbox-check {
  color: #346538;
  font-size: 13px;
  font-weight: 700;
}

.shopping-card-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  text-align: left;
}

.shopping-item-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
}

.shopping-item-status {
  color: #9f5c38;
  font-size: 12px;
}

.shopping-item-status.active {
  color: #346538;
}

.shopping-item-state {
  margin-left: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7ecea;
}

.shopping-item-state.active {
  background: #edf3ec;
}

.shopping-item-state-text {
  color: #9f5c38;
  font-size: 11px;
  font-weight: 600;
}

.shopping-item-state.active .shopping-item-state-text {
  color: #346538;
}

.shopping-sources-card {
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #fbf8f4;
}

.shopping-sources-title {
  color: #9a887a;
  font-size: 11px;
  font-weight: 600;
}

.shopping-source-row {
  color: #151515;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
}

.shopping-source-amount {
  color: #787774;
  font-size: 12px;
  font-weight: 500;
}

.shopping-empty-card {
  margin-top: 12px;
}

.shopping-empty-title {
  display: block;
  color: #151515;
  font-size: 15px;
  font-weight: 600;
}

.shopping-empty-desc {
  display: block;
  margin-top: 6px;
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.shopping-footer {
  margin-top: 16px;
}

.shopping-footer-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 52px;
  border-radius: 16px;
  background: #9f5c38;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
}

.shopping-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #787774;
}
</style>
