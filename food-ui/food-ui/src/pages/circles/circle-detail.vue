<template>
  <div class="page-shell detail-page">
    <header class="top-nav">
      <button class="back" type="button" @click="router.back()">‹</button>
      <h1>美食搭子</h1>
      <button class="back" type="button" @click="openInvitePicker">＋</button>
    </header>

    <section class="overview-card">
      <small>搭子小圈</small>
      <h2>{{ detail?.circle.name }}</h2>
      <p>{{ detail?.circle.description }}</p>
      <div class="stats">
        <div>
          <strong>{{ detail?.stats.memberCount ?? 0 }}</strong>
          <span>成员</span>
        </div>
        <div>
          <strong>{{ detail?.stats.sharedMenuCount ?? 0 }}</strong>
          <span>共享菜单</span>
        </div>
        <div>
          <strong>{{ detail?.stats.weeklyUpdateCount ?? 0 }}</strong>
          <span>本周更新</span>
        </div>
      </div>
      <button class="primary-button full" type="button" @click="openInvitePicker">邀请好友加入</button>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>圈内关系</small>
          <h3>圈内成员</h3>
        </div>
      </div>
      <div class="friend-grid">
        <FriendCard
          v-for="member in detail?.members || []"
          :key="member.id"
          :friend="memberToFriend(member)"
          :action-label="member.role === 'owner' ? '查看' : '在圈内'"
          @action="openUser(member.id)"
        />
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <small>共享菜单</small>
          <h3>圈内共享菜单</h3>
        </div>
      </div>
      <div class="menu-stack">
        <CircleMenuCard
          v-for="menu in detail?.sharedMenus || []"
          :key="menu.id"
          :menu="menu"
          @open="openDish(menu.id)"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FriendCard from '@/components/FriendCard.vue'
import CircleMenuCard from '@/components/CircleMenuCard.vue'
import { SocialService, type BuddyCircleDetail, type BuddyCircleMember, type FriendItem } from '@/services/social-service'

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()
const detail = ref<BuddyCircleDetail | null>(null)

const circleId = computed(() => String(route.params.id || ''))

onMounted(loadData)

async function loadData() {
  if (!circleId.value) return
  const { data } = await socialService.getCircleDetail(circleId.value)
  detail.value = data
}

function memberToFriend(member: BuddyCircleMember): FriendItem {
  return {
    id: member.id,
    account: member.account,
    nickname: member.nickname,
    avatar: member.avatar,
    bio: member.role === 'owner' ? '创建者' : '圈内成员',
    friend: true,
    visibleMenuCount: member.sharedMenuCount,
    sharedMenuCount: member.sharedMenuCount,
    memberInCircle: true,
  }
}

function openInvitePicker() {
  if (!detail.value) return
  router.push({
    name: 'friends',
    query: {
      circleId: circleId.value,
      circleName: detail.value.circle.name,
    },
  })
}

function openDish(id: string) {
  router.push({ name: 'dish-detail', params: { id } })
}

function openUser(userId: string) {
  router.push({ name: 'user-menu', params: { id: userId } })
}
</script>

<style scoped>
.detail-page {
  padding-bottom: 48px;
}

.top-nav,
.stats,
.section-head,
.friend-grid {
  display: flex;
}

.top-nav,
.section-head {
  justify-content: space-between;
  align-items: center;
}

.back {
  border: none;
  background: #fff;
  width: 36px;
  height: 36px;
  border-radius: 999px;
}

.overview-card,
.section {
  background: #fff;
  border-radius: 22px;
  padding: 18px;
  box-shadow: 0 12px 26px rgba(27, 58, 45, 0.06);
  margin-top: 16px;
}

.overview-card small,
.overview-card p,
.section small {
  color: var(--text-muted);
}

.overview-card h2,
.section h3 {
  margin: 0;
  color: var(--text-main);
}

.overview-card h2 {
  font-family: 'Playfair Display', serif;
  margin-top: 8px;
}

.stats {
  justify-content: space-between;
  margin: 16px 0;
  padding: 16px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.stats div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.full {
  width: 100%;
}

.friend-grid {
  gap: 12px;
}

.menu-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
