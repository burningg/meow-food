CREATE TABLE `category` (
  `id` varchar(50) NOT NULL COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品分类表';

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
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
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `default_menu_visibility` varchar(20) NOT NULL DEFAULT 'friends' COMMENT '默认菜单可见范围',
  `allow_friend_feed` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许进入好友动态',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_profile_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户资料配置';

CREATE TABLE `dish` (
  `id` varchar(100) NOT NULL COMMENT '菜品ID',
  `owner_user_id` bigint NOT NULL COMMENT '所属用户ID',
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
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '食材ID',
  `dish_id` varchar(100) NOT NULL COMMENT '菜品ID',
  `name` varchar(100) NOT NULL COMMENT '食材名称',
  `amount` varchar(100) NOT NULL COMMENT '食材用量',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_dish_ingredient_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品食材表';

CREATE TABLE `dish_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `dish_id` varchar(100) NOT NULL COMMENT '菜品ID',
  `step_no` int NOT NULL COMMENT '步骤序号',
  `content` varchar(1000) NOT NULL COMMENT '步骤内容',
  PRIMARY KEY (`id`),
  KEY `idx_dish_step_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜品步骤表';

CREATE TABLE `friend_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `requester_user_id` bigint NOT NULL COMMENT '发起人',
  `target_user_id` bigint NOT NULL COMMENT '接收人',
  `message` varchar(200) DEFAULT NULL COMMENT '申请备注',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_friend_request_requester` (`requester_user_id`),
  KEY `idx_friend_request_target` (`target_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友申请表';

CREATE TABLE `friend_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关系ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `friend_user_id` bigint NOT NULL COMMENT '好友ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_friend_pair` (`user_id`,`friend_user_id`),
  KEY `idx_friend_user` (`friend_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表';

CREATE TABLE `buddy_circle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '圈子ID',
  `name` varchar(100) NOT NULL COMMENT '圈名',
  `description` varchar(255) DEFAULT NULL COMMENT '圈描述',
  `owner_user_id` bigint NOT NULL COMMENT '创建人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈';

CREATE TABLE `buddy_circle_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成员ID',
  `circle_id` bigint NOT NULL COMMENT '圈子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL DEFAULT 'member' COMMENT '角色',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_member` (`circle_id`,`user_id`),
  KEY `idx_circle_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈成员';

CREATE TABLE `buddy_circle_invite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `circle_id` bigint NOT NULL COMMENT '圈子ID',
  `inviter_user_id` bigint NOT NULL COMMENT '邀请人',
  `invitee_user_id` bigint NOT NULL COMMENT '被邀请人',
  `status` varchar(20) NOT NULL DEFAULT 'joined' COMMENT '状态',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_invite_circle` (`circle_id`),
  KEY `idx_circle_invite_user` (`invitee_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搭子圈邀请记录';

CREATE TABLE `activity_feed` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `actor_user_id` bigint NOT NULL COMMENT '动态用户',
  `dish_id` varchar(100) DEFAULT NULL COMMENT '关联菜品ID',
  `circle_id` bigint DEFAULT NULL COMMENT '关联圈子ID',
  `activity_type` varchar(20) NOT NULL COMMENT '动态类型',
  `visibility_scope` varchar(20) NOT NULL COMMENT '动态可见性',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_activity_actor` (`actor_user_id`),
  KEY `idx_activity_circle` (`circle_id`),
  KEY `idx_activity_dish` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友动态';

CREATE TABLE `order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_amount` decimal(10,2) NOT NULL COMMENT '总金额',
  `status` tinyint NOT NULL COMMENT '状态：0-待支付，1-已支付，2-已完成，3-已取消',
  `order_date` date NOT NULL COMMENT '订单日期',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_order_date` (`order_date`),
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单表';

CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `dish_id` varchar(100) NOT NULL COMMENT '菜品ID',
  `quantity` int NOT NULL COMMENT '数量',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单项表';

INSERT INTO `category` (`id`, `name`, `sort`) VALUES
('meat', '家常肉菜', 1),
('seafood', '海鲜', 2),
('veggie', '清爽素菜', 3),
('soup', '热汤锅物', 4),
('staple', '主食', 5);

INSERT INTO `user` (`id`, `account`, `password_hash`, `username`, `nickname`, `avatar`, `bio`, `phone`) VALUES
(1, 'panghu', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '胖虎', '胖虎', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80', '好友能看见你的新菜动态，搭子圈能共享可见菜单。', '13800000001'),
(2, 'ali', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿梨', '阿梨', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80', '喜欢做适合夏天分享的清爽菜。', '13800000002'),
(3, 'zhouzhou', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '周周', '周周', 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80', '公开分享轻食和 brunch 灵感。', '13800000003'),
(4, 'ahao', '10000:FQIbdD4rx2LE459Dn2ereQ==:qVz9FQvpypcRhuGcDZYvNWWy17F2XOMQ77GtozS7uG0=', '阿豪', '阿豪', 'https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=400&q=80', '周末烧烤和夜宵摊专业户。', '13800000004');

INSERT INTO `user_profile_settings` (`user_id`, `default_menu_visibility`, `allow_friend_feed`) VALUES
(1, 'friends', 1),
(2, 'friends', 1),
(3, 'public', 1),
(4, 'friends', 1);

INSERT INTO `friend_relation` (`user_id`, `friend_user_id`) VALUES
(1, 2), (2, 1),
(1, 4), (4, 1),
(2, 4), (4, 2);

INSERT INTO `buddy_circle` (`id`, `name`, `description`, `owner_user_id`) VALUES
(1, '周末探店局', '和好友一起建圈、邀人、共享菜单，大家能在菜单页一起决定吃什么。', 1);

INSERT INTO `buddy_circle_member` (`circle_id`, `user_id`, `role`) VALUES
(1, 1, 'owner'),
(1, 3, 'member'),
(1, 4, 'member');

INSERT INTO `buddy_circle_invite` (`circle_id`, `inviter_user_id`, `invitee_user_id`, `status`) VALUES
(1, 1, 3, 'joined'),
(1, 1, 4, 'joined');

INSERT INTO `dish` (`id`, `owner_user_id`, `name`, `image`, `description`, `category_id`, `cook_time_minutes`, `difficulty`, `servings`, `visibility`, `is_featured`, `status`) VALUES
('dish-panghu-hotpot', 1, '露营火锅菜单', 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=800&q=80', '番茄锅底、肥牛卷、虾滑、手打柠檬茶。', 'soup', 35, 'medium', 4, 'friends', 1, 1),
('dish-ali-chicken', 2, '椒麻手撕鸡', 'https://images.unsplash.com/photo-1525755662778-989d0524087e?auto=format&fit=crop&w=800&q=80', '好友可见，适合夏天的凉拌菜。', 'meat', 25, 'easy', 2, 'friends', 1, 1),
('dish-ali-rice', 2, '黑松露牛肝菌饭', 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=800&q=80', '奶香浓郁的私房主食。', 'staple', 40, 'medium', 3, 'private', 0, 1),
('dish-zhou-brunch', 3, '城市漫步 brunch', 'https://images.unsplash.com/photo-1490645935967-10de6ba17061?auto=format&fit=crop&w=800&q=80', '公开访问，含牛油果吐司、班尼迪克蛋、冰美式。', 'staple', 20, 'easy', 2, 'public', 1, 1),
('dish-zhou-light', 3, '一周轻食合集', 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=800&q=80', '公开访问，含 7 道可收藏菜品。', 'veggie', 18, 'easy', 2, 'inherit', 1, 1),
('dish-ahao-bbq', 4, '周末烧烤采购清单', 'https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=800&q=80', '仅圈内可见，3 位搭子已加入讨论。', 'meat', 50, 'medium', 5, 'friends', 0, 1),
('dish-ahao-night', 4, '夜宵摊同款', 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=800&q=80', '炒河粉、椒盐鸡翅、冰镇毛豆。', 'staple', 30, 'medium', 3, 'inherit', 1, 1);

INSERT INTO `activity_feed` (`actor_user_id`, `dish_id`, `circle_id`, `activity_type`, `visibility_scope`) VALUES
(2, 'dish-ali-chicken', NULL, 'dish_created', 'friends'),
(3, 'dish-zhou-light', NULL, 'dish_updated', 'public'),
(4, 'dish-ahao-bbq', 1, 'circle_shared', 'circle');
