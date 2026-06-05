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
