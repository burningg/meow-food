<template>
  <view class="page-shell circle-detail-page">
    <header class="top-nav circle-nav">
      <button class="icon-shell" @tap="goBack('circles')">‹</button>
      <text class="page-title">美食搭子</text>
      <view class="circle-nav-actions">
        <button class="icon-shell share-shell" open-type="share">↗</button>
        <button class="icon-shell" @tap="openInvitePicker">＋</button>
      </view>
    </header>

    <template v-if="detail">
      <section class="overview-card">
        <view class="overview-head">
          <view class="overview-copy">
            <text class="eyebrow">当前圈子</text>
            <text class="circle-title">{{ detail.circle.name }}</text>
          </view>
          <text class="state-pill"
            >{{ detail.stats.memberCount }}人 ·
            {{ detail.stats.sharedMenuCount }}菜谱</text
          >
        </view>

        <scroll-view
          v-if="switcherCircles.length"
          class="circle-switch-list"
          scroll-x
        >
          <button
            v-for="circle in switcherCircles"
            :key="circle.id"
            :class="[
              'circle-switch-row',
              { active: circle.id === activeCircleId },
            ]"
            @tap="switchCircle(circle.id)"
          >
            <text>{{ circle.name }}</text>
            <text
              >{{ circle.memberCount }}人 ·
              {{ circle.sharedMenuCount }}菜谱</text
            >
          </button>
        </scroll-view>
      </section>

      <section class="section-block">
        <view class="section-head">
          <view>
            <text class="section-title">搭子圈成员</text>
            <text class="muted">点击进入成员页，查看圈成员关系</text>
          </view>
          <text class="section-link">共 {{ detail.stats.memberCount }} 人</text>
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

        <scroll-view
          class="category-strip"
          :scroll-x="true"
          :scroll-left="categoryScrollLeft"
          :scroll-with-animation="true"
        >
          <view class="category-strip-content">
            <button
              :class="['category-pill', { active: activeCategory === '全部' }]"
              @tap="selectCategory('')"
            >
              全部
            </button>
            <button
              v-for="category in categories"
              :key="category"
              :class="[
                'category-pill',
                { active: category === activeCategory },
              ]"
              @tap="selectCategory(category)"
            >
              {{ category }}
            </button>
          </view>
        </scroll-view>

        <view v-if="visibleMenus.length" class="recent-row">
          <button
            v-for="menu in visibleMenus"
            :key="menu.id"
            class="recent-card"
            @tap="openDish(menu.id)"
          >
            <SmartImage :src="menu.image" class-name="recent-image" />
            <view class="recent-copy">
              <text class="recent-name">{{ menu.name }}</text>
              <text class="recent-category">{{ menu.categoryName }}</text>
              <text class="recent-owner">{{ menu.ownerNickname }}创建</text>
            </view>
          </button>
        </view>
        <view v-else class="empty-card">这个分类还没有共享菜谱</view>
      </section>
    </template>

    <section v-else class="status-card">{{ statusText }}</section>

    <view
      v-if="inviteModalVisible"
      class="invite-modal-overlay"
      @tap="closeInvitePicker"
    >
      <section class="invite-modal-card" @tap.stop>
        <view class="invite-modal-handle"></view>
        <view class="invite-modal-head">
          <view class="invite-modal-title">
            <text class="eyebrow">搭子圈邀请</text>
          </view>
          <button
            class="modal-close-shell"
            :disabled="inviteSubmitting"
            @tap="closeInvitePicker"
          >
            ×
          </button>
        </view>

        <view v-if="inviteCandidates.length" class="invite-friends-card">
          <article
            v-for="(friend, index) in inviteCandidates"
            :key="friend.id"
            class="invite-friend-row"
          >
            <button
              class="invite-friend-main"
              :disabled="inviteSubmitting"
              @tap="selectedFriendId = friend.id"
            >
              <view
                :class="['invite-avatar-box', inviteAvatarToneClass(index)]"
              >
                <text>{{ avatarInitial(friend.nickname) }}</text>
              </view>
              <view class="invite-friend-copy">
                <text class="strong">{{ friend.nickname }}</text>
                <text class="muted">{{ inviteFriendMeta(friend, index) }}</text>
              </view>
              <text
                :class="[
                  'invite-check',
                  { active: selectedFriendId === friend.id },
                ]"
              >
                {{ selectedFriendId === friend.id ? "✓" : "" }}
              </text>
            </button>
          </article>
        </view>

        <view v-else class="invite-empty-card">
          <text class="strong">{{ inviteEmptyTitle }}</text>
          <text class="muted">{{ inviteEmptyText }}</text>
        </view>

        <view class="invite-modal-footer">
          <text class="muted">一次选择 1 位好友发出邀请。</text>
          <view class="invite-modal-actions">
            <button
              class="modal-action ghost"
              :disabled="inviteSubmitting"
              @tap="closeInvitePicker"
            >
              取消
            </button>
            <button
              class="modal-action primary"
              :disabled="!selectedFriendId || inviteSubmitting || inviteLoading"
              @tap="submitInvite"
            >
              {{ inviteSubmitting ? "邀请中..." : inviteButtonText }}
            </button>
          </view>
        </view>
      </section>
    </view>
  </view>
</template>

<script setup lang="ts">
import Taro, { useShareAppMessage } from "@tarojs/taro";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import SmartImage from "@/components/SmartImage.vue";
import { requireAuth } from "@/lib/auth";
import { Message } from "@/lib/feedback";
import {
  getRouteParams,
  goBack,
  push,
  resolveSharePath,
} from "@/lib/navigation";
import type { DishSummary } from "@/services/food-service";
import {
  SocialService,
  type BuddyCircleDetail,
  type BuddyCircleMember,
  type BuddyCircleSummary,
  type FriendItem,
} from "@/services/social-service";
import { useAuthStore } from "@/stores/auth-store";

type PreviewMember = { id: string; initial: string; avatarTone: number };

const params = getRouteParams() as { id?: string };
const socialService = new SocialService();
const authStore = useAuthStore();
const detail = ref<BuddyCircleDetail | null>(null);
const circles = ref<BuddyCircleSummary[]>([]);
const activeCategory = ref("全部");
const categoryScrollLeft = ref(0);
const isLoading = ref(true);
const inviteModalVisible = ref(false);
const inviteLoading = ref(false);
const inviteSubmitting = ref(false);
const inviteLoadFailed = ref(false);
const friends = ref<FriendItem[]>([]);
const selectedFriendId = ref("");
let detailRequestToken = 0;

const avatarPalette = [
  { bg: "#edf3ec", fg: "#346538" },
  { bg: "#f9ebdd", fg: "#9f5c38" },
  { bg: "#eeeaf7", fg: "#6c58a5" },
];

const circleId = computed(() => String(params.id || ""));
const activeCircleId = computed(
  () => detail.value?.circle.id || circleId.value,
);
const switcherCircles = computed(() =>
  circles.value.length
    ? circles.value
    : detail.value
      ? [detail.value.circle]
      : [],
);
const categories = computed(() =>
  Array.from(
    new Set(
      (detail.value?.sharedMenus || [])
        .map((menu) => menu.categoryName)
        .filter(Boolean),
    ),
  ),
);
const visibleMenus = computed(() => {
  const menus = detail.value?.sharedMenus || [];
  if (activeCategory.value === "全部") return menus.slice(0, 3);
  return menus
    .filter((menu) => menu.categoryName === activeCategory.value)
    .slice(0, 3);
});
const previewMembers = computed<PreviewMember[]>(() =>
  (detail.value?.members || []).slice(0, 3).map((member, index) => ({
    id: member.id,
    initial: getInitial(member),
    avatarTone: index % avatarPalette.length,
  })),
);
const statusText = computed(() =>
  isLoading.value ? "正在加载搭子圈..." : "没有找到这个搭子圈",
);
const circleMemberIds = computed(
  () => new Set((detail.value?.members || []).map((member) => member.id)),
);
const inviteCandidates = computed(() =>
  friends.value.filter((friend) => !circleMemberIds.value.has(friend.id)),
);
const inviteButtonText = computed(() => {
  const target = inviteCandidates.value.find(
    (friend) => friend.id === selectedFriendId.value,
  );
  return target ? `邀请${target.nickname}加入搭子圈` : "选择好友后邀请";
});
const inviteEmptyTitle = computed(() => {
  if (inviteLoading.value) return "正在加载好友";
  if (inviteLoadFailed.value) return "好友加载失败";
  return "暂无可邀请好友";
});
const inviteEmptyText = computed(() => {
  if (inviteLoading.value) return "稍等一下，马上就好。";
  if (inviteLoadFailed.value) return "请稍后重试。";
  return "你的好友已经都在这个搭子圈里了。";
});

useShareAppMessage(() => {
  const circle = detail.value?.circle;
  const inviterId = authStore.user?.id || "";
  const sharedCircleId = circle?.id || circleId.value;

  return {
    title: circle
      ? `邀请你加入「${circle.name}」搭子圈`
      : "邀请你加入 meow食堂搭子圈",
    path: resolveSharePath({
      name: "circle-share-invite",
      params: { circleId: sharedCircleId, inviterId },
    }),
  };
});

onMounted(async () => {
  if (!(await requireAuth("circle-detail"))) return;
  await loadData();
  void centerSelectedCategory();
});

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value))
    activeCategory.value = "全部";
  void centerSelectedCategory();
});

async function loadData() {
  try {
    const [{ data: circleList }] = await Promise.all([
      socialService.getCircles(),
      loadDetail(circleId.value),
    ]);
    circles.value = circleList;
  } finally {
    if (!circles.value.length) isLoading.value = false;
  }
}

async function loadDetail(targetCircleId: string) {
  if (!targetCircleId) {
    detail.value = null;
    isLoading.value = false;
    return;
  }
  const requestToken = ++detailRequestToken;
  isLoading.value = true;
  try {
    const { data } = await socialService.getCircleDetail(targetCircleId);
    if (requestToken !== detailRequestToken) return;
    detail.value = data;
  } finally {
    if (requestToken === detailRequestToken) isLoading.value = false;
  }
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || "?")
    .trim()
    .slice(0, 1)
    .toUpperCase();
}

function openInvitePicker() {
  if (!detail.value) return;
  inviteModalVisible.value = true;
  void ensureInviteCandidates();
}

function closeInvitePicker() {
  if (inviteSubmitting.value) return;
  inviteModalVisible.value = false;
  selectedFriendId.value = "";
}

async function ensureInviteCandidates() {
  if (friends.value.length) {
    inviteLoadFailed.value = false;
    syncSelectedFriend();
    return;
  }
  inviteLoading.value = true;
  inviteLoadFailed.value = false;
  try {
    const { data } = await socialService.getFriends();
    friends.value = data;
    syncSelectedFriend();
  } catch (error: any) {
    inviteLoadFailed.value = true;
    Message.error(error?.response?.data?.message || "加载好友列表失败");
  } finally {
    inviteLoading.value = false;
  }
}

function syncSelectedFriend() {
  if (
    inviteCandidates.value.some(
      (friend) => friend.id === selectedFriendId.value,
    )
  )
    return;
  selectedFriendId.value = inviteCandidates.value[0]?.id || "";
}

async function submitInvite() {
  if (!activeCircleId.value || !selectedFriendId.value) return;
  inviteSubmitting.value = true;
  try {
    const { data } = await socialService.inviteToCircle(activeCircleId.value, {
      inviteeUserId: selectedFriendId.value,
    });
    detail.value = data;
    Message.success("邀请已发送");
    closeInvitePicker();
  } catch (error: any) {
    Message.error(error?.response?.data?.message || "邀请失败");
  } finally {
    inviteSubmitting.value = false;
  }
}

function openMembers() {
  if (!detail.value) return;
  push({ name: "circle-members", params: { id: activeCircleId.value } });
}

function openDish(id: DishSummary["id"]) {
  push({ name: "dish-detail", params: { id } });
}

function selectCategory(category: string) {
  activeCategory.value = category;
  void centerSelectedCategory();
}

function switchCircle(id: string) {
  if (!id || id === activeCircleId.value) return;
  closeInvitePicker();
  push({ name: "circle-detail", params: { id } });
}

async function centerSelectedCategory() {
  await nextTick();

  const [stripRect, activeRect, contentRect] = await new Promise<
    [
      Taro.NodesRef.BoundingClientRectCallbackResult | null,
      Taro.NodesRef.BoundingClientRectCallbackResult | null,
      Taro.NodesRef.BoundingClientRectCallbackResult | null,
    ]
  >((resolve) => {
    Taro.createSelectorQuery()
      .select(".category-strip")
      .boundingClientRect()
      .select(".category-pill.active")
      .boundingClientRect()
      .select(".category-strip-content")
      .boundingClientRect()
      .exec((result) => {
        resolve([
          (result?.[0] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ??
            null,
          (result?.[1] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ??
            null,
          (result?.[2] as Taro.NodesRef.BoundingClientRectCallbackResult | null) ??
            null,
        ]);
      });
  });

  if (!stripRect || !activeRect || !contentRect) return;

  const nextScrollLeft =
    categoryScrollLeft.value +
    (activeRect.left - stripRect.left) -
    (stripRect.width - activeRect.width) / 2;
  const maxScrollLeft = Math.max(contentRect.width - stripRect.width, 0);

  categoryScrollLeft.value = Math.min(
    Math.max(nextScrollLeft, 0),
    maxScrollLeft,
  );
}

function avatarInitial(name: string) {
  return (name || "?").trim().slice(0, 1).toUpperCase();
}

function inviteAvatarToneClass(index: number) {
  return ["tone-sage", "tone-apricot", "tone-lavender"][index % 3];
}

function inviteFriendMeta(friend: FriendItem, index: number) {
  if (friend.bio?.trim()) return friend.bio.trim();
  const fallback = [
    `好友可见 ${friend.visibleMenuCount} 份菜单`,
    `共同收藏 ${Math.max(friend.sharedMenuCount, 1)} 道菜`,
    `一起吃过 ${Math.max(friend.sharedMenuCount, 1)} 次`,
  ];
  return fallback[index % fallback.length];
}
</script>

<style>
.circle-detail-page {
  padding: 0 0 28px;
  background: #f7f6f3;
}

.circle-nav {
  height: 54px;
  padding: 14px 20px 12px;
}

.circle-nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.share-shell {
  color: #1b3a2d;
}

.page-title,
.strong,
.section-title,
.recipe-name {
  color: #151515;
  font-weight: 800;
}

.page-title {
  font-size: 16px;
}

.muted,
.eyebrow {
  color: #787774;
  font-size: 12px;
}

.section-block {
  margin: 16px 20px 0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.section-head,
.member-entry,
.member-avatars {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head view,
.recent-copy {
  display: flex;
  flex-direction: column;
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
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-right: -8px;
  border: 2px solid #fff;
  border-radius: 999px;
  font-weight: 800;
}

.category-strip {
  width: 100%;
  padding: 0 2px 8px;
  overflow: hidden;
  white-space: nowrap;
  margin-bottom: 18px;
}

.category-strip-content {
  display: inline-flex;
  width: max-content;
  padding-right: 8px;
}

.category-pill {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  border-radius: 999px;
  background: #fff;
  padding: 10px 16px;
  font-size: var(--text-sm);
  font-weight: 700;
}

.category-pill.active {
  background: #1b3a2d;
  color: #fff;
}

.recent-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.recent-card {
  position: relative;
  height: 128px;
  overflow: hidden;
  border-radius: 18px;
  background: #ddd;
  text-align: left;
}

.recent-image {
  width: 100%;
  height: 100%;
}

.recent-copy {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  gap: 2px;
  padding: 32px 10px 10px;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.62));
}

.recent-name {
  color: #fff;
  font-size: 13px;
  font-weight: 800;
}

.recent-category {
  color: rgba(255, 255, 255, 0.78);
  font-size: 11px;
}

.recent-owner {
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0 20px;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.overview-head,
.circle-switch-list,
.invite-modal-head,
.invite-friend-main,
.invite-modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overview-copy,
.invite-friend-copy,
.invite-modal-footer {
  display: flex;
  flex-direction: column;
}

.circle-title {
  color: #151515;
  font-size: 22px;
  font-weight: 800;
}

.state-pill {
  border-radius: 999px;
  background: #edf3ec;
  color: #346538;
  padding: 5px 9px;
  font-size: 11px;
  font-weight: 800;
}

.circle-switch-list {
  white-space: nowrap;
}

.circle-switch-row {
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
  margin-right: 8px;
  border-radius: 12px;
  background: #f5f2ed;
  padding: 8px 10px;
  text-align: left;
}

.circle-switch-row.active {
  background: #1b3a2d;
  color: #fff;
}

.invite-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: flex-end;
  background: rgba(0, 0, 0, 0.35);
}

.invite-modal-card {
  width: 100%;
  border-radius: 24px 24px 0 0;
  background: #fff;
  padding: 12px 20px 24px;
}

.invite-modal-handle {
  width: 42px;
  height: 4px;
  margin: 0 auto 14px;
  border-radius: 999px;
  background: #ded8d0;
}

.modal-close-shell {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 22px;
}

.invite-friends-card,
.invite-empty-card {
  margin-top: 14px;
  border-radius: 16px;
  background: #f8f5f0;
  overflow: hidden;
}

.invite-empty-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px;
}

.invite-friend-main {
  width: 100%;
  justify-content: flex-start;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid #ebe4dc;
  text-align: left;
}

.invite-avatar-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  font-weight: 800;
}

.tone-sage {
  background: #edf3ec;
  color: #346538;
}

.tone-apricot {
  background: #f9ebdd;
  color: #9f5c38;
}

.tone-lavender {
  background: #eeeaf7;
  color: #6c58a5;
}

.invite-friend-copy {
  flex: 1;
}

.invite-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid #d8d0c6;
}

.invite-check.active {
  border-color: #9f5c38;
  background: #9f5c38;
  color: #fff;
}

.invite-modal-footer {
  gap: 12px;
  margin-top: 16px;
}

.invite-modal-actions {
  gap: 10px;
}

.modal-action {
  flex: 1;
  min-height: 44px;
  border-radius: 14px;
  font-weight: 800;
}

.modal-action.ghost {
  background: #f5efe7;
  color: #151515;
}

.modal-action.primary {
  background: #9f5c38;
  color: #fff;
}
</style>
