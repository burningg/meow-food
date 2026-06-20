CREATE TABLE `user_pet` (
  `id` varchar(36) NOT NULL COMMENT '宠物ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  `pet_type` varchar(40) NOT NULL COMMENT '宠物类型',
  `name` varchar(40) NOT NULL COMMENT '宠物昵称',
  `experience` int NOT NULL DEFAULT '0' COMMENT '宠物经验，预留成长功能',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_pet_user` (`user_id`),
  KEY `idx_user_pet_type` (`pet_type`),
  CONSTRAINT `fk_user_pet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户宠物表';
