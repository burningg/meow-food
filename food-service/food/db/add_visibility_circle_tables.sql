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
