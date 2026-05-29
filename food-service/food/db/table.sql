CREATE TABLE `category` (
  `id` varchar(50) NOT NULL COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品分类表';

CREATE TABLE `user` (
  `id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `account` varchar(50) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `username` varchar(50) NOT NULL COMMENT '兼容旧用户名字段',
  `nickname` varchar(50) NOT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account` (`account`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `user_profile_settings` (
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `default_menu_visibility` varchar(20) NOT NULL DEFAULT 'friends' COMMENT '默认菜单可见范围',
  `allow_friend_feed` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许进入好友动态',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_profile_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料配置';

CREATE TABLE `dish` (
  `id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)',
  `owner_user_id` varchar(36) NOT NULL COMMENT '所属用户ID(UUID)',
  `name` varchar(100) NOT NULL COMMENT '菜品名称',
  `image` varchar(255) DEFAULT NULL COMMENT '图片URL',
  `price` decimal(10,2) DEFAULT NULL COMMENT '价格',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `category_id` varchar(50) NOT NULL COMMENT '分类ID',
  `cook_time_minutes` int DEFAULT NULL COMMENT '烹饪时间(分钟)',
  `difficulty` varchar(20) DEFAULT NULL COMMENT '难度：easy/medium/hard',
  `servings` int DEFAULT NULL COMMENT '份量',
  `visibility` varchar(20) NOT NULL DEFAULT 'inherit' COMMENT '可见范围：inherit/public/friends/private',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：0-否，1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  CONSTRAINT `fk_dish_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品表';

CREATE TABLE `dish_ingredient` (
  `id` varchar(36) NOT NULL COMMENT '食材ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)',
  `name` varchar(100) NOT NULL COMMENT '食材名称',
  `amount` varchar(100) NOT NULL COMMENT '食材用量',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_dish_ingredient_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品食材表';

CREATE TABLE `dish_step` (
  `id` varchar(36) NOT NULL COMMENT '步骤ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)',
  `step_no` int NOT NULL COMMENT '步骤序号',
  `content` varchar(1000) NOT NULL COMMENT '步骤内容',
  PRIMARY KEY (`id`),
  KEY `idx_dish_step_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品步骤表';

CREATE TABLE `friend_request` (
  `id` varchar(36) NOT NULL COMMENT '申请ID(UUID)',
  `requester_user_id` varchar(36) NOT NULL COMMENT '发起人(UUID)',
  `target_user_id` varchar(36) NOT NULL COMMENT '接收人(UUID)',
  `message` varchar(200) DEFAULT NULL COMMENT '申请备注',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_friend_request_requester` (`requester_user_id`),
  KEY `idx_friend_request_target` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友申请表';

CREATE TABLE `friend_relation` (
  `id` varchar(36) NOT NULL COMMENT '关系ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `friend_user_id` varchar(36) NOT NULL COMMENT '好友ID(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_pair` (`user_id`,`friend_user_id`),
  KEY `idx_friend_user` (`friend_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';

CREATE TABLE `buddy_circle` (
  `id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `name` varchar(100) NOT NULL COMMENT '圈名',
  `description` varchar(255) DEFAULT NULL COMMENT '圈描述',
  `owner_user_id` varchar(36) NOT NULL COMMENT '创建人(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈';

CREATE TABLE `buddy_circle_member` (
  `id` varchar(36) NOT NULL COMMENT '成员ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '角色',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_member` (`circle_id`,`user_id`),
  KEY `idx_circle_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈成员';

CREATE TABLE `buddy_circle_invite` (
  `id` varchar(36) NOT NULL COMMENT '邀请ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `inviter_user_id` varchar(36) NOT NULL COMMENT '邀请人(UUID)',
  `invitee_user_id` varchar(36) NOT NULL COMMENT '被邀请人(UUID)',
  `status` varchar(20) NOT NULL DEFAULT 'joined' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_invite_circle` (`circle_id`),
  KEY `idx_circle_invite_user` (`invitee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈邀请记录';

CREATE TABLE `activity_feed` (
  `id` varchar(36) NOT NULL COMMENT '动态ID(UUID)',
  `actor_user_id` varchar(36) NOT NULL COMMENT '动态用户(UUID)',
  `dish_id` varchar(36) DEFAULT NULL COMMENT '关联菜品ID(UUID)',
  `circle_id` varchar(36) DEFAULT NULL COMMENT '关联圈子ID(UUID)',
  `activity_type` varchar(20) NOT NULL COMMENT '动态类型',
  `visibility_scope` varchar(20) NOT NULL COMMENT '动态可见性',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_actor` (`actor_user_id`),
  KEY `idx_activity_circle` (`circle_id`),
  KEY `idx_activity_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友动态';

INSERT INTO `user` (`id`, `account`, `password_hash`, `username`, `nickname`, `avatar`, `bio`, `phone`) VALUES
('6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', 'panghu', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '胖虎', '胖虎', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80', '好友能看见你的新菜动态，搭子圈能共享可见菜单。', '13800000001'),
('b2a6d8c1-9f44-4e7e-a2d6-3d2f07b2c102', 'ali', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿梨', '阿梨', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80', '喜欢做适合夏天分享的清爽菜。', '13800000002'),
('c3e9a0f2-6d51-45bd-86cf-5b0e8a93d203', 'zhouzhou', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '周周', '周周', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80', '公开分享轻食和 brunch 灵感。', '13800000003'),
('d4f7b1c3-8a62-4f8d-97ae-7c1f9b04e304', 'ahao', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿豪', '阿豪', 'https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=400&q=80', '周末烧烤和夜宵摊专业户。', '13800000004');
