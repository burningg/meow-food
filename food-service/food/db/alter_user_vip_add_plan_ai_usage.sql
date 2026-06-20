ALTER TABLE `user_vip`
  ADD COLUMN `monthly_plan_ai_used` int NOT NULL DEFAULT '0' COMMENT '每月AI排菜已使用次数' AFTER `daily_recipe_analysis_date`,
  ADD COLUMN `monthly_plan_ai_month` varchar(7) DEFAULT NULL COMMENT '每月AI排菜统计月份(YYYY-MM)' AFTER `monthly_plan_ai_used`;
