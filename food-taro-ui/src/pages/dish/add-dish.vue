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
          <input v-model.trim="item.name" class="line-input ingredient-name-input" placeholder="食材名称" />
          <input v-model.trim="item.amount" class="line-input amount-input" placeholder="用量" />
          <button class="delete-link" @tap="removeIngredient(index)">删除</button>
        </view>
      </section>

      <section class="group-section">
        <view class="group-head">
          <view>
            <text class="group-title">步骤</text>
            <text class="group-tip">可选，不写也可以</text>
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
              @tap="form.visibility = option.value"
            >
              {{ option.label }}
            </button>
          </view>
        </view>
      </section>
    </section>
  </view>
</template>

<script setup lang="ts">
import { useDidShow } from '@tarojs/taro'
import { computed, reactive, ref } from 'vue'
import AUploadComponent from '@/components/AUploadComponent.vue'
import { requireAuth } from '@/lib/auth'
import { Message } from '@/lib/feedback'
import { getRouteParams, push, replace } from '@/lib/navigation'
import {
  FoodService,
  type Category,
  type Difficulty,
  type DishDetail,
  type IngredientItem,
  type DishUpsertRequest,
  type MenuVisibility,
  type StepItem,
} from '@/services/food-service'

const foodService = new FoodService()
const params = getRouteParams() as { id?: string; mode?: string }
const categories = ref<Category[]>([])
const saving = ref(false)
const initializing = ref(false)
const formRenderKey = ref(0)
const initializedRouteKey = ref('')

type IngredientFormItem = IngredientItem & {
  clientId: string
}

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
    visibility: 'inherit',
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

const visibilityOptions: Array<{ label: string; value: MenuVisibility }> = [
  { label: '继承默认', value: 'inherit' },
  { label: '公开', value: 'public' },
  { label: '好友可见', value: 'friends' },
  { label: '仅自己', value: 'private' },
]

const isEditMode = computed(() => params.mode === 'edit' || Boolean(params.id))
const dishId = computed(() => String(params.id || ''))
const currentRouteKey = computed(() => `${isEditMode.value ? 'edit' : 'create'}:${dishId.value || 'new'}`)
const pageTitle = computed(() => (isEditMode.value ? '编辑菜谱' : '添加菜谱'))
const categoryNames = computed(() => categories.value.map((item) => item.name))
const categoryIndex = computed(() => Math.max(0, categories.value.findIndex((item) => item.id === form.categoryId)))
const selectedCategoryName = computed(() => categories.value.find((item) => item.id === form.categoryId)?.name || '')

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
    await loadCategories()
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
  form.ingredients = nextForm.ingredients
  form.steps = nextForm.steps
  formRenderKey.value += 1
}

function fillForm(detail: DishDetail) {
  form.name = detail.name || ''
  form.image = detail.image || ''
  form.description = detail.description || ''
  form.categoryId = detail.categoryId || ''
  form.cookTimeMinutes = detail.cookTimeMinutes ?? null
  form.difficulty = (detail.difficulty as Difficulty) || 'medium'
  form.servings = detail.servings ?? 1
  form.visibility = (detail.visibility as MenuVisibility) || 'inherit'
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
  form.ingredients.splice(index, 1)
}

function addStep() {
  form.steps.push({ content: '' })
}

function removeStep(index: number) {
  form.steps.splice(index, 1)
}

function validateForm() {
  if (!form.image) return '请先上传菜品图片'
  if (!form.name) return '请输入菜品名称'
  if (!form.categoryId) return '请选择分类'
  if (!form.description) return '请补充菜品描述'
  if (!form.servings) return '请填写份量'
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
  } catch (error) {
    Message.error('保存失败，请稍后再试')
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
</style>
