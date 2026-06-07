<template>
  <view class="circles-page-root">
    <PullRefreshPage @refresh="refreshCircles">
      <view class="page-shell circles-page">
        <template v-if="activeDetail">
          <main class="circle-content">
            <section class="circle-switch-panel">
              <view class="switch-panel-header">
                <text class="strong">圈子</text>
                <button class="nav-button" @tap="createCircle">＋</button>

              </view>
              <view class="current-circle-summary">
                <view class="current-circle-copy">
                  <text class="circle-name">{{
                    activeDetail.circle.name
                  }}</text>
                  <text class="muted"
                    >{{ activeDetail.stats.memberCount }}人 ·
                    {{ activeDetail.stats.sharedMenuCount }}菜谱 ·
                    正在使用</text
                  >
                </view>
                <button
                  class="circle-switch-button"
                  :disabled="circles.length <= 1"
                  @tap="openCircleSwitcher"
                >
                  <text>切换</text>
                </button>
              </view>
            </section>

            <section class="section-block">
              <view class="section-head">
                <text class="section-title">搭子圈成员</text>
                <text class="section-link"
                  >共 {{ activeDetail.stats.memberCount }} 人</text
                >
              </view>
              <button class="member-entry" @tap="openMembers">
                <view class="member-avatars">
                  <view
                    v-for="member in previewMembers"
                    :key="member.id"
                    class="avatar-badge"
                    :style="{
                      background: avatarPalette[member.avatarTone].bg,
                      color: avatarPalette[member.avatarTone].fg,
                    }"
                  >
                    {{ member.initial }}
                  </view>
                </view>
                <text class="member-entry-arrow">›</text>
              </button>
            </section>

            <section class="section-block recipes-section">
              <view class="section-head">
                <view>
                  <text class="section-title">圈内菜谱</text>
                </view>
              </view>

              <view class="recipe-search">
                <text class="recipe-search-icon">⌕</text>
                <input
                  v-model.trim="searchKeyword"
                  class="recipe-search-input"
                  maxlength="24"
                  placeholder="搜索菜谱名或食材"
                />
              </view>

              <scroll-view
                class="category-group"
                :scroll-x="true"
                :scroll-left="categoryScrollLeft"
                :scroll-with-animation="true"
              >
                <button
                  v-for="category in categories"
                  :key="category"
                  :class="[
                    'category-chip',
                    { active: category === activeCategory },
                  ]"
                  @tap="selectCategory(category)"
                >
                  <text>{{ category }}</text>
                </button>
              </scroll-view>

              <view v-if="visibleMenus.length" class="recipe-grid">
                <button
                  v-for="menu in visibleMenus"
                  :key="menu.id"
                  class="recipe-card"
                  @tap="openDish(menu.id)"
                >
                  <SmartImage :src="menu.image" class-name="recipe-image" />
                  <view class="recipe-copy">
                    <text class="recipe-name">{{ menu.name }}</text>
                    <view class="recipe-meta">
                      <text class="muted">{{ menu.categoryName }}</text>
                      <view
                        class="recipe-owner-avatar"
                        :style="{
                          background: avatarPalette[getMenuOwnerTone(menu)].bg,
                          color: avatarPalette[getMenuOwnerTone(menu)].fg,
                        }"
                      >
                        {{ getMenuOwnerInitial(menu) }}
                      </view>
                    </view>
                    <text
                      v-if="menu.matchedIngredientNames.length"
                      class="recipe-hit"
                    >
                      食材：{{ menu.matchedIngredientNames.join(" / ") }}
                    </text>
                  </view>
                </button>
              </view>

              <view v-else class="empty-card">{{ emptyRecipeText }}</view>
            </section>
          </main>
        </template>

        <section v-else class="status-card">
          <text>{{ statusText }}</text>
          <button v-if="!isLoading" class="create-button" @tap="createCircle">
            <text>新建圈子</text>
          </button>
        </section>
      </view>
    </PullRefreshPage>

    <AppTabBar active="circles" />

    <view
      v-if="switcherVisible"
      class="circle-switch-overlay"
      @tap="closeCircleSwitcher"
    >
      <section class="circle-switch-sheet" @tap.stop>
        <view class="sheet-handle"></view>
        <view class="sheet-head">
          <view>
            <text class="eyebrow">切换圈子</text>
            <text class="sheet-title">选择搭子圈</text>
          </view>
          <button class="sheet-close" @tap="closeCircleSwitcher">×</button>
        </view>
        <view class="circle-option-list">
          <button
            v-for="circle in circles"
            :key="circle.id"
            :class="['circle-option', { active: circle.id === activeCircleId }]"
            @tap="selectCircle(circle.id)"
          >
            <text class="circle-option-mark">{{
              circle.name.slice(0, 1)
            }}</text>
            <view class="circle-option-copy">
              <text class="strong">{{ circle.name }}</text>
              <text class="muted">{{ getCircleStatsText(circle.id) }}</text>
            </view>
            <text class="circle-option-state">{{
              circle.id === activeCircleId ? "当前" : "切换"
            }}</text>
          </button>
        </view>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import Taro from "@tarojs/taro";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import AppTabBar from "@/components/AppTabBar.vue";
import PullRefreshPage from "@/components/PullRefreshPage.vue";
import SmartImage from "@/components/SmartImage.vue";
import { requireAuth } from "@/lib/auth";
import { Message } from "@/lib/feedback";
import { getRouteParams, push } from "@/lib/navigation";
import type { DishSummary } from "@/services/food-service";
import {
  SocialService,
  type BuddyCircleDetail,
  type BuddyCircleMember,
  type BuddyCircleSummary,
} from "@/services/social-service";

type PreviewMember = {
  id: string;
  initial: string;
  avatarTone: number;
  vip: boolean;
};

type VisibleCircleMenu = DishSummary & {
  matchedIngredientNames: string[];
};

const socialService = new SocialService();
const params = getRouteParams() as { id?: string };
const circles = ref<BuddyCircleSummary[]>([]);
const activeDetail = ref<BuddyCircleDetail | null>(null);
const circleDetailsById = ref<Record<string, BuddyCircleDetail>>({});
const activeCircleId = ref("");
const activeCategory = ref("全部");
const categoryScrollLeft = ref(0);
const isLoading = ref(true);
const switcherVisible = ref(false);
const searchKeyword = ref("");
let detailRequestToken = 0;
let persistedCircleId = "";

const avatarPalette = [
  { bg: "#edf3ec", fg: "#346538" },
  { bg: "#f9ebdd", fg: "#9f5c38" },
  { bg: "#eeeaf7", fg: "#6c58a5" },
];


const categories = computed(() => [
  "全部",
  ...Array.from(
    new Set(
      (activeDetail.value?.sharedMenus || [])
        .map((menu) => menu.categoryName)
        .filter(Boolean),
    ),
  ),
]);
const visibleMenus = computed<VisibleCircleMenu[]>(() => {
  const menus = activeDetail.value?.sharedMenus || [];
  const categoryMenus =
    activeCategory.value === "全部"
      ? menus
      : menus.filter((menu) => menu.categoryName === activeCategory.value);
  const keyword = searchKeyword.value.trim().toLowerCase();

  if (!keyword) {
    return categoryMenus.map((menu) => ({
      ...menu,
      matchedIngredientNames: [],
    }));
  }

  return categoryMenus
    .map((menu) => {
      const nameMatched = menu.name.toLowerCase().includes(keyword);
      const matchedIngredientNames = Array.from(
        new Set(
          (menu.ingredientNames || []).filter((ingredientName) =>
            ingredientName.toLowerCase().includes(keyword),
          ),
        ),
      );
      if (!nameMatched && !matchedIngredientNames.length) return null;
      return { ...menu, matchedIngredientNames };
    })
    .filter((menu): menu is VisibleCircleMenu => Boolean(menu));
});
const emptyRecipeText = computed(() => {
  if (searchKeyword.value.trim()) return "没有找到相关菜谱，换个名字或食材试试";
  return "这个分类还没有共享菜谱";
});
const previewMembers = computed<PreviewMember[]>(() =>
  (activeDetail.value?.members || []).slice(0, 7).map((member, index) => ({
    id: member.id,
    initial: getInitial(member),
    avatarTone: index % avatarPalette.length,
    vip: member.vip,
  })),
);
const memberVipUserIds = computed(
  () =>
    new Set(
      (activeDetail.value?.members || [])
        .filter((member) => member.vip)
        .map((member) => member.id),
    ),
);
const statusText = computed(() =>
  isLoading.value ? "正在加载搭子圈..." : "还没有搭子圈",
);

onMounted(async () => {
  if (!(await requireAuth("circles"))) return;
  await loadData();
});

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value))
    activeCategory.value = "全部";
  void centerSelectedCategory();
});

async function loadData(preferredCircleId = "") {
  isLoading.value = true;
  try {
    const [{ data: profile }, { data }] = await Promise.all([
      socialService.getProfile(),
      socialService.getCircles(),
    ]);
    circles.value = data;
    retainCircleDetails(data);
    persistedCircleId = profile.lastSelectedCircleId || "";
    const routeCircleId = params.id || "";
    const savedCircleId =
      persistedCircleId &&
      data.some((circle) => circle.id === persistedCircleId)
        ? persistedCircleId
        : "";
    const retainedCircleId =
      preferredCircleId &&
      data.some((circle) => circle.id === preferredCircleId)
        ? preferredCircleId
        : "";
    const nextCircleId =
      retainedCircleId ||
      routeCircleId ||
      savedCircleId ||
      circles.value[0]?.id ||
      "";

    activeCircleId.value = nextCircleId;
    if (!nextCircleId) {
      activeDetail.value = null;
      return;
    }

    if (nextCircleId !== persistedCircleId) {
      void persistLastSelectedCircle(nextCircleId);
    }
    await loadCircleDetail(nextCircleId);
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "搭子圈加载失败");
  } finally {
    if (!activeCircleId.value) isLoading.value = false;
  }
}

async function refreshCircles() {
  await loadData(activeCircleId.value);
}

async function loadCircleDetail(circleId: string) {
  const requestToken = ++detailRequestToken;
  isLoading.value = true;
  try {
    const { data } = await socialService.getCircleDetail(circleId);
    if (requestToken !== detailRequestToken) return;
    activeDetail.value = data;
    circleDetailsById.value = {
      ...circleDetailsById.value,
      [circleId]: data,
    };
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "圈子详情加载失败");
  } finally {
    if (requestToken === detailRequestToken) isLoading.value = false;
  }
}

function openCircleSwitcher() {
  if (circles.value.length <= 1) return;
  switcherVisible.value = true;
}

function closeCircleSwitcher() {
  switcherVisible.value = false;
}

function selectCircle(circleId: string) {
  closeCircleSwitcher();
  if (!circleId || circleId === activeCircleId.value) return;
  activeCategory.value = "全部";
  categoryScrollLeft.value = 0;
  searchKeyword.value = "";
  activeCircleId.value = circleId;
  void loadCircleDetail(circleId);
  void persistLastSelectedCircle(circleId);
}

async function persistLastSelectedCircle(circleId: string) {
  if (!circleId || circleId === persistedCircleId) return;
  try {
    await socialService.updateLastSelectedCircle(circleId);
    persistedCircleId = circleId;
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "圈子选择保存失败");
  }
}

function selectCategory(category: string) {
  activeCategory.value = category;
  void centerSelectedCategory();
}

async function centerSelectedCategory() {
  await nextTick();

  const [groupRect, activeRect, chipRects] = await new Promise<
    [
      Taro.NodesRef.BoundingClientRectCallbackResult | null,
      Taro.NodesRef.BoundingClientRectCallbackResult | null,
      Taro.NodesRef.BoundingClientRectCallbackResult[],
    ]
  >((resolve) => {
    Taro.createSelectorQuery()
      .select(".category-group")
      .boundingClientRect()
      .select(".category-chip.active")
      .boundingClientRect()
      .selectAll(".category-chip")
      .boundingClientRect()
      .exec((result) => {
        resolve([
          (result?.[0] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ??
            null,
          (result?.[1] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ??
            null,
          (result?.[2] as Taro.NodesRef.BoundingClientRectCallbackResult[]) ??
            [],
        ]);
      });
  });

  if (!groupRect || !activeRect || !chipRects.length) return;

  const contentLeft = Math.min(...chipRects.map((rect) => rect.left));
  const contentRight = Math.max(...chipRects.map((rect) => rect.right));
  const nextScrollLeft =
    categoryScrollLeft.value +
    (activeRect.left - groupRect.left) -
    (groupRect.width - activeRect.width) / 2;
  const maxScrollLeft = Math.max(
    contentRight - contentLeft - groupRect.width,
    0,
  );

  categoryScrollLeft.value = Math.min(
    Math.max(nextScrollLeft, 0),
    maxScrollLeft,
  );
}

function createCircle() {
  push("create-circle");
}

function openMembers() {
  if (!activeCircleId.value) return;
  push({ name: "circle-members", params: { id: activeCircleId.value } });
}

function openDish(id: DishSummary["id"]) {
  push({ name: "dish-detail", params: { id } });
}

function getMenuOwnerInitial(menu: DishSummary) {
  return (menu.ownerNickname || "?").trim().slice(0, 1).toUpperCase();
}

function getMenuOwnerTone(menu: DishSummary) {
  return (
    menu.ownerUserId
      .split("")
      .reduce((sum, char) => sum + char.charCodeAt(0), 0) % avatarPalette.length
  );
}

function getMenuOwnerVip(menu: DishSummary) {
  return memberVipUserIds.value.has(menu.ownerUserId);
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || "?")
    .trim()
    .slice(0, 1)
    .toUpperCase();
}

function getCircleStatsText(circleId: string) {
  const detail = circleDetailsById.value[circleId];
  if (!detail) return "切换后查看统计";
  return `${detail.stats.memberCount}人 · ${detail.stats.sharedMenuCount}菜谱`;
}

function retainCircleDetails(nextCircles: BuddyCircleSummary[]) {
  const nextDetails: Record<string, BuddyCircleDetail> = {};
  nextCircles.forEach((circle) => {
    const detail = circleDetailsById.value[circle.id];
    if (detail) nextDetails[circle.id] = detail;
  });
  circleDetailsById.value = nextDetails;
}
</script>

<style>
.circles-page {
  min-height: 100vh;
  padding: 0 0 120px;
  background: #f7f6f3;
}

.circle-nav {
  height: 54px;
  padding: 14px 20px 0;
  display: grid;
  grid-template-columns: 36px 1fr 36px;
  align-items: center;
}

.nav-placeholder {
  width: 36px;
  height: 36px;
}

.page-title,
.strong,
.section-title,
.circle-name,
.sheet-title,
.recipe-name {
  color: #151515;
  font-weight: 800;
}

.page-title {
  justify-self: center;
  font-size: var(--title-lg);
}

.muted,
.eyebrow {
  color: #787774;
  font-size: 12px;
}

.circle-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 12px 20px 24px;
}

.circle-switch-panel,
.section-block {
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.switch-panel-header,
.current-circle-summary,
.section-head,
.member-entry,
.member-avatars,
.sheet-head,
.circle-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.current-circle-summary {
  min-height: 54px;
  margin-top: 10px;
  border-radius: 12px;
  background: #edf3ec;
  padding: 0 12px;
  gap: 12px;
}

.current-circle-copy,
.section-head view,
.recipe-copy,
.circle-option-copy,
.sheet-head view {
  display: flex;
  flex-direction: column;
}

.current-circle-copy {
  flex: 1;
  min-width: 0;
}

.circle-name {
  font-size: 18px;
}

.circle-switch-button,
.create-button {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #fff;
  color: #9f5c38;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  text-align: center;
}

.circle-switch-button {
  flex-shrink: 0;
  min-width: 52px;
  min-height: 32px;
}

.circle-switch-button[disabled] {
  color: #b48d73;
  opacity: 1;
}

.section-link,
.eyebrow {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
}

.member-entry {
  width: 100%;
  min-height: 58px;
  margin-top: 12px;
  border-radius: 14px;
  background: #f7f4ef;
  padding: 0 12px;
}

.avatar-badge {
  position: relative;
  display: flex;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: -8px;
  border: 2px solid #fff;
  border-radius: 999px;
  font-weight: 800;
}

.category-group {
  white-space: nowrap;
  margin: 14px 0;
}

.recipe-search,
.recipe-search-input {
  display: flex;
  align-items: center;
}

.recipe-search {
  gap: 8px;
  min-height: 42px;
  border-radius: 12px;
  background: #f7f4ef;
  padding: 0 12px;
  margin-top: 12px;
}

.recipe-search-icon {
  color: #9c968e;
  font-size: 14px;
}

.recipe-search-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  color: #151515;
  font-size: 14px;
}

.category-chip {
  display: inline-flex;
  margin-right: 8px;
  border-radius: 999px;
  background: #f5f2ed;
  color: #787774;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 800;
}

.category-chip.active {
  background: #1b3a2d;
  color: #fff;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recipe-card {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: #f7f4ef;
  text-align: left;
}

.recipe-image {
  width: 100%;
  height: 112px;
}

.recipe-copy {
  gap: 3px;
  padding: 10px;
}

.recipe-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recipe-hit {
  color: #346538;
  font-size: 12px;
  font-weight: 700;
}

.recipe-owner-avatar {
  position: relative;
  display: flex;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.95);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
  box-shadow: 0 6px 14px rgba(21, 21, 21, 0.08);
}


.status-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 24px 20px;
}

.circle-switch-overlay {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.circle-switch-sheet {
  width: 100%;
  max-height: calc(100vh - 88px);
  overflow-y: auto;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px 24px;
}

.sheet-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 14px;
  border-radius: 999px;
  background: #ded8d0;
}

.sheet-close {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  font-size: 22px;
}

.circle-option {
  width: 100%;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--line);
  text-align: left;
}

.circle-option-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: #edf3ec;
  color: #346538;
  font-weight: 800;
}

.circle-option-copy {
  flex: 1;
}

.circle-option-state {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 800;
}
</style>
