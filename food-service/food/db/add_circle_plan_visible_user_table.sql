CREATE TABLE `circle_plan_visible_user` (
  `id` varchar(36) NOT NULL COMMENT '计划可见用户ID(UUID)',
  `plan_id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '可见用户ID(UUID)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_plan_visible_user` (`plan_id`,`user_id`),
  KEY `idx_circle_plan_visible_user_user_plan` (`user_id`,`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划指定可见用户表';
