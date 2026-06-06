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

INSERT INTO `user_notification` (`id`, `user_id`, `title`, `summary`, `body`, `audience_type`, `priority`, `recipient_scope`, `recipient_cutoff_at`, `published_at`, `read_at`) VALUES
('1c08f7de-4a53-4301-a6b8-11d9f1d50001', NULL, '今晚共享厨房维护时间提前', '今晚共享厨房集中备菜时间提前到 19:00，请尽量在 18:40 前到场。', '由于公共冰柜需要临时检修，今晚共享厨房的集中备菜时间将提前到 19:00 开始。需要使用公共灶台的朋友请尽量在 18:40 前到场，避免排队等待。', 'broadcast', 'important', 'all_users', NULL, '2026-06-06 17:50:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50002', NULL, '六月厨房清洁日提醒', '本周五晚 8 点会统一做公共厨房清洁，记得把暂存食材贴好名字。', '本周五晚 8 点会统一做公共厨房清洁，记得把暂存食材贴好名字，易串味食材请提前封好。', 'broadcast', 'normal', 'all_users', NULL, '2026-06-06 14:20:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50003', NULL, '端午假期配送时段说明', '假期期间生鲜配送会顺延半天，急用食材建议提前一天完成下单。', '端午假期期间生鲜配送会顺延半天，急用食材建议提前一天完成下单，避免影响周末备菜安排。', 'broadcast', 'normal', 'existing_users_only', '2026-06-05 19:40:00', '2026-06-05 19:40:00', NULL),
('1c08f7de-4a53-4301-a6b8-11d9f1d50004', '6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', '你收藏的夜宵清单已整理完成', '系统已经把你最近收藏的夜宵菜单归入同一份清单，晚上想做时会更好找。', '系统已经把你最近收藏的夜宵菜单归入同一份清单，晚上想做时会更好找，也方便下次继续补充。', 'direct', 'normal', 'all_users', NULL, '2026-06-03 11:05:00', '2026-06-03 11:20:00');

INSERT INTO `user_notification_broadcast_read` (`id`, `notification_id`, `user_id`, `read_at`) VALUES
('1c08f7de-4a53-4301-a6b8-11d9f1d50011', '1c08f7de-4a53-4301-a6b8-11d9f1d50003', '6f1b2b8d-3ef7-4b7d-9eb2-6d9e8e4b1a01', '2026-06-05 20:10:00');
