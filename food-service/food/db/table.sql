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
  `default_menu_visibility` varchar(20) NOT NULL DEFAULT 'public' COMMENT '默认菜单可见范围',
  `last_selected_circle_id` varchar(36) DEFAULT NULL COMMENT '上次选中的圈子ID',
  `allow_friend_feed` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许进入好友动态',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_profile_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料配置';

CREATE TABLE `user_vip` (
  `id` varchar(36) NOT NULL COMMENT 'VIP记录ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `vip_level` varchar(20) DEFAULT NULL COMMENT 'VIP等级',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '当前是否VIP',
  `opened_at` datetime DEFAULT NULL COMMENT '开通时间',
  `expires_at` datetime DEFAULT NULL COMMENT '截止时间',
  `open_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '开通金额',
  `daily_recipe_analysis_limit` int NOT NULL DEFAULT '0' COMMENT '每日菜谱分析次数上限',
  `daily_recipe_analysis_used` int NOT NULL DEFAULT '0' COMMENT '每日菜谱分析已使用次数',
  `daily_recipe_analysis_date` date DEFAULT NULL COMMENT '每日次数统计日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_vip_user_id` (`user_id`),
  CONSTRAINT `fk_user_vip_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户VIP表';

CREATE TABLE `user_notification` (
  `id` varchar(36) NOT NULL COMMENT '通知ID(UUID)',
  `user_id` varchar(36) DEFAULT NULL COMMENT '接收用户ID(UUID)，广播消息为空',
  `title` varchar(120) NOT NULL COMMENT '通知标题',
  `summary` varchar(255) DEFAULT NULL COMMENT '通知摘要',
  `body` varchar(2000) NOT NULL COMMENT '通知正文',
  `audience_type` varchar(20) NOT NULL COMMENT '通知受众：broadcast/direct',
  `priority` varchar(20) NOT NULL DEFAULT 'normal' COMMENT '通知优先级：normal/important',
  `recipient_scope` varchar(30) NOT NULL DEFAULT 'all_users' COMMENT '广播接收范围：all_users/existing_users_only',
  `recipient_cutoff_at` datetime DEFAULT NULL COMMENT '广播接收截止创建时间，existing_users_only 使用',
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间，仅 direct 使用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_notification_audience` (`audience_type`,`published_at`),
  KEY `idx_user_notification_user` (`user_id`),
  KEY `idx_user_notification_user_priority` (`user_id`,`priority`),
  KEY `idx_user_notification_user_read` (`user_id`,`read_at`),
  CONSTRAINT `fk_user_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知表';

CREATE TABLE `user_notification_broadcast_read` (
  `id` varchar(36) NOT NULL COMMENT '广播已读记录ID(UUID)',
  `notification_id` varchar(36) NOT NULL COMMENT '广播通知ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '已读用户ID(UUID)',
  `read_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_user_read` (`notification_id`,`user_id`),
  KEY `idx_user_notification_broadcast_read_user` (`user_id`),
  CONSTRAINT `fk_notification_broadcast_read_notification` FOREIGN KEY (`notification_id`) REFERENCES `user_notification` (`id`),
  CONSTRAINT `fk_notification_broadcast_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广播通知已读表';

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
  `visibility` varchar(20) NOT NULL DEFAULT 'public' COMMENT '可见范围：public/private/circle，兼容旧值inherit/friends',
  `is_featured` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否推荐：0-否，1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_owner_user_id` (`owner_user_id`),
  CONSTRAINT `fk_dish_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品表';

CREATE TABLE `dish_visibility_circle` (
  `id` varchar(36) NOT NULL COMMENT '关联ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_visibility_circle` (`dish_id`,`circle_id`),
  KEY `idx_dish_visibility_circle_circle` (`circle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品指定圈子可见关系';

CREATE TABLE `user_default_visibility_circle` (
  `id` varchar(36) NOT NULL COMMENT '关联ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_default_visibility_circle` (`user_id`,`circle_id`),
  KEY `idx_user_default_visibility_circle_circle` (`circle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户默认指定圈子可见关系';

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

CREATE TABLE `circle_plan` (
  `id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `title` varchar(100) NOT NULL COMMENT '计划标题',
  `creator_user_id` varchar(36) NOT NULL COMMENT '创建人用户ID(UUID)',
  `shopping_status` varchar(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '采购状态: NOT_STARTED/NOT_PURCHASED/PARTIALLY_PURCHASED/PURCHASED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_plan_circle_date` (`circle_id`,`plan_date`),
  KEY `idx_circle_plan_creator` (`creator_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划表';

CREATE TABLE `circle_plan_recipe` (
  `id` varchar(36) NOT NULL COMMENT '计划菜谱关联ID(UUID)',
  `plan_id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜谱ID(UUID)',
  `added_by_user_id` varchar(36) NOT NULL COMMENT '添加人用户ID(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_plan_recipe` (`plan_id`,`dish_id`),
  KEY `idx_circle_plan_recipe_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划菜谱关联表';

CREATE TABLE `circle_plan_shopping_list` (
  `id` varchar(36) NOT NULL COMMENT '采购清单ID(UUID)',
  `plan_id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `started_by_user_id` varchar(36) NOT NULL COMMENT '发起采购用户ID(UUID)',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起采购时间',
  `restart_count` int NOT NULL DEFAULT '0' COMMENT '重新开始次数',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_plan_shopping_list_plan` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划采购清单表';

CREATE TABLE `circle_plan_shopping_item` (
  `id` varchar(36) NOT NULL COMMENT '采购项ID(UUID)',
  `shopping_list_id` varchar(36) NOT NULL COMMENT '采购清单ID(UUID)',
  `ingredient_name` varchar(100) NOT NULL COMMENT '食材名',
  `purchased` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已采买',
  `purchased_by_user_id` varchar(36) DEFAULT NULL COMMENT '采买人用户ID(UUID)',
  `purchased_at` datetime DEFAULT NULL COMMENT '采买时间',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_circle_plan_shopping_item_list` (`shopping_list_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划采购项表';

CREATE TABLE `circle_plan_shopping_item_source` (
  `id` varchar(36) NOT NULL COMMENT '采购项来源ID(UUID)',
  `shopping_item_id` varchar(36) NOT NULL COMMENT '采购项ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜谱ID(UUID)',
  `dish_name` varchar(100) NOT NULL COMMENT '菜谱名称',
  `amount` varchar(100) NOT NULL COMMENT '该菜谱中的用量',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_circle_plan_shopping_item_source_item` (`shopping_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划采购项来源表';

INSERT INTO `user` (`id`, `account`, `password_hash`, `username`, `nickname`, `avatar`, `bio`, `phone`) VALUES
('6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', 'panghu', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '胖虎', '胖虎', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80', '好友能看见你的新菜动态，搭子圈能共享可见菜单。', '13800000001'),
('b2a6d8c1-9f44-4e7e-a2d6-3d2f07b2c102', 'ali', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿梨', '阿梨', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80', '喜欢做适合夏天分享的清爽菜。', '13800000002'),
('c3e9a0f2-6d51-45bd-86cf-5b0e8a93d203', 'zhouzhou', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '周周', '周周', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80', '公开分享轻食和 brunch 灵感。', '13800000003'),
('d4f7b1c3-8a62-4f8d-97ae-7c1f9b04e304', 'ahao', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿豪', '阿豪', 'https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=400&q=80', '周末烧烤和夜宵摊专业户。', '13800000004');

INSERT INTO `user_vip` (`id`, `user_id`, `vip_level`, `is_vip`, `opened_at`, `expires_at`, `open_amount`, `daily_recipe_analysis_limit`, `daily_recipe_analysis_used`, `daily_recipe_analysis_date`) VALUES
('a4d19c4d-2fa8-4f15-b484-8186b7bb1111', '6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', 'VIP 3', 1, '2026-06-01 00:00:00', '2026-12-31 23:59:59', 199.00, 3, 0, '2026-06-05');

INSERT INTO `user_notification` (`id`, `user_id`, `title`, `summary`, `body`, `audience_type`, `priority`, `published_at`, `read_at`) VALUES
('1c08f7de-4a53-4301-a6b8-11d9f1d50001', NULL, '今晚共享厨房维护时间提前', '今晚共享厨房集中备菜时间提前到 19:00，请尽量在 18:40 前到场。', '由于公共冰柜需要临时检修，今晚共享厨房的集中备菜时间将提前到 19:00 开始。需要使用公共灶台的朋友请尽量在 18:40 前到场，避免排队等待。', 'broadcast', 'important', '2026-06-06 17:50:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50002', NULL, '六月厨房清洁日提醒', '本周五晚 8 点会统一做公共厨房清洁，记得把暂存食材贴好名字。', '本周五晚 8 点会统一做公共厨房清洁，记得把暂存食材贴好名字，易串味食材请提前封好。', 'broadcast', 'normal', '2026-06-06 14:20:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50003', NULL, '端午假期配送时段说明', '假期期间生鲜配送会顺延半天，急用食材建议提前一天完成下单。', '端午假期期间生鲜配送会顺延半天，急用食材建议提前一天完成下单，避免影响周末备菜安排。', 'broadcast', 'normal', '2026-06-05 19:40:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50004', '6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', '你收藏的夜宵清单已整理完成', '系统已经把你最近收藏的夜宵菜单归入同一份清单，晚上想做时会更好找。', '系统已经把你最近收藏的夜宵菜单归入同一份清单，晚上想做时会更好找，也方便下次继续补充。', 'direct', 'normal', '2026-06-03 11:05:00', '2026-06-03 11:20:00');

INSERT INTO `user_notification_broadcast_read` (`id`, `notification_id`, `user_id`, `read_at`) VALUES
('1c08f7de-4a53-4301-a6b8-11d9f1d50011', '1c08f7de-4a53-4301-a6b8-11d9f1d50003', '6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', '2026-06-05 20:10:00');
