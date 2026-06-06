<template>
  <view class="page-shell form-page">
    <header class="top-nav">
      <button class="nav-text" @tap="cancel">取消</button>
      <text class="page-title">{{ pageTitle }}</text>
      <button class="nav-text save" :disabled="saving" @tap="save">
        {{ saving ? '保存中' : '保存' }}
      </button>
    </header>

    <section :key="formRenderKey" class="form-stack">
      <view class="upload-card">
        <AUploadComponent :file-data="form.image" @success="uploadSuccess" />
        <text>点击添加菜品照片</text>
      </view>

      <section v-if="showAiCard" class="ai-tools-row">
        <view class="ai-button-group">
          <button
            :class="['ai-action-button', { disabled: isAiRecognizeDisabled }]"
            :disabled="isAiRecognizeDisabled"
            @tap="handleAiRecognizeTap"
          >
            <view class="ai-action-badge">
              <text class="ai-action-badge-icon">✦</text>
            </view>
            <text class="ai-action-text">{{ analyzingAi ? '识别中' : 'AI识别' }}</text>
          </button>
          <button
            :class="['ai-action-button', { disabled: isAiImportOpenDisabled }]"
            :disabled="isAiImportOpenDisabled"
            @tap="openAiImportModal"
          >
            <view class="ai-action-badge">
              <text class="ai-action-badge-icon">⇧</text>
            </view>
            <text class="ai-action-text">AI导入</text>
          </button>
        </view>
        <text class="ai-usage-text">{{ aiUsageText }}</text>
      </section>

      <label class="field-block">
        <text>菜品名称</text>
        <input v-model.trim="form.name" class="line-input" placeholder="输入菜品名称" />
      </label>

      <view class="field-block">
        <text>分类</text>
        <picker mode="selector" :range="categoryNames" :value="categoryIndex" @change="selectCategory">
          <view class="line-input picker-line">{{ selectedCategoryName || '选择分类' }}</view>
        </picker>
      </view>

      <label class="field-block">
        <text>描述</text>
        <textarea v-model.trim="form.description" class="textarea-input" placeholder="添加菜品描述..." />
      </label>

      <section class="group-section">
        <view class="group-head">
          <view>
            <text class="group-title">食材</text>
          </view>
          <button class="pill-button" @tap="addIngredient">＋ 添加</button>
        </view>
        <view v-if="!form.ingredients.length" class="empty-helper">暂未添加食材，想写时再补充即可</view>
        <view
          v-for="(item, index) in form.ingredients"
          :key="item.clientId"
          class="row-grid"
        >
          <input
            v-model.trim="item.name"
            class="line-input ingredient-name-input"
            placeholder="食材名称"
            confirm-type="next"
            :focus="isIngredientInputFocused(item.clientId, 'name')"
            @focus="setIngredientInputFocus(item.clientId, 'name')"
            @blur="clearIngredientInputFocus(item.clientId, 'name')"
            @confirm="focusIngredientInput(item.clientId, 'amount')"
          />
          <input
            v-model.trim="item.amount"
            class="line-input amount-input"
            placeholder="用量"
            :confirm-type="hasNextIngredient(index) ? 'next' : 'done'"
            :focus="isIngredientInputFocused(item.clientId, 'amount')"
            @focus="setIngredientInputFocus(item.clientId, 'amount')"
            @blur="clearIngredientInputFocus(item.clientId, 'amount')"
            @confirm="focusNextIngredientName(index)"
          />
          <button class="delete-link" @tap="removeIngredient(index)">删除</button>
        </view>
      </section>

      <section class="group-section">
        <view class="group-head">
          <view>
            <text class="group-title">步骤（可选）</text>
          </view>
          <button class="pill-button" @tap="addStep">＋ 添加</button>
        </view>
        <view v-if="!form.steps.length" class="empty-helper">暂未添加步骤，用户可以稍后再完善做法</view>
        <view v-for="(item, index) in form.steps" :key="`step-${index}`" class="step-row">
          <text class="step-badge">{{ index + 1 }}</text>
          <textarea v-model.trim="item.content" class="step-input" :placeholder="`输入第 ${index + 1} 步`" />
          <button class="delete-link" @tap="removeStep(index)">删除</button>
        </view>
      </section>

      <section class="group-section">
        <view class="group-head simple">
          <text class="group-title">额外信息</text>
        </view>

        <label class="field-row">
          <text>烹饪时间</text>
          <input
            v-model.number="form.cookTimeMinutes"
            class="mini-input"
            type="number"
            placeholder="选填"
          />
          <text class="field-unit">min</text>
        </label>

        <view class="field-row difficulty-row">
          <text>难度</text>
          <view class="difficulty-pills">
            <button
              v-for="option in difficultyOptions"
              :key="option.value"
              :class="['difficulty-pill', { active: form.difficulty === option.value }]"
              @tap="form.difficulty = option.value"
            >
              {{ option.label }}
            </button>
          </view>
        </view>

        <label class="field-row">
          <text>份量</text>
          <input v-model.number="form.servings" class="mini-input" type="number" />
          <text class="field-unit">人份</text>
        </label>

        <view class="field-row visibility-wrap">
          <text>菜单可见范围</text>
          <view class="difficulty-pills visibility-pills">
            <button
              v-for="option in visibilityOptions"
              :key="option.value"
              :class="['difficulty-pill', { active: form.visibility === option.value }]"
              @tap="setVisibility(option.value)"
            >
              {{ option.label }}
            </button>
          </view>
        </view>

        <view v-if="showCircleSelector" class="circle-selector">
          <view class="circle-selector-head">
            <text class="circle-selector-title">选择可见圈子</text>
            <text class="group-tip">{{ form.visibilityCircleIds.length }} 个已选</text>
          </view>
          <view v-if="availableCircles.length" class="circle-chip-list">
            <button
              v-for="circle in availableCircles"
              :key="circle.id"
              :class="['circle-chip', { active: form.visibilityCircleIds.includes(circle.id) }]"
              @tap="toggleVisibilityCircle(circle.id)"
            >
              {{ circle.name }}
            </button>
          </view>
          <view v-else class="empty-helper">你还没有加入圈子，暂时不能使用指定圈子权限</view>
        </view>
      </section>
    </section>

    <DishAiImportModal
      :visible="showAiImportModal"
      :loading="importingAi"
      @close="closeAiImportModal"
      @import="importDishByAi"
    />
  </view>
</template>

<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import AUploadComponent from '@/components/AUploadComponent.vue'
import DishAiImportModal from '@/components/DishAiImportModal.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, push, replace } from '@/lib/navigation'
import { useAuthStore } from '@/stores/auth-store'
import {
  FoodService,
  type Category,
  type Difficulty,
  type DishDetail,
  type DishAiImportRequest,
  type DishAiAnalysisResponse,
  type IngredientItem,
  type DishUpsertRequest,
  type MenuVisibility,
  type StepItem,
} from '@/services/food-service'
import { SocialService, type BuddyCircleSummary, type ProfileResponse, type VipInfo } from '@/services/social-service'

const foodService = new FoodService()
const socialService = new SocialService()
const authStore = useAuthStore()
const params = getRouteParams() as { id?: string; mode?: string }
const categories = ref<Category[]>([])
const saving = ref(false)
const initializing = ref(false)
const analyzingAi = ref(false)
const importingAi = ref(false)
const showAiImportModal = ref(false)
const formRenderKey = ref(0)
const initializedRouteKey = ref('')
const focusedIngredientInput = ref<{ clientId: string; field: 'name' | 'amount' } | null>(null)
const vipInfo = ref<VipInfo | null>(null)
const profileDefaults = ref<ProfileResponse | null>(null)
const availableCircles = ref<BuddyCircleSummary[]>([])

type IngredientFormItem = IngredientItem & {
  clientId: string
}

type DishEditableVisibility = Exclude<MenuVisibility, 'inherit' | 'friends'>

type DishFormState = Omit<DishUpsertRequest, 'ingredients' | 'steps'> & {
  ingredients: IngredientFormItem[]
  steps: StepItem[]
}

let ingredientIdSeed = 0

function nextIngredientClientId() {
  ingredientIdSeed += 1
  return `ingredient-${Date.now()}-${ingredientIdSeed}`
}

function createEmptyIngredient(): IngredientFormItem {
  return {
    clientId: nextIngredientClientId(),
    name: '',
    amount: '',
  }
}

function createDefaultFormState(): DishFormState {
  return {
    name: '',
    image: '',
    description: '',
    categoryId: '',
    cookTimeMinutes: null,
    difficulty: 'medium',
    servings: 1,
    visibility: 'public',
    visibilityCircleIds: [],
    ingredients: [],
    steps: [],
  }
}

const form = reactive<DishFormState>(createDefaultFormState())

const difficultyOptions: Array<{ label: string; value: Difficulty }> = [
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
]

const visibilityOptions: Array<{ label: string; value: DishEditableVisibility }> = [
  { label: '圈内公开', value: 'public' },
  { label: '指定圈子', value: 'circle' },
  { label: '仅自己', value: 'private' },
]

const isEditMode = computed(() => params.mode === 'edit' || Boolean(params.id))
const dishId = computed(() => String(params.id || ''))
const currentRouteKey = computed(() => `${isEditMode.value ? 'edit' : 'create'}:${dishId.value || 'new'}`)
const pageTitle = computed(() => (isEditMode.value ? '编辑菜谱' : '添加菜谱'))
const showAiCard = computed(() => Boolean(authStore.user?.vip))
const categoryNames = computed(() => categories.value.map((item) => item.name))
const categoryIndex = computed(() => Math.max(0, categories.value.findIndex((item) => item.id === form.categoryId)))
const selectedCategoryName = computed(() => categories.value.find((item) => item.id === form.categoryId)?.name || '')
const isAiRecognizeDisabled = computed(() => saving.value || analyzingAi.value || importingAi.value || !form.image)
const isAiImportOpenDisabled = computed(() => saving.value || analyzingAi.value || importingAi.value)
const currentDefaultVisibility = computed(() => profileDefaults.value?.defaultMenuVisibility || authStore.user?.defaultMenuVisibility || 'public')
const defaultCircleIds = computed(() => profileDefaults.value?.defaultMenuCircleIds || authStore.user?.defaultMenuCircleIds || [])
const showCircleSelector = computed(() => form.visibility === 'circle')
const aiUsageText = computed(() => {
  if (!vipInfo.value) return 'VIP权益'
  return `VIP权益，今日剩余${vipInfo.value.dailyRecipeAnalysisRemaining}次`
})

useDidShow(() => {
  if (initializedRouteKey.value === currentRouteKey.value) return
  void initializePage()
})

async function initializePage() {
  if (initializing.value) return
  initializing.value = true

  try {
    if (!(await requireAuth('add-dish'))) return
    resetForm()
    await Promise.all([loadCategories(), loadVisibilityContext()])
    if (!isEditMode.value) {
      applyCreateVisibilityDefaults()
    }
    if (isEditMode.value && dishId.value) {
      await loadDetail(dishId.value)
    }
    initializedRouteKey.value = currentRouteKey.value
  } finally {
    initializing.value = false
  }
}

async function loadCategories() {
  const { data } = await foodService.queryCategory()
  categories.value = data
}

async function loadVisibilityContext() {
  const [profileResult, circlesResult] = await Promise.allSettled([
    socialService.getProfile(),
    socialService.getCircles(),
  ])
  if (profileResult.status === 'fulfilled') {
    profileDefaults.value = profileResult.value.data
    vipInfo.value = !isEditMode.value && authStore.user?.vip ? profileResult.value.data.vipInfo : null
  } else {
    profileDefaults.value = null
    vipInfo.value = null
  }
  if (circlesResult.status === 'fulfilled') {
    availableCircles.value = circlesResult.value.data
  } else {
    availableCircles.value = []
  }
}

async function loadDetail(id: string) {
  try {
    const { data } = await foodService.getDishDetail(id)
    fillForm(data)
  } catch (error) {
    Message.error('菜谱详情加载失败')
  }
}

function resetForm() {
  const nextForm = createDefaultFormState()
  form.name = nextForm.name
  form.image = nextForm.image
  form.description = nextForm.description
  form.categoryId = nextForm.categoryId
  form.cookTimeMinutes = nextForm.cookTimeMinutes
  form.difficulty = nextForm.difficulty
  form.servings = nextForm.servings
  form.visibility = nextForm.visibility
  form.visibilityCircleIds = nextForm.visibilityCircleIds
  form.ingredients = nextForm.ingredients
  form.steps = nextForm.steps
  vipInfo.value = null
  formRenderKey.value += 1
}

function applyCreateVisibilityDefaults() {
  form.visibility = currentDefaultVisibility.value as DishEditableVisibility
  form.visibilityCircleIds = form.visibility === 'circle' ? [...defaultCircleIds.value] : []
}

function fillForm(detail: DishDetail) {
  form.name = detail.name || ''
  form.image = detail.image || ''
  form.description = detail.description || ''
  form.categoryId = detail.categoryId || ''
  form.cookTimeMinutes = detail.cookTimeMinutes ?? null
  form.difficulty = (detail.difficulty as Difficulty) || 'medium'
  form.servings = detail.servings ?? 1
  form.visibility = ((detail.visibility === 'inherit' ? currentDefaultVisibility.value : detail.visibility) as DishEditableVisibility) || 'public'
  form.visibilityCircleIds = [...(detail.visibilityCircleIds || [])]
  form.ingredients = detail.ingredients.length
    ? detail.ingredients.map((item) => ({
        clientId: nextIngredientClientId(),
        name: item.name || '',
        amount: item.amount?.trim() || '适量',
        sort: item.sort,
      }))
    : []
  form.steps = detail.steps.length
    ? detail.steps.map((item) => ({
        content: item.content || '',
        stepNo: item.stepNo,
      }))
    : []
  formRenderKey.value += 1
}

function selectCategory(event: { detail: { value: number } }) {
  form.categoryId = categories.value[event.detail.value]?.id || ''
}

function uploadSuccess(url: string) {
  form.image = url
}

function addIngredient() {
  form.ingredients.push(createEmptyIngredient())
}

function removeIngredient(index: number) {
  const removedClientId = form.ingredients[index]?.clientId
  form.ingredients.splice(index, 1)
  if (removedClientId && focusedIngredientInput.value?.clientId === removedClientId) {
    focusedIngredientInput.value = null
  }
}

function setIngredientInputFocus(clientId: string, field: 'name' | 'amount') {
  focusedIngredientInput.value = { clientId, field }
}

function clearIngredientInputFocus(clientId: string, field: 'name' | 'amount') {
  if (isIngredientInputFocused(clientId, field)) {
    focusedIngredientInput.value = null
  }
}

function isIngredientInputFocused(clientId: string, field: 'name' | 'amount') {
  return focusedIngredientInput.value?.clientId === clientId && focusedIngredientInput.value.field === field
}

function focusIngredientInput(clientId: string, field: 'name' | 'amount') {
  focusedIngredientInput.value = null
  // 先清空再设置，确保回车时 Taro 重新触发对应输入框聚焦。
  setTimeout(() => {
    focusedIngredientInput.value = { clientId, field }
  }, 0)
}

function hasNextIngredient(index: number) {
  return index < form.ingredients.length - 1
}

function focusNextIngredientName(index: number) {
  const nextIngredient = form.ingredients[index + 1]
  if (!nextIngredient) {
    focusedIngredientInput.value = null
    return
  }
  focusIngredientInput(nextIngredient.clientId, 'name')
}

function addStep() {
  form.steps.push({ content: '' })
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
}

function toggleVisibilityCircle(circleId: string) {
  if (!circleId) return
  if (form.visibilityCircleIds.includes(circleId)) {
    form.visibilityCircleIds = form.visibilityCircleIds.filter((id) => id !== circleId)
    return
  }
  form.visibilityCircleIds = [...form.visibilityCircleIds, circleId]
}

function setVisibility(value: DishEditableVisibility) {
  form.visibility = value
  if (value !== 'circle') {
    form.visibilityCircleIds = []
    return
  }
  if (!form.visibilityCircleIds.length && !isEditMode.value) {
    form.visibilityCircleIds = [...defaultCircleIds.value]
  }
}

function toIngredientFormItems(items: IngredientItem[]) {
  return items.map((item, index) => ({
    clientId: nextIngredientClientId(),
    name: item.name || '',
    amount: item.amount || '适量',
    sort: item.sort ?? index + 1,
  }))
}

function toStepItems(items: StepItem[]) {
  return items.map((item, index) => ({
    content: item.content || '',
    stepNo: item.stepNo ?? index + 1,
  }))
}

function updateVipUsage(data: DishAiAnalysisResponse) {
  if (!vipInfo.value || !data.usage) return
  vipInfo.value = {
    ...vipInfo.value,
    dailyRecipeAnalysisLimit: data.usage.dailyLimit,
    dailyRecipeAnalysisUsed: data.usage.usedToday,
    dailyRecipeAnalysisRemaining: data.usage.remainingToday,
  }
}

function applyAiNameIfEmpty(data: DishAiAnalysisResponse) {
  const aiName = data.name?.trim()
  // 仅当用户没有填写名称时，才使用 AI 识别/导入返回的菜品名称，避免覆盖用户输入。
  if (!form.name.trim() && aiName) {
    form.name = aiName
  }
}

function applyAiAnalysisResult(data: DishAiAnalysisResponse) {
  applyAiNameIfEmpty(data)
  form.ingredients = toIngredientFormItems(data.ingredients || [])
  form.steps = toStepItems(data.steps || [])
  updateVipUsage(data)
  formRenderKey.value += 1
}

function applyAiImportResult(data: DishAiAnalysisResponse) {
  applyAiNameIfEmpty(data)
  form.ingredients = toIngredientFormItems(data.ingredients || [])
  form.steps = toStepItems(data.steps || [])
  updateVipUsage(data)
  formRenderKey.value += 1
}

async function analyzeDish() {
  if (!form.image) {
    Message.warning('请先上传菜品图片')
    return
  }
  if (analyzingAi.value) return

  analyzingAi.value = true
  try {
    const { data } = await foodService.analyzeDishByAi({
      image: form.image,
      name: form.name || undefined,
    })
    applyAiAnalysisResult(data)
    Message.success('AI 已填充菜谱')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || 'AI 识别失败，请稍后再试')
  } finally {
    analyzingAi.value = false
  }
}

function handleAiRecognizeTap() {
  if (isAiRecognizeDisabled.value) return
  void analyzeDish()
}

function openAiImportModal() {
  if (isAiImportOpenDisabled.value) return
  showAiImportModal.value = true
}

function closeAiImportModal() {
  if (importingAi.value) return
  showAiImportModal.value = false
}

async function importDishByAi(payload: DishAiImportRequest) {
  if (importingAi.value) return

  importingAi.value = true
  try {
    const { data } = await foodService.importDishByAi(payload)
    applyAiImportResult(data)
    showAiImportModal.value = false
    Message.success('AI 已填充，请检查后再保存')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || 'AI 导入失败，请稍后再试')
  } finally {
    importingAi.value = false
  }
}

function validateForm() {
  if (!form.image) return '请先上传菜品图片'
  if (!form.name) return '请输入菜品名称'
  if (!form.categoryId) return '请选择分类'
  if (!form.description) return '请补充菜品描述'
  if (!form.servings) return '请填写份量'
  if (form.visibility === 'circle' && !form.visibilityCircleIds.length) return '请选择至少一个可见圈子'
  return ''
}

function normalizedIngredients() {
  return form.ingredients
    .filter((item) => item.name.trim())
    .map((item, index) => ({
      name: item.name.trim(),
      amount: item.amount.trim() || '适量',
      sort: index + 1,
    }))
}

function normalizedSteps() {
  return form.steps
    .filter((item) => item.content.trim())
    .map((item, index) => ({
      content: item.content.trim(),
      stepNo: index + 1,
    }))
}

function normalizedCookTimeMinutes() {
  return typeof form.cookTimeMinutes === 'number' && Number.isFinite(form.cookTimeMinutes)
    ? form.cookTimeMinutes
    : null
}

async function save() {
  const errorText = validateForm()
  if (errorText) {
    Message.warning(errorText)
    return
  }

  const payload: DishUpsertRequest = {
    ...form,
    cookTimeMinutes: normalizedCookTimeMinutes(),
    visibility: form.visibility,
    visibilityCircleIds: form.visibility === 'circle' ? [...form.visibilityCircleIds] : [],
    ingredients: normalizedIngredients(),
    steps: normalizedSteps(),
  }

  saving.value = true
  try {
    const response = isEditMode.value
      ? await foodService.updateDish(dishId.value, payload)
      : await foodService.createDish(payload)
    Message.success('保存成功')
    replace({ name: 'dish-detail', params: { id: response.data.id } })
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '保存失败，请稍后再试')
  } finally {
    saving.value = false
  }
}

function cancel() {
  if (isEditMode.value && dishId.value) {
    push({ name: 'dish-detail', params: { id: dishId.value } })
    return
  }
  push('home')
}
</script>

<style>
.form-page {
  padding-bottom: 48px;
}

.page-title {
  color: var(--text-main);
  font-size: var(--title-sm);
  font-weight: 800;
}

.nav-text {
  color: #8b8b8b;
  font-size: var(--text-md);
}

.nav-text.save {
  color: var(--accent);
  font-weight: 800;
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-card,
.group-section,
.field-block {
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 12px 28px rgba(27, 58, 45, 0.08);
}

.upload-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  color: #8b8b8b;
  font-size: var(--text-sm);
}

.ai-tools-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.ai-button-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-action-button {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  padding: 6px 10px;
  border: 1px solid #ead9c4;
  border-radius: 999px;
  background: #fff8ef;
  color: #9f5c38;
}

.ai-action-button.disabled {
  opacity: 0.45;
}

.ai-action-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #f7e5c8;
  flex-shrink: 0;
}

.ai-action-badge-icon {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.ai-action-text,
.ai-usage-text {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.ai-usage-text {
  margin-left: auto;
  text-align: right;
  white-space: nowrap;
}

.field-block {
  padding: 16px;
  color: #8b8b8b;
  font-size: var(--text-sm);
  font-weight: 700;
}

.line-input,
.picker-line {
  min-height: 46px;
  border-bottom: 1px solid var(--line);
  color: var(--text-main);
}

.textarea-input,
.step-input {
  min-height: 88px;
  border-radius: 14px;
  background: #f0ede8;
  padding: 12px 14px;
}

.group-section {
  padding: 16px;
}

.group-head,
.field-row,
.difficulty-pills,
.row-grid,
.step-row {
  display: flex;
}

.group-head,
.field-row {
  align-items: center;
  justify-content: space-between;
}

.group-head {
  margin-bottom: 12px;
}

.group-title {
  color: var(--text-main);
  font-size: var(--title-md);
  font-weight: 800;
}

.group-tip,
.empty-helper {
  color: #8b8b8b;
  font-size: var(--text-sm);
}

.empty-helper {
  padding: 14px 0 4px;
}

.pill-button {
  border-radius: 999px;
  background: var(--accent-soft);
  color: #7a9e7e;
  padding: 7px 12px;
  font-size: var(--text-xs);
  font-weight: 800;
}

.row-grid,
.step-row,
.field-row {
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line);
}

.row-grid {
  align-items: center;
}

.ingredient-name-input {
  flex: 1;
  border-bottom: none;
}

.amount-input {
  max-width: 92px;
  text-align: right;
  border-bottom: none;
}

.delete-link {
  color: #8b8b8b;
  white-space: nowrap;
}

.step-row {
  align-items: flex-start;
}

.step-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-top: 6px;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: 800;
}

.step-input {
  flex: 1;
}

.field-row > text:first-child {
  color: #8b8b8b;
  font-size: var(--text-sm);
  font-weight: 800;
}

.mini-input {
  max-width: 88px;
  text-align: right;
}

.field-unit {
  color: #8b8b8b;
  font-size: var(--text-sm);
}

.difficulty-pills {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  flex: 1;
}

.difficulty-pill {
  border-radius: 999px;
  background: #f0ede8;
  color: #6b6a67;
  padding: 8px 12px;
  font-size: var(--text-xs);
  font-weight: 800;
}

.difficulty-pill.active {
  background: #1b3a2d;
  color: #fff;
}

.visibility-wrap {
  align-items: flex-start;
}

.circle-selector {
  margin-top: 12px;
  border-radius: 16px;
  background: #f7f4ef;
  padding: 12px;
}

.circle-selector-title {
  color: var(--text-main);
  font-size: var(--text-sm);
  font-weight: 800;
}

.circle-selector-head,
.circle-chip-list {
  display: flex;
}

.circle-selector-head {
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.circle-chip-list {
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.circle-chip {
  border-radius: 999px;
  background: #ebe4d8;
  color: #6e6253;
  padding: 8px 12px;
  font-size: var(--text-xs);
  font-weight: 800;
}

.circle-chip.active {
  background: #9f5c38;
  color: #fff;
}
</style>
