<template>
  <view class="plan-shopping-page">
    <view v-if="shopping" class="page-shell shopping-shell">
      <header class="top-nav">
        <button class="nav-shell" @tap="backToPlan">‹</button>
        <text class="shopping-page-title">采购清单</text>
        <button class="nav-shell reset-shell" @tap="resetShopping">↺</button>
      </header>

      <section class="shopping-hero">
        <view class="shopping-hero-head">
          <view>
            <text class="shopping-eyebrow">{{ shopping.planTitle }} · {{ formatDisplayDate(shopping.planDate) }}</text>
            <text class="shopping-title">{{ shopping.shoppingStarted ? '大家一起采买这顿饭' : '还没开始采购' }}</text>
          </view>
          <text class="shopping-chip">{{ shopping.shoppingStarted ? '进行中' : '未开始' }}</text>
        </view>

        <text class="shopping-description">
          {{ shopping.shoppingStarted
            ? `由 ${shopping.startedByNickname || '圈内成员'} 发起，勾选后会显示采买人昵称。`
            : '开始采购后会按当前计划里的全部菜谱自动生成共享清单。' }}
        </text>

        <view class="shopping-stats">
          <view class="shopping-stat">
            <text class="shopping-stat-label">食材总数</text>
            <text class="shopping-stat-value">{{ shopping.totalItemCount }} 项</text>
          </view>
          <view class="shopping-stat">
            <text class="shopping-stat-label">已买</text>
            <text class="shopping-stat-value">{{ shopping.purchasedItemCount }} 项</text>
          </view>
          <view class="shopping-stat">
            <text class="shopping-stat-label">重启次数</text>
            <text class="shopping-stat-value">{{ shopping.restartCount }}</text>
          </view>
        </view>
      </section>

      <section v-if="shopping.shoppingStarted" class="shopping-section">
        <view class="section-head">
          <view>
            <text class="section-title">按食材合并的采购项</text>
            <text class="section-subtitle">同名食材合一条，并保留来自哪道菜、各自用量。</text>
          </view>
          <text class="section-link">{{ shopping.items.length }} 项</text>
        </view>

        <view class="shopping-list">
          <article v-for="item in shopping.items" :key="item.id" class="shopping-card">
            <button class="shopping-main" @tap="toggleItem(item.id)">
              <view class="shopping-main-left">
                <view :class="['shopping-checkbox', { active: item.purchased }]">
                  <text v-if="item.purchased" class="shopping-checkmark">✓</text>
                </view>
                <view class="shopping-copy">
                  <text class="shopping-item-title">{{ item.ingredientName }}</text>
                  <text :class="['shopping-item-status', { active: item.purchased }]">
                    {{ item.purchased ? `${item.purchasedByNickname || '圈内成员'}已采买` : '待采买' }}
                  </text>
                </view>
              </view>
              <text :class="['shopping-item-badge', { active: item.purchased }]">
                {{ item.purchased ? '已完成' : '待购买' }}
              </text>
            </button>

            <view class="shopping-sources">
              <text class="shopping-sources-title">来源菜谱与用量</text>
              <button
                v-for="source in item.sources"
                :key="`${item.id}-${source.dishId}-${source.amount}`"
                class="shopping-source-row"
                @tap="openDish(source.dishId)"
              >
                <text class="shopping-source-name">{{ source.dishName }}</text>
                <text class="shopping-source-amount">{{ source.amount }}</text>
              </button>
            </view>
          </article>
        </view>
      </section>

      <view v-else class="empty-card">当前还没有采购清单，点击下方按钮就会按计划里的菜谱自动生成。</view>
    </view>

    <view v-else class="page-shell loading-state">正在加载采购清单...</view>

    <footer v-if="shopping" class="bottom-bar shopping-bottom-bar">
      <button class="secondary-button shopping-action" @tap="resetShopping">重新开始</button>
      <button class="primary-button shopping-action" @tap="startOrReloadShopping">
        {{ shopping.shoppingStarted ? '刷新清单' : '开始采购' }}
      </button>
    </footer>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, goBack, push, replace } from '@/lib/navigation'
import { PlanService, type PlanShoppingList } from '@/services/plan-service'

const params = getRouteParams() as { id?: string }
const planService = new PlanService()
const shopping = ref<PlanShoppingList | null>(null)

onMounted(async () => {
  if (!(await requireAuth('plan-shopping'))) return
  if (!params.id) {
    replace('feed')
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
    replace('feed')
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
  if (!params.id) return
  try {
    const { data } = await planService.toggleShoppingItem(String(params.id), itemId)
    shopping.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购项更新失败')
  }
}

async function resetShopping() {
  if (!params.id) return
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

function backToPlan() {
  goBack({ name: 'plan-detail', params: { id: String(params.id || '') } })
}

function formatDisplayDate(date: string) {
  const [, month, day] = date.split('-')
  return `${Number(month)} 月 ${Number(day)} 日`
}
</script>

<style>
.plan-shopping-page {
  min-height: 100vh;
  padding-bottom: 108px;
  background: #f7f6f3;
}

.shopping-shell {
  padding-bottom: 24px;
}

.shopping-page-title {
  color: #151515;
  font-size: 16px;
  font-weight: 700;
}

.reset-shell {
  color: var(--accent-dark);
}

.shopping-hero {
  border-radius: 20px;
  background: #1b3a2d;
  padding: 18px;
  box-shadow: 0 18px 30px rgba(27, 58, 45, 0.16);
}

.shopping-hero-head,
.shopping-stats,
.shopping-stat,
.shopping-section,
.shopping-list,
.shopping-card,
.shopping-main,
.shopping-main-left,
.shopping-copy,
.shopping-bottom-bar,
.shopping-sources,
.shopping-source-row,
.section-head {
  display: flex;
}

.shopping-hero-head,
.section-head,
.shopping-main,
.shopping-source-row {
  align-items: center;
  justify-content: space-between;
}

.shopping-hero-head {
  align-items: flex-start;
}

.shopping-eyebrow,
.shopping-description,
.shopping-stat-label,
.section-subtitle,
.shopping-item-status,
.shopping-sources-title,
.shopping-source-amount {
  color: #dbe7dd;
  font-size: 12px;
  line-height: 1.55;
}

.shopping-title {
  display: block;
  margin-top: 4px;
  color: #fff;
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 600;
}

.shopping-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.shopping-description {
  display: block;
  margin-top: 12px;
}

.shopping-stats {
  gap: 10px;
  margin-top: 16px;
}

.shopping-stat {
  flex: 1;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 14px;
  background: #335b47;
}

.shopping-stat-value {
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.shopping-section,
.shopping-list,
.shopping-card,
.shopping-copy,
.shopping-sources {
  flex-direction: column;
}

.shopping-section {
  gap: 12px;
  margin-top: 18px;
}

.section-title {
  display: block;
  color: #151515;
  font-size: 16px;
  font-weight: 800;
}

.section-subtitle {
  color: #787774;
}

.section-link {
  color: var(--accent-dark);
  font-size: 12px;
  font-weight: 700;
}

.shopping-list {
  gap: 12px;
}

.shopping-card {
  gap: 12px;
  border-radius: 18px;
  background: #fff;
  padding: 16px;
  box-shadow: var(--shadow);
}

.shopping-main-left {
  align-items: center;
  gap: 12px;
}

.shopping-checkbox {
  display: flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f3eee7;
}

.shopping-checkbox.active {
  background: #edf3ec;
}

.shopping-checkmark {
  color: #346538;
  font-size: 14px;
  font-weight: 800;
}

.shopping-item-title {
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 600;
}

.shopping-item-status {
  color: var(--accent-dark);
}

.shopping-item-status.active {
  color: #346538;
}

.shopping-item-badge {
  margin-left: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f7ecea;
  color: var(--accent-dark);
  font-size: 11px;
  font-weight: 700;
}

.shopping-item-badge.active {
  background: #edf3ec;
  color: #346538;
}

.shopping-sources {
  gap: 8px;
  border-radius: 14px;
  background: #fbf8f4;
  padding: 12px;
}

.shopping-sources-title {
  color: #9a887a;
  font-size: 11px;
  font-weight: 700;
}

.shopping-source-row {
  color: #151515;
  font-size: 13px;
  font-weight: 600;
  text-align: left;
}

.shopping-source-amount {
  color: #787774;
  font-weight: 500;
}

.shopping-bottom-bar {
  gap: 12px;
}

.shopping-action {
  flex: 1;
}
</style>
