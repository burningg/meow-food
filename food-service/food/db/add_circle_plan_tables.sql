CREATE TABLE `circle_plan` (
  `id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  `plan_date` date NOT NULL COMMENT '计划日期',
  `title` varchar(100) NOT NULL COMMENT '计划标题',
  `creator_user_id` varchar(36) NOT NULL COMMENT '创建人用户ID(UUID)',
  `share_token` varchar(64) DEFAULT NULL COMMENT '计划分享令牌',
  `shopping_status` varchar(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '采购状态: NOT_STARTED/NOT_PURCHASED/PARTIALLY_PURCHASED/PURCHASED',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_circle_plan_circle_date` (`circle_id`,`plan_date`),
  KEY `idx_circle_plan_creator` (`creator_user_id`),
  UNIQUE KEY `uk_circle_plan_share_token` (`share_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='圈子计划表';

CREATE TABLE `circle_plan_recipe` (
  `id` varchar(36) NOT NULL COMMENT '计划菜谱关联ID(UUID)',
  `plan_id` varchar(36) NOT NULL COMMENT '计划ID(UUID)',
  `dish_id` varchar(36) NOT NULL COMMENT '菜谱ID(UUID)',
  `added_by_user_id` varchar(36) NOT NULL COMMENT '添加人用户ID(UUID)',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_circle_plan_recipe` (`plan_id`,`dish_id`),
  KEY `idx_circle_plan_recipe_dish` (`dish_id`),
  KEY `idx_circle_plan_recipe_added_by` (`added_by_user_id`)
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
