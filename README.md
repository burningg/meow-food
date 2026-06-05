# pigtiger-food

一个做菜分享与好友社交结合的项目，包含后端服务和跨平台前端。

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
