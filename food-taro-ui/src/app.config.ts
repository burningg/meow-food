export default defineAppConfig({
  pages: [
    'pages/loading/loading',
    'pages/auth/login',
    'pages/auth/register',
    'pages/home/home',
    'pages/dish/add-dish',
    'pages/dish/dish-detail',
  ],
  subPackages: [
    {
      root: 'pages/profile',
      pages: ['profile', 'edit-profile', 'edit-avatar', 'friends', 'friend-requests', 'friend-invite'],
    },
    {
      root: 'pages/plan',
      pages: ['plan', 'plan-detail', 'plan-shopping'],
    },
    {
      root: 'pages/user',
      pages: ['user-menu'],
    },
    {
      root: 'pages/circles',
      pages: ['circles', 'create-circle', 'circle-members', 'circle-share-invite'],
    },
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#f5f2ed',
    navigationBarTitleText: 'meow食堂',
    navigationBarTextStyle: 'black',
  },
})
