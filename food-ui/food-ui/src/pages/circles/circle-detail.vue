<template>
  <div class="page-shell detail-page">
    <header class="top-nav">
      <button class="icon-shell" type="button" aria-label="返回" @click="goBack">
        <span class="icon-back">‹</span>
      </button>
      <h1>美食搭子</h1>
      <button class="icon-shell" type="button" aria-label="邀请好友" @click="openInvitePicker">
        <span class="icon-plus">＋</span>
      </button>
    </header>

    <template v-if="detail">
      <section class="overview-card">
        <div class="overview-head">
          <div class="overview-copy">
            <span class="eyebrow">当前圈子</span>
            <h2>{{ detail.circle.name }}</h2>
          </div>
          <span class="state-pill">{{ detail.stats.memberCount }}人 · {{ detail.stats.sharedMenuCount }}菜谱</span>
        </div>

        <div v-if="switcherCircles.length" class="circle-switch-list" aria-label="切换搭子圈">
          <button
            v-for="circle in switcherCircles"
            :key="circle.id"
            class="circle-switch-row"
            :class="{ active: circle.id === activeCircleId }"
            :title="circle.name"
            type="button"
            @click="switchCircle(circle.id)"
          >
            <strong>{{ circle.name }}</strong>
            <small>{{ circle.memberCount }}人 · {{ circle.sharedMenuCount }}菜谱</small>
          </button>
        </div>
      </section>

      <section class="section-block">
        <div class="section-head">
          <div>
            <h3>搭子圈成员</h3>
            <p>点击进入成员页，查看圈成员关系</p>
          </div>
          <span class="section-link">共 {{ detail.stats.memberCount }} 人</span>
        </div>

        <button class="member-entry" type="button" @click="openMembers">
          <div class="member-avatars">
            <div
              v-for="member in previewMembers"
              :key="member.id"
              class="avatar-badge"
              :style="{ background: avatarPalette[member.avatarTone].bg, color: avatarPalette[member.avatarTone].fg }"
            >
              {{ member.initial }}
            </div>
          </div>
          <span class="member-entry-arrow">›</span>
        </button>
      </section>

      <section class="section-block recipes-section">
        <div class="section-head">
          <div>
            <h3>圈内菜谱</h3>
            <p>按分类快速看圈内最近共享的菜</p>
          </div>
          <span class="section-link">最近更新</span>
        </div>

        <div class="category-group">
          <button
            v-for="category in categories"
            :key="category"
            class="category-chip"
            :class="{ active: category === activeCategory }"
            type="button"
            @click="activeCategory = category"
          >
            {{ category }}
          </button>
        </div>

        <div v-if="visibleMenus.length" class="recipe-grid">
          <article
            v-for="menu in visibleMenus"
            :key="menu.id"
            class="recipe-card"
            @click="openDish(menu.id)"
          >
            <img class="recipe-image" :src="menu.image" :alt="menu.name" />
            <div class="recipe-copy">
              <h4>{{ menu.name }}</h4>
              <p class="recipe-category">{{ menu.categoryName }}</p>
              <p class="recipe-owner">{{ menu.ownerNickname }}创建</p>
            </div>
          </article>
        </div>

        <div v-else class="empty-card">
          <p>这个分类还没有共享菜谱</p>
        </div>
      </section>
    </template>

    <section v-else class="status-card">
      <p>{{ statusText }}</p>
    </section>

    <div
      v-if="inviteModalVisible"
      class="invite-modal-overlay"
      @click.self="closeInvitePicker"
    >
      <section class="invite-modal-card">
        <div class="invite-modal-handle"></div>

        <div class="invite-modal-head">
          <div class="invite-modal-title">
            <small>搭子圈邀请</small>
          </div>
          <button class="modal-close-shell" type="button" :disabled="inviteSubmitting" @click="closeInvitePicker">×</button>
        </div>

        <div v-if="inviteCandidates.length" class="invite-friends-card">
          <article
            v-for="(friend, index) in inviteCandidates"
            :key="friend.id"
            class="invite-friend-row"
          >
            <button
              class="invite-friend-main"
              type="button"
              :disabled="inviteSubmitting"
              @click="selectedFriendId = friend.id"
            >
              <div :class="['invite-avatar-box', inviteAvatarToneClass(index)]">
                <span>{{ avatarInitial(friend.nickname) }}</span>
              </div>
              <div class="invite-friend-copy">
                <strong>{{ friend.nickname }}</strong>
                <p>{{ inviteFriendMeta(friend, index) }}</p>
              </div>
              <span :class="['invite-check', { active: selectedFriendId === friend.id }]">
                {{ selectedFriendId === friend.id ? '✓' : '' }}
              </span>
            </button>
          </article>
        </div>

        <div v-else class="invite-empty-card">
          <strong>{{ inviteEmptyTitle }}</strong>
          <p>{{ inviteEmptyText }}</p>
        </div>

        <div class="invite-modal-footer">
          <p>一次选择 1 位好友发出邀请。</p>
          <div class="invite-modal-actions">
            <button class="modal-action ghost" type="button" :disabled="inviteSubmitting" @click="closeInvitePicker">
              取消
            </button>
            <button
              class="modal-action primary"
              type="button"
              :disabled="!selectedFriendId || inviteSubmitting || inviteLoading"
              @click="submitInvite"
            >
              {{ inviteSubmitting ? '邀请中...' : inviteButtonText }}
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import type { DishSummary } from '@/services/food-service'
import {
  SocialService,
  type BuddyCircleDetail,
  type BuddyCircleMember,
  type BuddyCircleSummary,
  type FriendItem,
} from '@/services/social-service'

type PreviewMember = {
  id: string
  initial: string
  avatarTone: number
}

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()

const detail = ref<BuddyCircleDetail | null>(null)
const circles = ref<BuddyCircleSummary[]>([])
const activeCategory = ref('全部')
const isLoading = ref(true)
const inviteModalVisible = ref(false)
const inviteLoading = ref(false)
const inviteSubmitting = ref(false)
const inviteLoadFailed = ref(false)
const friends = ref<FriendItem[]>([])
const selectedFriendId = ref('')
let detailRequestToken = 0

const avatarPalette = [
  { bg: '#edf3ec', fg: '#346538' },
  { bg: '#f9ebdd', fg: '#9f5c38' },
  { bg: '#eeeaf7', fg: '#6c58a5' },
]

const circleId = computed(() => String(route.params.id || ''))
const activeCircleId = computed(() => detail.value?.circle.id || circleId.value)

const switcherCircles = computed(() => {
  if (circles.value.length) return circles.value
  return detail.value ? [detail.value.circle] : []
})

const categories = computed(() => {
  const names = Array.from(new Set((detail.value?.sharedMenus || []).map((menu) => menu.categoryName).filter(Boolean)))
  return ['全部', ...names]
})

const visibleMenus = computed(() => {
  const menus = detail.value?.sharedMenus || []
  if (activeCategory.value === '全部') return menus.slice(0, 4)
  return menus.filter((menu) => menu.categoryName === activeCategory.value).slice(0, 4)
})

const previewMembers = computed<PreviewMember[]>(() =>
  (detail.value?.members || []).slice(0, 3).map((member, index) => ({
    id: member.id,
    initial: getInitial(member),
    avatarTone: index % avatarPalette.length,
  })),
)

const statusText = computed(() => (isLoading.value ? '正在加载搭子圈…' : '没有找到这个搭子圈'))
const circleMemberIds = computed(() => new Set((detail.value?.members || []).map((member) => member.id)))
const inviteCandidates = computed(() => friends.value.filter((friend) => !circleMemberIds.value.has(friend.id)))
const inviteButtonText = computed(() => {
  const target = inviteCandidates.value.find((friend) => friend.id === selectedFriendId.value)
  return target ? `邀请${target.nickname}加入搭子圈` : '选择好友后邀请'
})
const inviteEmptyTitle = computed(() => {
  if (inviteLoading.value) return '正在加载好友'
  if (inviteLoadFailed.value) return '好友加载失败'
  return '暂无可邀请好友'
})
const inviteEmptyText = computed(() => {
  if (inviteLoading.value) return '稍等一下，马上就好。'
  if (inviteLoadFailed.value) return '请稍后重试。'
  return '你的好友已经都在这个搭子圈里了。'
})

onMounted(loadData)

watch(circleId, (nextId, prevId) => {
  if (!nextId || nextId === prevId) return
  void loadDetail(nextId)
})

watch(categories, (nextCategories) => {
  if (!nextCategories.includes(activeCategory.value)) {
    activeCategory.value = '全部'
  }
})

async function loadData() {
  try {
    const [{ data: circleList }] = await Promise.all([
      socialService.getCircles(),
      loadDetail(circleId.value),
    ])
    circles.value = circleList
  } finally {
    if (!circles.value.length) {
      isLoading.value = false
    }
  }
}

async function loadDetail(targetCircleId: string) {
  if (!targetCircleId) {
    detail.value = null
    isLoading.value = false
    return
  }

  const requestToken = ++detailRequestToken
  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(targetCircleId)
    if (requestToken !== detailRequestToken) return
    detail.value = data
  } finally {
    if (requestToken !== detailRequestToken) return
    isLoading.value = false
  }
}

function getInitial(member: BuddyCircleMember) {
  return (member.nickname || member.account || '?').trim().slice(0, 1).toUpperCase()
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'circles' })
}

function openInvitePicker() {
  if (!detail.value) return
  inviteModalVisible.value = true
  void ensureInviteCandidates()
}

function closeInvitePicker() {
  if (inviteSubmitting.value) return
  inviteModalVisible.value = false
  selectedFriendId.value = ''
}

async function ensureInviteCandidates() {
  if (friends.value.length) {
    inviteLoadFailed.value = false
    syncSelectedFriend()
    return
  }

  inviteLoading.value = true
  inviteLoadFailed.value = false
  try {
    const { data } = await socialService.getFriends()
    friends.value = data
    syncSelectedFriend()
  } catch (error: any) {
    inviteLoadFailed.value = true
    Message.error(error?.response?.data?.message || '加载好友列表失败')
  } finally {
    inviteLoading.value = false
  }
}

function syncSelectedFriend() {
  if (inviteCandidates.value.some((friend) => friend.id === selectedFriendId.value)) return
  selectedFriendId.value = inviteCandidates.value[0]?.id || ''
}

async function submitInvite() {
  if (!activeCircleId.value || !selectedFriendId.value) return

  inviteSubmitting.value = true
  try {
    const { data } = await socialService.inviteToCircle(activeCircleId.value, {
      inviteeUserId: selectedFriendId.value,
    })
    detail.value = data
    Message.success('邀请已发送')
    closeInvitePicker()
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '邀请失败')
  } finally {
    inviteSubmitting.value = false
  }
}

function openMembers() {
  if (!detail.value) return
  router.push({
    name: 'circle-members',
    params: { id: activeCircleId.value },
  })
}

function openDish(id: DishSummary['id']) {
  router.push({ name: 'dish-detail', params: { id } })
}

function switchCircle(id: string) {
  if (!id || id === activeCircleId.value) return
  closeInvitePicker()
  router.push({ name: 'circle-detail', params: { id } })
}

function avatarInitial(name: string) {
  const text = (name || '?').trim()
  return text.slice(0, 1).toUpperCase()
}

function inviteAvatarToneClass(index: number) {
  return ['tone-sage', 'tone-apricot', 'tone-lavender'][index % 3]
}

function inviteFriendMeta(friend: FriendItem, index: number) {
  if (friend.bio?.trim()) return friend.bio.trim()

  const fallback = [
    `好友可见 ${friend.visibleMenuCount} 份菜单`,
    `共同收藏 ${Math.max(friend.sharedMenuCount, 1)} 道菜`,
    `一起吃过 ${Math.max(friend.sharedMenuCount, 1)} 次`,
  ]
  return fallback[index % fallback.length]
}
</script>

<style scoped>
.detail-page {
  position: relative;
  min-height: 100vh;
  padding: 0 0 28px;
  background: #f7f6f3;
}

.top-nav,
.overview-head,
.section-head,
.circle-switch-list,
.member-entry,
.member-avatars,
.recipe-grid,
.invite-modal-head,
.invite-friend-main,
.invite-modal-actions {
  display: flex;
}

.top-nav,
.overview-head,
.section-head,
.member-entry,
.invite-modal-head {
  align-items: center;
  justify-content: space-between;
}

.top-nav {
  padding: 14px 20px 12px;
}

.icon-shell {
  cursor: pointer;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #151515;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 14px rgba(27, 58, 45, 0.05);
}

.icon-back,
.icon-plus,
.member-entry-arrow {
  line-height: 1;
}

.icon-back {
  font-size: 1.3rem;
}

.icon-plus {
  font-size: 1.15rem;
}

h1,
h2,
h3,
h4,
p {
  margin: 0;
}

h1 {
  color: #151515;
  font-size: 1rem;
  font-weight: 600;
}

.overview-card,
.status-card {
  margin: 0 20px;
  background: #fff;
  border-radius: 14px;
  padding: 14px;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.overview-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.eyebrow,
.section-link,
.recipe-owner {
  color: #9f5c38;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

h2,
h3 {
  color: #151515;
  font-family: var(--font-serif);
  font-weight: 600;
}

h2 {
  font-size: 1.125rem;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

h3 {
  font-size: 1.18rem;
  line-height: 1.25;
}

.state-pill {
  background: #edf3ec;
  color: #346538;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.6875rem;
  font-weight: 600;
  white-space: nowrap;
}

.circle-switch-list {
  flex-direction: column;
  gap: 6px;
}

.circle-switch-row {
  cursor: pointer;
  width: 100%;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: none;
  border-radius: 10px;
  background: #f8f3ec;
  padding: 0 10px;
  text-align: left;
}

.circle-switch-row strong {
  min-width: 0;
  overflow: hidden;
  color: #151515;
  font-size: 0.8125rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.circle-switch-row small {
  flex: none;
  color: #9f5c38;
  font-size: 0.6875rem;
  font-weight: 600;
  white-space: nowrap;
}

.circle-switch-row.active {
  background: #1b3a2d;
}

.circle-switch-row.active strong {
  color: #fff;
}

.circle-switch-row.active small {
  color: #dde8dd;
}

.section-head p,
.recipe-category,
.status-card p {
  color: #8b8b8b;
  font-size: 0.75rem;
}

.section-head p {
  margin-top: 4px;
}

.section-block {
  margin: 16px 20px 0;
}

.section-head {
  align-items: flex-end;
  gap: 12px;
}

.member-entry {
  cursor: pointer;
  width: 100%;
  margin-top: 12px;
  padding: 16px;
  border: none;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 10px 22px rgba(27, 58, 45, 0.07);
}

.member-avatars {
  align-items: center;
  gap: 8px;
}

.avatar-badge {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 0.9375rem;
  font-weight: 600;
}

.member-entry-arrow {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #f5efe7;
  color: #9f5c38;
  display: grid;
  place-items: center;
  font-size: 1rem;
}

.recipes-section {
  padding-bottom: 4px;
}

.category-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.category-chip {
  cursor: pointer;
  border: none;
  border-radius: 999px;
  background: #fff;
  color: #4d4d4d;
  padding: 8px 16px;
  font-size: 0.75rem;
  font-weight: 500;
}

.category-chip.active {
  background: #1b3a2d;
  color: #fff;
}

.recipe-grid {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.recipe-card,
.empty-card {
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 10px 22px rgba(27, 58, 45, 0.07);
}

.recipe-card {
  width: calc((100% - 12px) / 2);
}

.recipe-image {
  width: 100%;
  height: 118px;
  object-fit: cover;
  display: block;
}

.recipe-copy {
  padding: 10px;
}

h4 {
  color: #1b3a2d;
  font-size: 0.9375rem;
  font-weight: 600;
  line-height: 1.3;
}

.recipe-category,
.recipe-owner {
  margin-top: 5px;
}

.empty-card,
.status-card {
  display: grid;
  place-items: center;
  min-height: 120px;
}

.invite-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  background: rgba(21, 21, 21, 0.4);
}

.invite-modal-card,
.invite-friends-card,
.invite-empty-card {
  background: #fff;
}

.invite-modal-card {
  width: 100%;
  border-radius: 24px 24px 0 0;
  padding: 20px 20px 24px;
}

.invite-modal-handle {
  width: 48px;
  height: 4px;
  margin: 0 auto 16px;
  border-radius: 999px;
  background: #ddd2c7;
}

.invite-modal-head {
  align-items: flex-start;
  gap: 16px;
}

.invite-modal-title small {
  display: block;
  margin-bottom: 4px;
  color: #9f5c38;
  font-size: 0.6875rem;
  font-weight: 600;
}

.invite-modal-title h2 {
  margin: 0;
  color: #151515;
  font-family: var(--font-serif);
  font-size: 1.5rem;
  font-weight: 600;
  line-height: 1.2;
}

.invite-modal-title p,
.invite-modal-footer p,
.invite-friend-copy p,
.invite-empty-card p {
  margin: 0;
  color: #787774;
  font-size: 0.75rem;
}

.invite-modal-title p {
  margin-top: 6px;
  line-height: 1.5;
}

.modal-close-shell,
.modal-action {
  cursor: pointer;
  border: none;
  border-radius: 999px;
}

.modal-close-shell {
  width: 36px;
  height: 36px;
  flex: none;
  background: #f5efe7;
  color: #151515;
  font-size: 1.125rem;
  line-height: 1;
}

.invite-friends-card {
  margin-top: 16px;
  border-radius: 18px;
  padding: 6px 0;
}

.invite-friend-row + .invite-friend-row {
  border-top: 1px solid #f1ece6;
}

.invite-friend-main {
  width: 100%;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: none;
  background: transparent;
  text-align: left;
}

.invite-avatar-box {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 0.9375rem;
  font-weight: 600;
  flex: none;
}

.invite-friend-copy {
  flex: 1;
  min-width: 0;
}

.invite-friend-copy strong,
.invite-empty-card strong {
  display: block;
  color: #151515;
  font-size: 0.875rem;
  font-weight: 600;
}

.invite-friend-copy p,
.invite-empty-card p {
  margin-top: 4px;
}

.invite-check {
  width: 24px;
  height: 24px;
  border: 1px solid #ddd2c7;
  border-radius: 999px;
  color: transparent;
  display: grid;
  place-items: center;
  flex: none;
  font-size: 0.75rem;
  font-weight: 600;
}

.invite-check.active {
  border-color: #1b3a2d;
  background: #1b3a2d;
  color: #fff;
}

.invite-empty-card {
  margin-top: 16px;
  border-radius: 18px;
  padding: 24px 18px;
  text-align: center;
}

.invite-modal-footer {
  margin-top: 16px;
}

.invite-modal-actions {
  gap: 12px;
  margin-top: 12px;
}

.modal-action {
  flex: 1;
  padding: 12px 16px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.modal-action.ghost {
  background: #f5efe7;
  color: #6a625a;
}

.modal-action.primary {
  background: #9f5c38;
  color: #fff;
}

.modal-close-shell:disabled,
.invite-friend-main:disabled,
.modal-action:disabled {
  cursor: not-allowed;
  opacity: 0.6;
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

@media (max-width: 360px) {
  .recipe-card {
    width: 100%;
  }

  .recipe-grid {
    flex-direction: column;
  }

  .invite-modal-overlay {
    padding-top: 40px;
  }

  .invite-modal-head,
  .invite-modal-actions {
    gap: 10px;
  }
}
</style>
