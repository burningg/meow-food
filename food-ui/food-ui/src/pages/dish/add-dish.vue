<template>
  <div class="page-shell form-page">
    <header class="top-nav">
      <button class="nav-text" type="button" @click="cancel">取消</button>
      <h1>{{ pageTitle }}</h1>
      <button class="nav-text save" type="button" :disabled="saving" @click="save">
        {{ saving ? '保存中' : '保存' }}
      </button>
    </header>

    <section class="form-stack">
      <div class="upload-card">
        <AUploadComponent :fileData="form.image" @success="uploadSuccess" />
        <p>点击添加菜品照片</p>
      </div>

      <label class="field-block">
        <span>菜品名称</span>
        <input v-model.trim="form.name" class="line-input" placeholder="输入菜品名称" />
      </label>

      <label class="field-block">
        <span>分类</span>
        <select v-model="form.categoryId" class="line-input select-input">
          <option value="" disabled>选择分类</option>
          <option v-for="category in categories" :key="category.id" :value="category.id">
            {{ category.name }}
          </option>
        </select>
      </label>

      <label class="field-block">
        <span>描述</span>
        <textarea
          v-model.trim="form.description"
          class="textarea-input"
          rows="4"
          placeholder="添加菜品描述..."
        />
      </label>

      <section class="group-section">
        <div class="group-head">
          <h2>食材</h2>
          <button class="pill-button" type="button" @click="addIngredient">＋ 添加</button>
        </div>
        <div v-for="(item, index) in form.ingredients" :key="`ingredient-${index}`" class="row-grid">
          <input v-model.trim="item.name" class="line-input" placeholder="食材名称" />
          <input v-model.trim="item.amount" class="line-input amount-input" placeholder="用量" />
          <button class="delete-link" type="button" @click="removeIngredient(index)">删除</button>
        </div>
      </section>

      <section class="group-section">
        <div class="group-head">
          <h2>步骤</h2>
          <button class="pill-button" type="button" @click="addStep">＋ 添加</button>
        </div>
        <div v-for="(item, index) in form.steps" :key="`step-${index}`" class="step-row">
          <span class="step-badge">{{ index + 1 }}</span>
          <textarea
            v-model.trim="item.content"
            class="step-input"
            rows="2"
            :placeholder="`输入第 ${index + 1} 步`"
          />
          <button class="delete-link" type="button" @click="removeStep(index)">删除</button>
        </div>
      </section>

      <section class="group-section">
        <div class="group-head simple">
          <h2>额外信息</h2>
        </div>

        <label class="field-row">
          <span>烹饪时间</span>
          <input v-model.number="form.cookTimeMinutes" class="mini-input" type="number" min="1" />
          <small>min</small>
        </label>

        <div class="field-row difficulty-row">
          <span>难度</span>
          <div class="difficulty-pills">
            <button
              v-for="option in difficultyOptions"
              :key="option.value"
              :class="['difficulty-pill', { active: form.difficulty === option.value }]"
              type="button"
              @click="form.difficulty = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <label class="field-row">
          <span>份量</span>
          <input v-model.number="form.servings" class="mini-input" type="number" min="1" />
          <small>人份</small>
        </label>

        <label class="field-row switch-row">
          <span>首页推荐</span>
          <input v-model="form.isFeatured" type="checkbox" />
        </label>

        <div class="field-row visibility-wrap">
          <span>菜单可见范围</span>
          <div class="difficulty-pills visibility-pills">
            <button
              v-for="option in visibilityOptions"
              :key="option.value"
              :class="['difficulty-pill', { active: form.visibility === option.value }]"
              type="button"
              @click="form.visibility = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import AUploadComponent from '@/components/AUploadComponent.vue'
import {
  FoodService,
  type Category,
  type Difficulty,
  type DishDetail,
  type DishUpsertRequest,
  type MenuVisibility,
} from '@/services/food-service'

const router = useRouter()
const route = useRoute()
const foodService = new FoodService()

const categories = ref<Category[]>([])
const saving = ref(false)

const form = reactive<DishUpsertRequest>({
  name: '',
  image: '',
  description: '',
  categoryId: '',
  cookTimeMinutes: 45,
  difficulty: 'medium',
  servings: 4,
  visibility: 'inherit',
  isFeatured: false,
  ingredients: [
    { name: '', amount: '' },
    { name: '', amount: '' },
  ],
  steps: [{ content: '' }],
})

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

const isEditMode = computed(() => route.name === 'edit-dish')
const dishId = computed(() => String(route.params.id || ''))
const pageTitle = computed(() => (isEditMode.value ? '编辑菜谱' : '添加菜谱'))

onMounted(async () => {
  await loadCategories()
  if (isEditMode.value && dishId.value) {
    await loadDetail(dishId.value)
  }
})

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

function fillForm(detail: DishDetail) {
  form.name = detail.name
  form.image = detail.image
  form.description = detail.description
  form.categoryId = detail.categoryId
  form.cookTimeMinutes = detail.cookTimeMinutes
  form.difficulty = detail.difficulty
  form.servings = detail.servings
  form.visibility = detail.visibility
  form.isFeatured = detail.isFeatured
  form.ingredients = detail.ingredients.length ? detail.ingredients.map((item) => ({ ...item })) : [{ name: '', amount: '' }]
  form.steps = detail.steps.length ? detail.steps.map((item) => ({ ...item })) : [{ content: '' }]
}

function uploadSuccess(url: string) {
  form.image = url
}

function addIngredient() {
  form.ingredients.push({ name: '', amount: '' })
}

function removeIngredient(index: number) {
  if (form.ingredients.length === 1) {
    form.ingredients[0] = { name: '', amount: '' }
    return
  }
  form.ingredients.splice(index, 1)
}

function addStep() {
  form.steps.push({ content: '' })
}

function removeStep(index: number) {
  if (form.steps.length === 1) {
    form.steps[0] = { content: '' }
    return
  }
  form.steps.splice(index, 1)
}

function validateForm() {
  if (!form.image) return '请先上传菜品图片'
  if (!form.name) return '请输入菜品名称'
  if (!form.categoryId) return '请选择分类'
  if (!form.description) return '请补充菜品描述'
  if (!form.cookTimeMinutes) return '请填写烹饪时间'
  if (!form.servings) return '请填写份量'
  const ingredients = normalizedIngredients()
  if (!ingredients.length) return '请至少填写一个食材'
  const steps = normalizedSteps()
  if (!steps.length) return '请至少填写一个步骤'
  return ''
}

function normalizedIngredients() {
  return form.ingredients
    .filter((item) => item.name.trim() && item.amount.trim())
    .map((item, index) => ({
      name: item.name.trim(),
      amount: item.amount.trim(),
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

async function save() {
  const errorText = validateForm()
  if (errorText) {
    Message.warning(errorText)
    return
  }

  const payload: DishUpsertRequest = {
    ...form,
    ingredients: normalizedIngredients(),
    steps: normalizedSteps(),
  }

  saving.value = true
  try {
    const response = isEditMode.value
      ? await foodService.updateDish(dishId.value, payload)
      : await foodService.createDish(payload)
    Message.success('保存成功')
    router.replace({ name: 'dish-detail', params: { id: response.data.id } })
  } catch (error) {
    Message.error('保存失败，请稍后再试')
  } finally {
    saving.value = false
  }
}

function cancel() {
  if (isEditMode.value && dishId.value) {
    router.push({ name: 'dish-detail', params: { id: dishId.value } })
    return
  }
  router.push({ name: 'home' })
}
</script>

<style scoped>
.form-page {
  padding-bottom: 48px;
}

.top-nav,
.group-head,
.field-row,
.difficulty-pills,
.row-grid,
.step-row {
  display: flex;
}

.top-nav,
.group-head,
.field-row {
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  margin-bottom: 18px;
}

.top-nav h1,
.group-head h2 {
  margin: 0;
  color: #1b3a2d;
}

.top-nav h1 {
  font-size: 1rem;
  font-weight: 700;
}

.nav-text {
  border: none;
  background: none;
  color: #8b8b8b;
  font-size: 0.95rem;
}

.nav-text.save {
  color: #c4704b;
  font-weight: 700;
}

.form-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-card,
.group-section,
.field-block {
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 12px 28px rgba(27, 58, 45, 0.08);
}

.upload-card {
  padding: 20px;
  text-align: center;
}

.upload-card p {
  margin-top: 10px;
  color: #8b8b8b;
  font-size: 0.85rem;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

.field-block span,
.field-row span {
  color: #8b8b8b;
  font-size: 0.82rem;
  font-weight: 600;
}

.line-input,
.textarea-input,
.mini-input,
.step-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #1b3a2d;
  font-size: 0.95rem;
}

.line-input,
.field-row {
  min-height: 46px;
}

.line-input,
.select-input,
.field-row,
.row-grid,
.step-row {
  border-bottom: 1px solid #e8e5e0;
}

.field-block:last-child .line-input {
  border-bottom: none;
}

.textarea-input,
.step-input {
  resize: vertical;
  background: #f0ede8;
  border-radius: 14px;
  padding: 12px 14px;
}

.group-section {
  padding: 16px;
}

.group-head {
  margin-bottom: 12px;
}

.group-head.simple {
  margin-bottom: 0;
}

.group-head h2 {
  font-family: 'Playfair Display', serif;
  font-size: 1.06rem;
}

.pill-button {
  border: none;
  border-radius: 999px;
  background: #e8f0e9;
  color: #7a9e7e;
  padding: 7px 12px;
  font-size: 0.76rem;
  font-weight: 700;
}

.row-grid {
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}

.row-grid .line-input {
  border-bottom: none;
}

.amount-input {
  max-width: 92px;
  text-align: right;
}

.delete-link {
  border: none;
  background: none;
  color: #8b8b8b;
  white-space: nowrap;
}

.step-row {
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
}

.step-badge {
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #c4704b;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.74rem;
  font-weight: 700;
  margin-top: 6px;
}

.step-input {
  min-height: 76px;
}

.field-row {
  gap: 12px;
  padding: 12px 0;
}

.field-row:last-child {
  border-bottom: none;
}

.mini-input {
  max-width: 80px;
  text-align: right;
}

.field-row small {
  color: #8b8b8b;
}

.difficulty-row {
  align-items: flex-start;
}

.visibility-wrap {
  align-items: flex-start;
}

.visibility-pills {
  justify-content: flex-end;
  flex-wrap: wrap;
  max-width: 220px;
}

.difficulty-pills {
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.difficulty-pill {
  border: none;
  border-radius: 999px;
  background: #f5f2ed;
  color: #8b8b8b;
  padding: 7px 14px;
}

.difficulty-pill.active {
  background: #c4704b;
  color: #fff;
}

.switch-row input {
  width: 18px;
  height: 18px;
}
</style>
