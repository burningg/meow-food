<template>
  <view class="plan-detail-page">
    <view v-if="detail" class="page-shell detail-shell">
      <header class="top-nav">
        <button class="nav-shell" @tap="goBack('feed')">‹</button>
        <text class="detail-page-title">计划详情</text>
        <button v-if="isCreator" class="nav-shell delete-shell" @tap="deletePlan">✕</button>
        <view v-else class="nav-shell placeholder"></view>
      </header>

      <section class="card detail-hero">
        <view class="detail-hero-head">
          <view>
            <text class="detail-eyebrow">{{ detail.circleName }} · {{ formatDisplayDate(detail.planDate) }}</text>
            <text class="detail-title">{{ detail.title }}</text>
          </view>
          <text class="detail-chip">{{ detail.creatorNickname }}创建</text>
        </view>

        <text class="detail-description">圈内成员都能继续添加菜谱、开始采购，也都能看到采购进度。</text>

        <view class="detail-stats">
          <view class="detail-stat">
            <text class="detail-stat-label">已加菜谱</text>
            <text class="detail-stat-value">{{ detail.recipes.length }} 道</text>
          </view>
          <view class="detail-stat">
            <text class="detail-stat-label">采购状态</text>
            <text class="detail-stat-value">{{ detail.shoppingStarted ? '进行中' : '未开始' }}</text>
          </view>
          <view class="detail-stat">
            <text class="detail-stat-label">重启次数</text>
            <text class="detail-stat-value">{{ detail.shoppingRestartCount }}</text>
          </view>
        </view>
      </section>

      <section class="recipe-section">
        <view class="section-head">
          <view>
            <text class="section-title">计划里的菜谱</text>
            <text class="section-subtitle">点击卡片查看菜谱，成员都能继续增减。</text>
          </view>
          <button class="section-action" @tap="drawerVisible = true">添加菜谱</button>
        </view>

        <view v-if="detail.recipes.length" class="recipe-list">
          <article v-for="recipe in detail.recipes" :key="recipe.id" class="recipe-card">
            <button class="recipe-main" @tap="openDish(recipe.id)">
              <view class="recipe-copy">
                <text class="recipe-title">{{ recipe.name }}</text>
                <text class="recipe-meta">{{ recipe.ownerNickname }} · {{ recipe.categoryName }} · {{ recipe.cookTimeMinutes || '-' }} 分钟</text>
                <text class="recipe-body">
                  {{ recipe.ingredientNames.length ? recipe.ingredientNames.join('、') : recipe.description || '点击查看完整菜谱' }}
                </text>
              </view>
              <text class="recipe-badge">已加入</text>
            </button>
            <button class="recipe-remove" @tap="removeRecipe(recipe.id)">移出计划</button>
          </article>
        </view>

        <view v-else class="empty-card">这份计划里还没有菜谱，先从圈内共享菜谱里加几道吧。</view>
      </section>
    </view>

    <view v-else class="page-shell loading-state">正在加载计划详情...</view>

    <footer v-if="detail" class="bottom-bar detail-bottom-bar">
      <button class="secondary-button action-button" @tap="drawerVisible = true">添加菜谱</button>
      <button class="primary-button action-button" @tap="startOrOpenShopping">
        {{ detail.shoppingStarted ? '查看采购' : '开始采购' }}
      </button>
    </footer>

    <view v-if="drawerVisible" class="drawer-overlay" @tap="closeDrawer">
      <section class="drawer-sheet" @tap.stop>
        <view class="drawer-handle"></view>
        <view class="drawer-head">
          <view>
            <text class="section-title">添加圈内菜谱</text>
            <text class="section-subtitle">支持搜索和多选，确认后会加入当前计划。</text>
          </view>
          <button class="drawer-close" @tap="closeDrawer">×</button>
        </view>

        <view class="recipe-search">
          <text class="recipe-search-icon">⌕</text>
          <input v-model.trim="searchKeyword" class="recipe-search-input" maxlength="24" placeholder="搜索菜谱名或食材" />
        </view>

        <view v-if="availableMenus.length" class="drawer-list">
          <button
            v-for="menu in availableMenus"
            :key="menu.id"
            :class="['drawer-item', { active: selectedDishIds.includes(menu.id) }]"
            @tap="toggleSelectedDish(menu.id)"
          >
            <view>
              <text class="drawer-item-title">{{ menu.name }}</text>
              <text class="drawer-item-meta">{{ menu.ownerNickname }} · {{ menu.categoryName }}</text>
              <text class="drawer-item-body">
                {{ menu.ingredientNames.length ? menu.ingredientNames.join('、') : menu.description || '圈内共享菜谱' }}
              </text>
            </view>
            <text class="drawer-item-check">{{ selectedDishIds.includes(menu.id) ? '已选' : '选择' }}</text>
          </button>
        </view>
        <view v-else class="empty-card">当前圈子里没有更多可添加的菜谱了。</view>

        <button class="primary-button drawer-submit" @tap="submitSelectedRecipes">
          确认添加 {{ selectedDishIds.length ? `(${selectedDishIds.length})` : '' }}
        </button>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { requireAuth } from '@/lib/auth'
import { confirmDialog, Message } from '@/lib/feedback'
import { getRouteParams, goBack, push, replace } from '@/lib/navigation'
import { PlanService, type PlanDetail } from '@/services/plan-service'
import { SocialService, type BuddyCircleDetail } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const params = getRouteParams() as { id?: string }
const planService = new PlanService()
const socialService = new SocialService()
const authStore = useAuthStore()

const detail = ref<PlanDetail | null>(null)
const circleDetail = ref<BuddyCircleDetail | null>(null)
const drawerVisible = ref(false)
const searchKeyword = ref('')
const selectedDishIds = ref<string[]>([])

const isCreator = computed(() => Boolean(detail.value && authStore.user?.id === detail.value.creatorUserId))
const availableMenus = computed(() => {
  const existingIds = new Set((detail.value?.recipes || []).map((recipe) => recipe.id))
  const keyword = searchKeyword.value.trim().toLowerCase()
  return (circleDetail.value?.sharedMenus || []).filter((menu) => {
    if (existingIds.has(menu.id)) return false
    if (!keyword) return true
    return (
      menu.name.toLowerCase().includes(keyword) ||
      menu.ingredientNames.some((ingredient) => ingredient.toLowerCase().includes(keyword))
    )
  })
})

onMounted(async () => {
  if (!(await requireAuth('plan-detail'))) return
  if (!params.id) {
    replace('feed')
    return
  }
  await loadData()
})

async function loadData() {
  try {
    const { data } = await planService.getPlanDetail(String(params.id))
    detail.value = data
    await loadCircleDetail(data.circleId)
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '计划详情加载失败')
    replace('feed')
  }
}

async function loadCircleDetail(circleId: string) {
  try {
    const { data } = await socialService.getCircleDetail(circleId)
    circleDetail.value = data
  } catch (error: any) {
    circleDetail.value = null
    Message.info(error?.response?.data?.message || '圈内菜谱加载失败')
  }
}

function closeDrawer() {
  drawerVisible.value = false
  searchKeyword.value = ''
  selectedDishIds.value = []
}

function toggleSelectedDish(dishId: string) {
  if (selectedDishIds.value.includes(dishId)) {
    selectedDishIds.value = selectedDishIds.value.filter((id) => id !== dishId)
    return
  }
  selectedDishIds.value = [...selectedDishIds.value, dishId]
}

async function submitSelectedRecipes() {
  if (!detail.value) return
  if (!selectedDishIds.value.length) {
    Message.info('先选中要加入的菜谱')
    return
  }
  try {
    const { data } = await planService.addRecipes(detail.value.id, selectedDishIds.value)
    detail.value = data
    await loadCircleDetail(data.circleId)
    closeDrawer()
    Message.success('菜谱已加入计划')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '添加菜谱失败')
  }
}

async function removeRecipe(dishId: string) {
  if (!detail.value) return
  try {
    await confirmDialog({
      title: '移出计划',
      message: '移除后，这道菜对应的采购食材也会同步更新，确认继续吗？',
    })
    const { data } = await planService.removeRecipe(detail.value.id, dishId)
    detail.value = data
    await loadCircleDetail(data.circleId)
    Message.success('菜谱已移出')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '移除菜谱失败')
  }
}

async function startOrOpenShopping() {
  if (!detail.value) return
  try {
    if (!detail.value.shoppingStarted) {
      await planService.startShoppingList(detail.value.id)
    }
    push({ name: 'plan-shopping', params: { id: detail.value.id } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单启动失败')
  }
}

async function deletePlan() {
  if (!detail.value) return
  try {
    await confirmDialog({
      title: '删除计划',
      message: '删除后计划和采购清单都会一起移除，确认继续吗？',
    })
    await planService.deletePlan(detail.value.id)
    Message.success('计划已删除')
    replace('feed')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '删除计划失败')
  }
}

function openDish(dishId: string) {
  push({ name: 'dish-detail', params: { id: dishId } })
}

function formatDisplayDate(date: string) {
  const [, month, day] = date.split('-')
  return `${Number(month)} 月 ${Number(day)} 日`
}
</script>

<style>
.plan-detail-page {
  min-height: 100vh;
  padding-bottom: 108px;
  background: #f7f6f3;
}

.detail-shell {
  padding-bottom: 24px;
}

.detail-page-title {
  color: #151515;
  font-size: 16px;
  font-weight: 700;
}

.delete-shell {
  color: var(--accent-dark);
}

.detail-hero,
.recipe-card,
.drawer-sheet {
  border-radius: 18px;
  background: #fff;
  box-shadow: var(--shadow);
}

.detail-hero {
  padding: 18px;
}

.detail-hero-head,
.detail-stats,
.detail-stat,
.recipe-main,
.recipe-copy,
.recipe-section,
.recipe-list,
.drawer-head,
.drawer-list,
.drawer-item,
.detail-bottom-bar,
.recipe-search,
.section-head {
  display: flex;
}

.detail-hero-head,
.section-head,
.drawer-head {
  align-items: center;
  justify-content: space-between;
}

.detail-hero-head {
  align-items: flex-start;
}

.detail-eyebrow,
.detail-description,
.detail-stat-label,
.recipe-meta,
.recipe-body,
.section-subtitle,
.drawer-item-meta,
.drawer-item-body {
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.detail-title,
.recipe-title {
  display: block;
  margin-top: 4px;
  color: #151515;
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 600;
}

.detail-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #edf3ec;
  color: #346538;
  font-size: 11px;
  font-weight: 700;
}

.detail-description {
  display: block;
  margin-top: 12px;
}

.detail-stats {
  gap: 10px;
  margin-top: 16px;
}

.detail-stat {
  flex: 1;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 14px;
  background: #fbf8f4;
}

.detail-stat-value {
  color: #151515;
  font-size: 16px;
  font-weight: 800;
}

.recipe-section,
.recipe-list,
.recipe-copy,
.drawer-list {
  flex-direction: column;
}

.recipe-section {
  gap: 12px;
  margin-top: 18px;
}

.section-title {
  display: block;
  color: #151515;
  font-size: 16px;
  font-weight: 800;
}

.section-action {
  min-width: 88px;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f3eee7;
  color: #5a4333;
  font-size: 12px;
  font-weight: 700;
}

.recipe-list {
  gap: 12px;
}

.recipe-card {
  padding: 16px;
}

.recipe-main {
  justify-content: space-between;
  text-align: left;
}

.recipe-copy {
  flex: 1;
  gap: 4px;
}

.recipe-title {
  margin-top: 0;
  font-size: 20px;
}

.recipe-badge {
  margin-left: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fbf0e8;
  color: #9f5c38;
  font-size: 11px;
  font-weight: 700;
}

.recipe-remove {
  margin-top: 12px;
  color: var(--accent-dark);
  font-size: 12px;
  font-weight: 700;
  text-align: right;
}

.detail-bottom-bar {
  gap: 12px;
}

.action-button {
  flex: 1;
}

.drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: flex-end;
  background: rgba(21, 21, 21, 0.36);
}

.drawer-sheet {
  width: 100%;
  max-height: 78vh;
  padding: 14px 16px calc(20px + env(safe-area-inset-bottom));
  border-radius: 24px 24px 0 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-handle {
  width: 44px;
  height: 5px;
  margin: 0 auto;
  border-radius: 999px;
  background: #d9cec3;
}

.drawer-close {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #f4eee7;
  color: #151515;
  font-size: 20px;
}

.recipe-search {
  align-items: center;
  gap: 8px;
  min-height: 46px;
  border-radius: 14px;
  background: #f3efe9;
  padding: 0 14px;
}

.recipe-search-icon {
  color: #8b837c;
  font-size: 16px;
}

.recipe-search-input {
  flex: 1;
  min-height: 46px;
  color: #151515;
  font-size: 14px;
}

.drawer-list {
  gap: 10px;
  overflow-y: auto;
}

.drawer-item {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 14px;
  background: #fbf8f4;
  padding: 14px;
  text-align: left;
}

.drawer-item.active {
  background: #edf3ec;
}

.drawer-item-title {
  display: block;
  color: #151515;
  font-size: 14px;
  font-weight: 700;
}

.drawer-item-check {
  flex-shrink: 0;
  color: var(--accent-dark);
  font-size: 12px;
  font-weight: 700;
}

.drawer-submit {
  min-height: 48px;
}
</style>
