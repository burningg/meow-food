<template>
  <view class="plan-detail-page">
    <view v-if="detail" class="page-shell detail-shell">
      <section class="detail-hero">
        <view class="detail-hero-copy">
          <text class="detail-hero-title">{{ detail.title }}</text>
          <text class="detail-hero-meta">
            {{ detail.planDate }} · {{ detail.circleName }} · {{ detail.creatorNickname }}创建
          </text>
        </view>
        <button
          class="detail-share-button"
          open-type="share"
          aria-label="分享计划"
        >
          ↗
        </button>
      </section>

      <section class="detail-summary">
        <view class="detail-stat-card">
          <text class="detail-stat-label">已选菜谱</text>
          <text class="detail-stat-value">{{ detail.recipes.length }} 道</text>
        </view>
        <view class="detail-stat-card">
          <text class="detail-stat-label">采购状态</text>
          <text class="detail-stat-value">{{ detail.shoppingStarted ? '已开始' : '未开始' }}</text>
        </view>
      </section>

      <section class="detail-visibility-card">
        <view class="detail-visibility-head">
          <view class="detail-visibility-copy">
            <text class="detail-section-title">可见范围</text>
            <text class="detail-section-subtitle">{{ visibilitySummary }}</text>
          </view>
          <button
            v-if="canEditVisibility"
            class="detail-visibility-edit"
            @tap="openVisibilitySheet"
          >
            修改
          </button>
        </view>

        <view v-if="visibleUsers.length" class="detail-visible-user-list">
          <view
            v-for="user in visibleUsers"
            :key="user.id"
            class="detail-visible-user-chip"
          >
            <SmartImage :src="user.avatar" class-name="detail-visible-user-avatar" />
            <text class="detail-visible-user-name">{{ getVisibleUserName(user) }}</text>
          </view>
        </view>
        <text v-else class="detail-visibility-desc">
          {{ detail.circleName }} 全部成员可见
        </text>
      </section>

      <view class="detail-section">
        <view class="detail-section-head">
          <view>
            <text class="detail-section-title">选择菜谱</text>
          </view>
        </view>

        <view class="detail-search">
          <text class="detail-search-icon">⌕</text>
          <input
            v-model.trim="searchKeyword"
            class="detail-search-input"
            maxlength="24"
            placeholder="搜索菜谱名或食材"
            placeholder-class="detail-search-placeholder"
          />
        </view>

        <view v-if="categoryItems.length" class="detail-category-section">
          <scroll-view class="detail-category-scroll" :scroll-x="true">
            <view class="detail-category-row">
              <button
                :class="['detail-category-pill', { active: !selectedCategoryId }]"
                @tap="selectCategory('')"
              >
                全部
              </button>
              <button
                v-for="category in categoryItems"
                :key="category.id"
                :class="['detail-category-pill', { active: selectedCategoryId === category.id }]"
                @tap="selectCategory(category.id)"
              >
                {{ category.name }}
              </button>
            </view>
          </scroll-view>
        </view>

        <view v-if="loadingCandidates" class="detail-candidate-empty">
          正在加载待添加菜谱...
        </view>
        <view v-else-if="filteredMenus.length" class="detail-candidate-list">
          <button
            v-for="menu in filteredMenus"
            :key="menu.id"
            :class="['detail-candidate-card', { preview: !isLoggedIn }]"
            @tap="handleCandidateTap(menu)"
          >
            <SmartImage :src="menu.image" class-name="detail-candidate-image" />
            <view class="detail-candidate-body">
              <view class="detail-candidate-top">
                <text class="detail-candidate-title">{{ menu.name }}</text>
                <text class="detail-candidate-meta">
                  {{ menu.categoryName || '共享菜谱' }}{{ isExistingDish(menu.id) && menu.addedByNickname ? ` · ${menu.addedByNickname}加入` : '' }}
                </text>
              </view>

              <view class="detail-candidate-bottom">
                <view class="detail-owner-chip">
                  <text class="detail-owner-chip-text">{{ menu.ownerNickname }}</text>
                </view>
                <view :class="['detail-candidate-radio', { active: isDishChecked(menu.id) }]">
                  <text v-if="isDishChecked(menu.id)" class="detail-candidate-radio-check">✓</text>
                </view>
              </view>
            </view>
          </button>
        </view>
        <view v-else class="detail-candidate-empty">
          暂时没有可选择的菜谱
        </view>
      </view>

      <view v-if="canEditRecipes" class="detail-save-bar">
        <button
          class="detail-save-button"
          :disabled="submitting || !hasSelectedDishChanges"
          @tap="submitSelectedRecipes"
        >
          {{ submitting ? '保存中...' : '保存' }}
        </button>
      </view>
    </view>

    <view v-else class="page-shell detail-loading">正在加载计划详情...</view>

    <view
      v-if="visibilitySheetVisible && detail"
      class="visibility-overlay"
      @tap="closeVisibilitySheet"
    >
      <section class="visibility-sheet" @tap.stop>
        <view class="visibility-sheet-handle"></view>
        <view class="visibility-sheet-head">
          <view>
            <text class="visibility-sheet-title">可见成员</text>
            <text class="visibility-sheet-subtitle">{{ visibilityDraftSummary }}</text>
          </view>
        </view>

        <view v-if="loadingVisibilityMembers" class="visibility-member-empty">
          成员加载中...
        </view>
        <view v-else-if="circleMembers.length" class="visibility-member-list">
          <button
            v-for="member in circleMembers"
            :key="member.id"
            :class="[
              'visibility-member-chip',
              { active: isDraftVisibleUserSelected(member.id) },
            ]"
            @tap="toggleDraftVisibleUser(member.id)"
          >
            <SmartImage :src="member.avatar" class-name="visibility-member-avatar" />
            <text class="visibility-member-name">{{ getCircleMemberName(member) }}</text>
          </button>
        </view>
        <view v-else class="visibility-member-empty">
          暂无成员
        </view>

        <view class="visibility-sheet-actions">
          <button class="visibility-cancel-button" :disabled="savingVisibility" @tap="closeVisibilitySheet">
            取消
          </button>
          <button class="visibility-save-button" :disabled="savingVisibility || !hasVisibilityChanges" @tap="submitVisibleUsers">
            {{ savingVisibility ? '保存中...' : '保存' }}
          </button>
        </view>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useShareAppMessage } from '@tarojs/taro'
import { computed, onMounted, ref } from 'vue'
import SmartImage from '@/components/SmartImage.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { createHomeShareMessage } from '@/lib/share'
import { getRouteParams, replace, resolveSharePath } from '@/lib/navigation'
import { PlanService, type PlanDetail, type PlanRecipe, type PlanRecipeCandidatesResponse, type PlanVisibleUser } from '@/services/plan-service'
import { SocialService, type BuddyCircleMember } from '@/services/social-service'
import { useAuthStore } from '@/stores/auth-store'

const params = getRouteParams<{ id?: string; shareToken?: string }>()
const planService = new PlanService()
const socialService = new SocialService()
const authStore = useAuthStore()

const detail = ref<PlanDetail | null>(null)
const candidates = ref<PlanRecipeCandidatesResponse | null>(null)
const circleMembers = ref<BuddyCircleMember[]>([])
const loadingCandidates = ref(false)
const loadingVisibilityMembers = ref(false)
const submitting = ref(false)
const savingVisibility = ref(false)
const searchKeyword = ref('')
const selectedCategoryId = ref('')
const selectedDishIds = ref<string[]>([])
const draftVisibleUserIds = ref<string[]>([])
const visibilitySheetVisible = ref(false)

const shareToken = computed(() => String(params.shareToken || ''))
const isLoggedIn = computed(() => authStore.isLoggedIn)
const canAddRecipes = computed(() => Boolean(candidates.value?.viewerCanAddRecipes && isLoggedIn.value))
const canManageRecipes = computed(() => Boolean(detail.value?.viewerCanManageRecipes && isLoggedIn.value))
const canEditRecipes = computed(() => canAddRecipes.value || canManageRecipes.value)
const canEditVisibility = computed(() => Boolean(detail.value?.viewerCanDelete && isLoggedIn.value))
const currentShareToken = computed(() => detail.value?.shareToken || shareToken.value)
const candidateSubtitle = computed(() => candidates.value?.sourceLabel || '')
const candidateRecipes = computed<PlanRecipe[]>(() => candidates.value?.recipes || [])
const visibleUsers = computed<PlanVisibleUser[]>(() => detail.value?.visibleUsers || [])
const visibilitySummary = computed(() => visibleUsers.value.length ? `指定 ${visibleUsers.value.length} 人可见` : '全圈可见')
const normalizedDraftVisibleUserIds = computed(() => normalizeVisibleUserDraft(draftVisibleUserIds.value))
const visibilityDraftSummary = computed(() => normalizedDraftVisibleUserIds.value.length ? `已选 ${normalizedDraftVisibleUserIds.value.length} 人` : '全圈可见')
const hasVisibilityChanges = computed(() => !isSameIdSet(normalizedDraftVisibleUserIds.value, visibleUsers.value.map((user) => user.id)))
const existingDishIds = computed(() => (detail.value?.recipes || []).map((recipe) => recipe.id))
const newSelectedDishIds = computed(() => selectedDishIds.value.filter((dishId) => !existingDishIds.value.includes(dishId)))
const removedSelectedDishIds = computed(() => existingDishIds.value.filter((dishId) => !selectedDishIds.value.includes(dishId)))
const hasSelectedDishChanges = computed(() => newSelectedDishIds.value.length > 0 || removedSelectedDishIds.value.length > 0)
const displayMenus = computed<PlanRecipe[]>(() => {
  const addedIds = new Set(existingDishIds.value)
  return [
    ...(detail.value?.recipes || []),
    ...candidateRecipes.value.filter((menu) => !addedIds.has(menu.id)),
  ]
})


const categoryItems = computed(() => {
  const categoryMap = new Map<string, { id: string; name: string }>()
  displayMenus.value.forEach((menu) => {
    if (!menu.categoryId || !menu.categoryName) return
    if (!categoryMap.has(menu.categoryId)) {
      categoryMap.set(menu.categoryId, { id: menu.categoryId, name: menu.categoryName })
    }
  })
  return Array.from(categoryMap.values())
})

const filteredMenus = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return displayMenus.value.filter((menu) => {
    if (selectedCategoryId.value && menu.categoryId !== selectedCategoryId.value) return false
    if (!keyword) return true
    return (
      menu.name.toLowerCase().includes(keyword) ||
      (menu.ingredientNames || []).some((ingredient) => ingredient.toLowerCase().includes(keyword))
    )
  })
})

const shoppingStatusLabel = computed(() => {
  if (!detail.value) return ''
  switch (detail.value.shoppingStatus) {
    case 'NOT_PURCHASED':
      return '未采购'
    case 'PARTIALLY_PURCHASED':
      return '部分采购'
    case 'PURCHASED':
      return '采购完成'
    case 'NOT_STARTED':
    default:
      return '待采购'
  }
})

useShareAppMessage(() => {
  const planId = String(params.id || '')
  const token = currentShareToken.value

  if (!planId || !token) {
    return createHomeShareMessage({
      title: '我在 meoi食堂安排本周吃什么，来一起看看',
    })
  }

  return {
    title: `邀请你看看「${detail.value?.title || '这条计划'}」`,
    path: resolveSharePath({
      name: 'plan-detail',
      params: { id: planId },
      query: { shareToken: token },
    }),
  }
})

onMounted(async () => {
  if (!params.id) {
    replace('plan')
    return
  }

  await authStore.restore()

  // 无分享 token 的详情页仍然要求登录；有分享 token 时允许游客先查看详情。
  if (!shareToken.value && !authStore.isLoggedIn) {
    if (!(await requireAuth('plan-detail'))) return
  }

  await loadDetail()
})

async function loadDetail() {
  try {
    const { data } = await planService.getPlanDetail(String(params.id), shareToken.value || undefined)
    detail.value = data
    selectedDishIds.value = data.recipes.map((recipe) => recipe.id)

    await loadCandidates(data.id)
  } catch (error: any) {
    Message.error(error?.response?.data?.message || (shareToken.value ? '分享计划加载失败' : '计划详情加载失败'))
    replace(shareToken.value ? 'home' : 'plan')
  }
}

async function loadCandidates(planId: string) {
  candidates.value = null
  loadingCandidates.value = true
  try {
    // 候选菜谱接口已经覆盖成员、分享访客、登录后访客三种视角，这里直接以接口返回为准。
    const candidateResult = await planService.getRecipeCandidates(planId, shareToken.value || undefined)
    candidates.value = candidateResult.data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '可选菜谱加载失败')
  } finally {
    loadingCandidates.value = false
  }
}

async function openVisibilitySheet() {
  if (!detail.value) return
  if (!isLoggedIn.value) {
    void requirePlanDetailAuth()
    return
  }
  if (!canEditVisibility.value) return

  draftVisibleUserIds.value = visibleUsers.value.map((user) => user.id)
  visibilitySheetVisible.value = true
  await loadVisibilityMembers()
}

function closeVisibilitySheet() {
  if (savingVisibility.value) return
  visibilitySheetVisible.value = false
}

async function loadVisibilityMembers() {
  if (!detail.value || loadingVisibilityMembers.value) return
  loadingVisibilityMembers.value = true
  try {
    const { data } = await socialService.getCircleMembers(detail.value.circleId)
    circleMembers.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '圈成员加载失败')
  } finally {
    loadingVisibilityMembers.value = false
  }
}

function toggleDraftVisibleUser(userId: string) {
  if (!userId || savingVisibility.value) return
  if (draftVisibleUserIds.value.includes(userId)) {
    const nextIds = draftVisibleUserIds.value.filter((id) => id !== userId)
    if (authStore.user?.id === userId && nextIds.length) {
      Message.info('创建者会始终可见')
      return
    }
    draftVisibleUserIds.value = normalizeVisibleUserDraft(nextIds)
    return
  }
  draftVisibleUserIds.value = normalizeVisibleUserDraft([...draftVisibleUserIds.value, userId])
}

function isDraftVisibleUserSelected(userId: string) {
  return normalizedDraftVisibleUserIds.value.includes(userId)
}

function normalizeVisibleUserDraft(userIds: string[]) {
  const normalized = Array.from(new Set(userIds.filter((userId) => Boolean(userId))))
  if (!normalized.length) return []
  const currentUserId = authStore.user?.id || ''
  if (currentUserId && !normalized.includes(currentUserId)) {
    // 指定可见时创建者必须保留，否则保存后自己可能失去计划入口。
    return [currentUserId, ...normalized]
  }
  return normalized
}

function isSameIdSet(leftIds: string[], rightIds: string[]) {
  if (leftIds.length !== rightIds.length) return false
  const rightSet = new Set(rightIds)
  return leftIds.every((id) => rightSet.has(id))
}

function getVisibleUserName(user: PlanVisibleUser) {
  const name = user.nickname || user.account || '未命名'
  return user.id === authStore.user?.id ? `${name}（我）` : name
}

function getCircleMemberName(member: BuddyCircleMember) {
  const name = member.nickname || member.account || '未命名'
  return member.id === authStore.user?.id ? `${name}（我）` : name
}

async function submitVisibleUsers() {
  if (!detail.value) return
  const visibleUserIds = normalizedDraftVisibleUserIds.value
  if (!hasVisibilityChanges.value) {
    Message.info('可见成员没有变化')
    return
  }

  savingVisibility.value = true
  try {
    const { data } = await planService.updateVisibleUsers(detail.value.id, visibleUserIds)
    detail.value = data
    visibilitySheetVisible.value = false
    Message.success('可见成员已更新')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '可见成员保存失败')
  } finally {
    savingVisibility.value = false
  }
}

function selectCategory(categoryId: string) {
  selectedCategoryId.value = categoryId
}

function toggleSelectedDish(dishId: string) {
  // 本地只维护勾选草稿，保存时再统一计算新增和移除，避免点一下就改计划。
  if (selectedDishIds.value.includes(dishId)) {
    selectedDishIds.value = selectedDishIds.value.filter((id) => id !== dishId)
    return
  }
  selectedDishIds.value = [...selectedDishIds.value, dishId]
}

function handleCandidateTap(menu: PlanRecipe) {
  if (!isLoggedIn.value) {
    void requirePlanDetailAuth()
    return
  }
  const dishId = menu.id
  if (isExistingDish(dishId) && !canToggleExistingDish(menu)) return
  if (!isExistingDish(dishId) && !canAddRecipes.value) return
  toggleSelectedDish(dishId)
}

function canToggleExistingDish(menu: PlanRecipe) {
  return canManageRecipes.value || menu.addedByUserId === authStore.user?.id
}

function isExistingDish(dishId: string) {
  return existingDishIds.value.includes(dishId)
}

function isDishChecked(dishId: string) {
  return selectedDishIds.value.includes(dishId)
}

async function requirePlanDetailAuth() {
  // 登录回跳固定回当前计划详情页，保证分享上下文不会丢。
  if (!(await requireAuth('plan-detail'))) return false
  await authStore.restore()
  return true
}

async function submitSelectedRecipes() {
  if (!detail.value) return
  if (!hasSelectedDishChanges.value) {
    Message.info('菜谱选择没有变化')
    return
  }

  submitting.value = true
  try {
    if (newSelectedDishIds.value.length) {
      await planService.addRecipes(detail.value.id, newSelectedDishIds.value, shareToken.value || undefined)
    }
    for (const dishId of removedSelectedDishIds.value) {
      await planService.removeRecipe(detail.value.id, dishId, shareToken.value || undefined)
    }
    Message.success(getSaveSuccessMessage())
    replace({
      name: 'plan',
      query: {
        date: detail.value.planDate,
        planId: detail.value.id,
        sharedPlanId: detail.value.sharedView ? detail.value.id : undefined,
        shareToken: detail.value.sharedView ? shareToken.value : undefined,
      },
    })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存菜谱失败')
  } finally {
    submitting.value = false
  }
}

function getSaveSuccessMessage() {
  if (newSelectedDishIds.value.length && removedSelectedDishIds.value.length) return '计划菜谱已更新'
  if (removedSelectedDishIds.value.length) return '菜谱已移出计划'
  return '菜谱已加入计划'
}

</script>

<style>
.plan-detail-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.detail-shell {
  padding: 20px 20px calc(112px + env(safe-area-inset-bottom));
}

.detail-sticky-top {
  position: sticky;
  top: 0;
  z-index: 20;
  padding-bottom: 12px;
  background: #f7f6f3;
}

.detail-top-nav {
  margin-top: 2px;
}

.detail-top-nav-inner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.08);
}

.detail-top-nav-title {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  color: #151515;
  font-size: 16px;
  font-weight: 700;
}

.detail-save-button,
.detail-share-button,
.detail-category-pill,
.detail-visibility-edit,
.visibility-member-chip,
.visibility-cancel-button,
.visibility-save-button {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.detail-save-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  z-index: 30;
  width: min(390px, 100vw);
  padding: 12px 20px calc(18px + env(safe-area-inset-bottom));
  transform: translateX(-50%);
  background: rgba(247, 246, 243, 0.96);
  box-shadow: 0 -12px 24px rgba(27, 58, 45, 0.08);
  backdrop-filter: blur(12px);
}

.detail-save-button {
  width: 100%;
  min-height: 50px;
  border-radius: 16px;
  background: #9f5c38;
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  line-height: 50px;
}

.detail-save-button[disabled] {
  opacity: 0.45;
}

.detail-share-button {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 999px;
  background: #edf3ec;
  color: #1b3a2d;
  font-size: 17px;
  font-weight: 700;
  line-height: 32px;
}

.detail-candidate-empty {
  padding: 24px 0 8px;
  color: #6f7c74;
  font-size: 13px;
  text-align: center;
}

.detail-hero,
.detail-summary,
.detail-section-head,
.detail-login-card,
.detail-candidate-card {
  display: flex;
}

.detail-hero,
.detail-section-head,
.detail-candidate-bottom {
  align-items: center;
  justify-content: space-between;
}

.detail-candidate-bottom {
  display: flex;
  gap: 10px;
  width: 100%;
}

.detail-hero,
.detail-summary,
.detail-section,
.detail-visibility-card,
.detail-login-card,
.detail-candidate-card {
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(27, 58, 45, 0.08);
}

.detail-hero {
  gap: 12px;
  margin-bottom: 10px;
  padding: 14px 16px;
}

.detail-hero-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.detail-hero-eyebrow {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.detail-hero-title {
  color: var(--text-main);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.2;
}

.detail-hero-meta {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.detail-hero-chip {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  min-width: 76px;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(27, 58, 45, 0.08);
}

.detail-hero-chip-text {
  color: #1b3a2d;
  font-size: 12px;
  font-weight: 700;
}

.detail-summary {
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
}

.detail-visibility-card {
  margin-bottom: 12px;
  padding: 14px 16px;
}

.detail-visibility-head,
.detail-visible-user-chip,
.visibility-sheet-head,
.visibility-sheet-actions {
  display: flex;
  align-items: center;
}

.detail-visibility-head,
.visibility-sheet-head,
.visibility-sheet-actions {
  justify-content: space-between;
}

.detail-visibility-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.detail-visibility-edit {
  min-width: 58px;
  min-height: 32px;
  flex-shrink: 0;
  border-radius: 999px;
  background: #edf3ec;
  color: #1b3a2d;
  font-size: 12px;
  font-weight: 700;
  line-height: 32px;
}

.detail-visible-user-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.detail-visible-user-chip {
  max-width: 100%;
  gap: 6px;
  min-height: 32px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: #f7f6f3;
}

.detail-visible-user-avatar,
.visibility-member-avatar {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 999px;
  background: #efe6dc;
}

.detail-visible-user-name {
  max-width: 180px;
  overflow: hidden;
  color: #5a4333;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-visibility-desc {
  display: block;
  margin-top: 10px;
  color: #6f7c74;
  font-size: 12px;
  line-height: 1.45;
}

.detail-stat-card {
  flex: 1;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f7f6f3;
}

.detail-stat-label,
.detail-stat-value,
.detail-empty-title,
.detail-empty-desc,
.detail-login-title,
.detail-login-desc,
.detail-section-title,
.detail-section-subtitle {
  display: block;
}

.detail-stat-label {
  color: var(--text-muted);
  font-size: 11px;
}

.detail-stat-value {
  margin-top: 3px;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.detail-section {
  margin-bottom: 16px;
  padding: 16px;
}

.detail-section-head {
  margin-bottom: 12px;
}

.detail-section-title {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
}

.detail-section-subtitle {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
}

.detail-candidate-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-candidate-card {
  gap: 14px;
  width: 100%;
  padding: 10px;
  text-align: left;
}

.detail-candidate-image {
  width: 116px;
  height: 116px;
  flex-shrink: 0;
  border-radius: 16px;
}

.detail-candidate-body,
.detail-candidate-top {
  display: flex;
  flex-direction: column;
}

.detail-candidate-body {
  flex: 1;
  min-width: 0;
  justify-content: center;
  gap: 8px;
}

.detail-candidate-title {
  color: #151515;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
}

.detail-candidate-meta {
  color: #66605a;
  font-size: 12px;
  line-height: 1.5;
}

.detail-preview-note {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f7f6f3;
}

.detail-preview-note-text,
.detail-empty-title {
  display: block;
}

.detail-preview-note-text {
  color: #6b625b;
  font-size: 12px;
  line-height: 1.5;
}

.detail-empty-title {
  color: #151515;
  font-size: 15px;
  font-weight: 700;
}

.detail-empty-desc {
  color: #787774;
  font-size: 12px;
  line-height: 1.55;
}

.detail-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  margin-bottom: 14px;
  padding: 0 14px;
  border-radius: 14px;
  background: #f3efe9;
}

.detail-search-icon {
  color: #8b837c;
  font-size: 16px;
}

.detail-search-input {
  flex: 1;
  min-height: 46px;
  color: #151515;
  font-size: 14px;
}

.detail-search-placeholder {
  color: #8b837c;
}

.detail-category-section {
  margin-bottom: 14px;
}

.detail-category-scroll {
  width: 100%;
}

.detail-category-row {
  display: flex;
  gap: 8px;
  width: max-content;
}

.detail-category-pill {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #f7f6f3;
  color: #5a4333;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.detail-category-pill.active {
  background: #edf3ec;
  color: #346538;
}

.detail-candidate-card.preview {
  opacity: 0.96;
}

.detail-candidate-body {
  min-height: 116px;
}

.detail-owner-chip {
  display: flex;
  max-width: calc(100% - 40px);
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f4eee7;
}

.detail-owner-chip-text {
  overflow: hidden;
  color: #5a4333;
  font-size: 11px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-candidate-radio {
  display: flex;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid #c9c1b8;
  border-radius: 999px;
  background: #ffffff;
}

.detail-candidate-radio.active {
  border-color: #1b3a2d;
  background: #1b3a2d;
}

.detail-candidate-radio-check {
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.detail-empty-card {
  padding: 18px;
  background: #f7f6f3;
  box-shadow: none;
}

.detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #787774;
}

.detail-share-button::after {
  border: none;
}

.visibility-overlay {
  position: fixed;
  inset: 0;
  z-index: 45;
  display: flex;
  align-items: flex-end;
  background: rgba(246, 239, 231, 0.72);
}

.visibility-sheet {
  width: 100%;
  padding: 12px 16px calc(24px + env(safe-area-inset-bottom));
  border-radius: 24px 24px 0 0;
  background: #ffffff;
}

.visibility-sheet-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto;
  border-radius: 999px;
  background: #e6dbcf;
}

.visibility-sheet-head {
  margin-top: 14px;
}

.visibility-sheet-title,
.visibility-sheet-subtitle {
  display: block;
}

.visibility-sheet-title {
  color: var(--text-main);
  font-size: 18px;
  font-weight: 800;
}

.visibility-sheet-subtitle {
  margin-top: 4px;
  color: #9a887a;
  font-size: 12px;
  font-weight: 600;
}

.visibility-member-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 240px;
  margin-top: 14px;
  overflow-y: auto;
}

.visibility-member-chip {
  gap: 8px;
  min-width: 0;
  min-height: 42px;
  padding: 0 10px;
  border: 1px solid #eadfd5;
  border-radius: 999px;
  background: #f7f6f3;
  color: #5a4333;
}

.visibility-member-chip.active {
  border-color: #1b3a2d;
  background: #1b3a2d;
  color: #ffffff;
}

.visibility-member-name {
  min-width: 0;
  overflow: hidden;
  color: inherit;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visibility-member-empty {
  padding: 22px 0 8px;
  color: #9a887a;
  font-size: 13px;
  text-align: center;
}

.visibility-sheet-actions {
  gap: 8px;
  margin-top: 14px;
}

.visibility-cancel-button,
.visibility-save-button {
  flex: 1;
  min-height: 44px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 800;
  line-height: 44px;
}

.visibility-cancel-button {
  background: #f7f3ee;
  color: #5a4333;
}

.visibility-save-button {
  background: #9f5c38;
  color: #ffffff;
}

.visibility-cancel-button[disabled],
.visibility-save-button[disabled] {
  opacity: 0.45;
}
</style>
