# pigtiger-food

一个做菜分享与好友社交结合的项目，包含后端服务和跨平台前端。

## Intro

这是一个围绕“做菜、分享、社交”展开的小项目，核心场景包括：

- 记录和发布自己的菜品
- 查看好友动态
- 添加好友、管理好友申请
- 创建搭子圈，按圈子分享内容
- 管理菜单可见范围与个人资料

微信小程序可直接搜索：`meoi餐厅`

## Stack

- Backend: Spring Boot, MyBatis-Plus, MySQL, JDK 17
- Frontend: Taro, Vue 3, TypeScript

## Project Structure

```text
.
├── food-service/food   # Java 后端
├── food-taro-ui        # 当前前端（Taro 跨平台）
└── image/uploads       # 本地上传图片目录
```

## Quick Start

### 1. Start Backend

```bash
cd food-service/food
./run-backend-jdk17.sh
```

如果还没有数据库，可以先参考：

```text
food-service/food/db/table.sql
```

### 2. Start Frontend

```bash
cd food-taro-ui
npm install
npm run dev:h5
```

如果要运行微信小程序版本：

```bash
npm run dev:weapp
```

## Notes

- 当前主要维护的前端目录是 `food-taro-ui`
- `food-ui` 是历史项目，通常不需要修改
- 设计稿在 `food-ui/design.pen`
- 体验线上版本时，可在微信小程序中搜索 `meoi餐厅`
