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
      pages: [
        'profile',
        'edit-profile',
        'edit-avatar',
        'notifications',
        'feedback',
        'friends',
        'friend-requests',
        'friend-invite',
        'vip',
        'pet-adoption',
        'pet-detail',
      ],
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
    {
      root: 'pages/knowledge',
      pages: ['archive', 'detail'],
    },
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#f5f2ed',
    navigationBarTitleText: 'meoi食堂',
    navigationBarTextStyle: 'black',
  },
})
