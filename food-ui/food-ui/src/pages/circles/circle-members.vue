<template>
  <div class="page-shell circle-members-page">
    <div class="circle-members-content">
      <section class="circle-members-top-card">
        <button class="nav-shell" type="button" aria-label="返回" @click="goBack">‹</button>
        <div class="circle-members-title-stack">
          <small>搭子圈成员</small>
          <h1>圈内成员</h1>
        </div>
        <span class="nav-shell nav-shell-placeholder" aria-hidden="true"></span>
      </section>

      <section v-if="detail" class="circle-members-intro-card">
        <h2>{{ detail.circle.name }}</h2>
        <p>
          {{ detail.stats.memberCount }} 位成员 · {{ detail.stats.sharedMenuCount }} 份共享菜谱 · 发起人
          {{ detail.circle.ownerNickname }}
        </p>
        <p class="circle-members-intro-desc">
          {{ detail.circle.description || '只显示当前搭子圈里的成员，点击可查看对方菜单。' }}
        </p>
      </section>

      <section class="circle-members-list-section">
        <div class="circle-members-list-head">
          <div>
            <h3>成员列表</h3>
          </div>
          <span v-if="detail" class="circle-members-count">{{ detail.stats.memberCount }} 人</span>
        </div>

        <div v-if="detail?.members.length" class="circle-members-list-card">
          <article
            v-for="(member, index) in orderedMembers"
            :key="member.id"
            class="circle-member-row"
          >
            <button class="circle-member-main" type="button" @click="openMember(member.id)">
              <div :class="['member-avatar-box', avatarToneClass(index)]">
                <span>{{ avatarInitial(member.nickname || member.account) }}</span>
              </div>
              <div class="circle-member-copy">
                <div class="circle-member-name-row">
                  <strong>{{ member.nickname }}</strong>
                  <span :class="['role-pill', member.role === 'owner' ? 'role-pill-owner' : 'role-pill-member']">
                    {{ member.role === 'owner' ? '发起人' : '成员' }}
                  </span>
                </div>
                <p>{{ memberMeta(member, index) }}</p>
              </div>
            </button>
          </article>
        </div>

        <section v-else-if="!isLoading" class="circle-members-empty-card">
          <p>等有人加入后，就会出现在这里。</p>
        </section>

        <section v-else class="circle-members-empty-card">
          <strong>正在加载成员</strong>
        </section>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { SocialService, type BuddyCircleDetail, type BuddyCircleMember } from '@/services/social-service'

const route = useRoute()
const router = useRouter()
const socialService = new SocialService()

const detail = ref<BuddyCircleDetail | null>(null)
const isLoading = ref(true)

const avatarTones = [
  { box: 'tone-sage' },
  { box: 'tone-apricot' },
  { box: 'tone-lavender' },
]

const circleId = computed(() => String(route.params.id || ''))

const orderedMembers = computed(() => {
  const members = detail.value?.members || []
  return [...members].sort((left, right) => {
    if (left.role === right.role) return 0
    return left.role === 'owner' ? -1 : 1
  })
})

onMounted(loadCircleDetail)

async function loadCircleDetail() {
  if (!circleId.value) {
    isLoading.value = false
    return
  }

  isLoading.value = true
  try {
    const { data } = await socialService.getCircleDetail(circleId.value)
    detail.value = data
  } catch (error: any) {
    Message.error(error?.response?.data?.message || '加载圈成员失败')
  } finally {
    isLoading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'circle-detail', params: { id: circleId.value } })
}

function openMember(userId: string) {
  router.push({ name: 'user-menu', params: { id: userId } })
}

function avatarInitial(name: string) {
  const text = (name || '?').trim()
  return text.slice(0, 1).toUpperCase()
}

function avatarToneClass(index: number) {
  return avatarTones[index % avatarTones.length].box
}

function memberMeta(member: BuddyCircleMember, index: number) {
  if (member.role === 'owner') {
    return `发起了这个搭子圈 · 共享 ${member.sharedMenuCount} 份菜单`
  }

  const fallback = [
    `共享了 ${member.sharedMenuCount} 份菜单`,
    `通过搭子圈可见 ${Math.max(member.sharedMenuCount, 1)} 份内容`,
    `圈内一起收藏了不少想吃的菜`,
  ]

  return fallback[index % fallback.length]
}
</script>

<style scoped>
.circle-members-page {
  min-height: 100vh;
  background: #f7f6f3;
}

.circle-members-top-card,
.circle-members-list-head,
.circle-member-main,
.circle-member-name-row {
  display: flex;
}

.circle-members-top-card,
.circle-members-list-head,
.circle-member-name-row {
  justify-content: space-between;
}

.circle-members-top-card,
.circle-member-main,
.circle-member-name-row {
  align-items: center;
}

.circle-members-content {
  padding: 10px 20px 28px;
}

.circle-members-top-card,
.circle-members-intro-card,
.circle-members-list-card,
.circle-members-empty-card {
  background: #ffffff;
}

.circle-members-top-card {
  padding: 16px;
  border-radius: 12px;
}

.nav-shell {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  background: #f5efe7;
  color: #151515;
  font-size: 24px;
  line-height: 1;
}

.nav-shell-placeholder {
  visibility: hidden;
}

.circle-members-title-stack {
  text-align: center;
}

.circle-members-title-stack small {
  display: block;
  margin-bottom: 3px;
  font-size: 11px;
  font-weight: 600;
  color: #9f5c38;
}

.circle-members-title-stack h1,
.circle-members-intro-card h2,
.circle-members-list-head h3 {
  margin: 0;
  color: #151515;
}

.circle-members-title-stack h1 {
  font-size: 16px;
  font-weight: 600;
}

.circle-members-intro-card {
  margin-top: 16px;
  border-radius: 14px;
  padding: 16px;
}

.circle-members-intro-card h2 {
  font-family: var(--font-serif);
  font-size: 22px;
  font-weight: 600;
}

.circle-members-intro-card p {
  margin: 0;
  color: #787774;
  font-size: 12px;
}

.circle-members-intro-card > p + p {
  margin-top: 8px;
}

.circle-members-intro-desc {
  line-height: 1.5;
  color: #2f3437;
}

.circle-members-list-section {
  margin-top: 24px;
}

.circle-members-list-head {
  margin-bottom: 12px;
}

.circle-members-list-head h3 {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 600;
}

.circle-members-list-head p,
.circle-member-copy p,
.circle-members-empty-card p {
  margin: 0;
  color: #787774;
}

.circle-members-list-head p {
  margin-top: 4px;
  font-size: 11px;
}

.circle-members-count {
  color: #9f5c38;
  font-size: 12px;
  font-weight: 600;
}

.circle-members-list-card {
  border-radius: 16px;
  padding: 18px;
}

.circle-member-row + .circle-member-row {
  border-top: 1px solid #ece5dc;
}

.circle-member-main {
  width: 100%;
  gap: 16px;
  min-height: 86px;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
}

.circle-member-row + .circle-member-row .circle-member-main {
  padding-top: 16px;
}

.circle-member-row:not(:last-child) .circle-member-main {
  padding-bottom: 16px;
}

.member-avatar-box {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.member-avatar-box span {
  font-size: 18px;
  font-weight: 600;
}

.tone-sage {
  background: #edf3ec;
}

.tone-sage span {
  color: #346538;
}

.tone-apricot {
  background: #f9ebdd;
}

.tone-apricot span {
  color: #9f5c38;
}

.tone-lavender {
  background: #eeeaf7;
}

.tone-lavender span {
  color: #6c58a5;
}

.circle-member-copy {
  flex: 1;
}

.circle-member-name-row strong,
.circle-members-empty-card strong {
  color: #151515;
}

.circle-member-name-row strong {
  font-size: 15px;
  font-weight: 600;
}

.circle-member-copy p {
  margin-top: 6px;
  font-size: 12px;
}

.role-pill {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.role-pill-owner {
  background: #f9ebdd;
  color: #9f5c38;
}

.role-pill-member {
  background: #edf3ec;
  color: #346538;
}

.circle-members-empty-card {
  border-radius: 16px;
  padding: 18px;
}

.circle-members-empty-card strong {
  display: block;
  margin-bottom: 6px;
}

@media (max-width: 420px) {
  .circle-members-content {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
