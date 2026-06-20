CREATE TABLE `user_feedback` (
  `id` varchar(36) NOT NULL COMMENT '用户反馈ID(UUID)',
  `user_id` varchar(36) NOT NULL COMMENT '提交用户ID(UUID)',
  `content` varchar(500) NOT NULL COMMENT '反馈内容',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_feedback_user_created` (`user_id`,`created_at`),
  CONSTRAINT `fk_user_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈表';
