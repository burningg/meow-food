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
import { PlanService, type PlanShoppingItem, type PlanShoppingList } from '@/services/plan-service'

const params = getRouteParams<{ id?: string }>()
const planService = new PlanService()
const shopping = ref<PlanShoppingList | null>(null)
const alliumIngredientNames = '蒜,大蒜,蒜头,蒜瓣,独头蒜,紫皮蒜,白皮蒜,腊八蒜,糖蒜,青蒜,蒜米,蒜泥,蒜蓉,蒜末,蒜片,蒜碎,葱,大葱,小葱,香葱,青葱,老葱,火葱,分葱,胡葱,红葱,红葱头,圆葱,洋葱,紫洋葱,白洋葱,黄洋葱,甜洋葱,珍珠洋葱,韭葱,葱花,葱段,葱丝,姜,生姜,老姜,嫩姜,子姜,姜芽,沙姜,山奈,南姜,良姜,高良姜,黄姜,姜黄,洋姜,鬼子姜,菊芋,姜汁,姜末,姜片,姜丝,姜粉,蒜粉,葱粉,葱姜,姜蒜,葱蒜,葱姜蒜,野葱,薤白,藠头,蒜水,葱姜水,葱姜蒜水'.split(',')
const condimentIngredientNames = '油,酒,粉,盐,食盐,井盐,海盐,湖盐,低钠盐,椒盐,糖,白糖,白砂糖,绵白糖,红糖,黑糖,黄糖,冰糖,方糖,糖粉,蜂蜜,枫糖浆,酱油,生抽,老抽,味极鲜,蒸鱼豉油,鱼露,蚝油,醋,陈醋,香醋,米醋,白醋,大红浙醋,果醋,苹果醋,料酒,黄酒,花雕酒,白酒,米酒,清酒,味淋,啤酒,葡萄酒,食用油,植物油,菜籽油,菜油,花生油,大豆油,豆油,玉米油,葵花籽油,橄榄油,茶油,山茶油,芝麻油,香油,花椒油,辣椒油,红油,葱油,蒜油,猪油,黄油,牛油,人造黄油,起酥油,椰油,棕榈油,亚麻籽油,核桃油,牛油果油,调和油,色拉油,鸡油,虾油,豆瓣酱,郫县豆瓣,郫县豆瓣酱,黄豆酱,黄酱,甜面酱,面酱,芝麻酱,花生酱,沙拉酱,蛋黄酱,美乃滋,千岛酱,番茄酱,番茄沙司,辣椒酱,蒜蓉辣酱,剁椒,泡椒,豆豉,水豆豉,干豆豉,腐乳,豆腐乳,南乳,红腐乳,白腐乳,臭腐乳,韭菜花酱,虾酱,蟹酱,鱼子酱,芥末,青芥辣,山葵酱,芥末酱,辣根,蜂蜜芥末酱,黄芥末酱,味噌,豆味噌,咖喱,咖喱块,咖喱粉,咖喱酱,黄咖喱,红咖喱,绿咖喱,冬阴功酱,沙茶酱,沙嗲酱,XO酱,柱侯酱,海鲜酱,排骨酱,叉烧酱,甜辣酱,泰式甜辣酱,照烧酱,黑椒酱,烧烤酱,牛排酱,芝士酱,塔塔酱,凯撒酱,鲍鱼汁,瑶柱酱,味精,味素,鸡精,鸡粉,蘑菇精,蔬之鲜,高汤粉,浓汤宝,松鲜鲜,五香粉,十三香,七味粉,香辛料,花椒,花椒粉,花椒面,花椒粒,白胡椒,白胡椒粉,白胡椒粒,黑胡椒,黑胡椒粉,黑胡椒粒,胡椒碎,辣椒粉,辣椒面,辣椒碎,孜然,孜然粉,小茴香,小茴香粉,八角,大料,桂皮,肉桂,桂枝,香叶,月桂叶,丁香,草果,草豆蔻,白豆蔻,肉豆蔻,豆蔻,砂仁,栀子,陈皮,甘草,罗汉果,百里香,迷迭香,披萨草,牛至,罗勒,紫苏,薄荷,香茅,柠檬草,莳萝,茴香,芹菜籽,芥菜籽,黄芥末籽,黑芥末籽,罂粟籽,奇亚籽,芝麻,白芝麻,黑芝麻,酵母,酵母粉,泡打粉,塔塔粉,小苏打,苏打粉,碱面,食用碱,淀粉,玉米淀粉,土豆淀粉,红薯淀粉,小麦淀粉,生粉,水淀粉,烧烤料,腌肉料,炸鸡粉,面包糠,蒸肉米粉,蒸肉粉,卤料,卤料包,火锅底料,火锅料,麻辣烫底料,麻辣烫料,酸菜鱼料,酸菜鱼底料,水煮鱼料,水煮鱼底料,回锅肉调料,回锅肉料,鱼香肉丝调料,鱼香肉丝料,宫保鸡丁调料,宫保鸡丁料,麻婆豆腐调料,麻婆豆腐料,红烧肉调料,红烧肉料,糖醋汁,糖醋料,照烧汁,照烧料,黑椒汁,黑椒料,意面酱,披萨酱,芝士粉,芝士片,炼乳,巧克力酱,果酱,蓝莓酱,草莓酱,橙子酱,红烧汁,卤水汁,凉拌汁,捞拌汁,油醋汁,柠檬汁,青柠汁,韩式烤肉酱,大酱,韩式辣酱'.split(',')
const alliumIngredientNameSet = new Set(alliumIngredientNames)
const condimentIngredientNameSet = new Set(condimentIngredientNames)

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
    applyShoppingData(data)
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单加载失败')
    replace('plan')
  }
}

async function startOrReloadShopping() {
  if (!params.id) return
  try {
    const { data } = await planService.startShoppingList(String(params.id))
    applyShoppingData(data)
    Message.success(data.shoppingStarted ? '采购清单已准备好' : '采购清单已刷新')
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '采购清单启动失败')
  }
}

async function toggleItem(itemId: string) {
  if (!params.id || !shopping.value?.shoppingStarted) return
  try {
    const { data } = await planService.toggleShoppingItem(String(params.id), itemId)
    applyShoppingData(data)
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
    applyShoppingData(data)
    Message.success('采购清单已重置')
  } catch (error: any) {
    if (error?.message === 'cancelled') return
    Message.error(error?.response?.data?.message || '采购清单重置失败')
  }
}

function applyShoppingData(data: PlanShoppingList) {
  shopping.value = {
    ...data,
    items: sortShoppingItems(data.items),
  }
}

function sortShoppingItems(items: PlanShoppingItem[]) {
  const normalItems: PlanShoppingItem[] = []
  const alliumItems: PlanShoppingItem[] = []
  const condimentItems: PlanShoppingItem[] = []

  // 前端只做分组重排：普通食材保持原顺序，其次葱姜蒜类，最后调味品类。
  items.forEach((item) => {
    if (isAlliumIngredient(item.ingredientName)) {
      alliumItems.push(item)
      return
    }
    if (isCondimentIngredient(item.ingredientName)) {
      condimentItems.push(item)
      return
    }
    normalItems.push(item)
  })

  return [...normalItems, ...alliumItems, ...condimentItems]
}

function isAlliumIngredient(name: string) {
  return alliumIngredientNameSet.has(name.trim())
}

function isCondimentIngredient(name: string) {
  return condimentIngredientNameSet.has(name.trim())
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
