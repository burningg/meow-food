ALTER TABLE `circle_plan_recipe`
  ADD COLUMN `sort` int NOT NULL DEFAULT '0' COMMENT '排序' AFTER `added_by_user_id`;
