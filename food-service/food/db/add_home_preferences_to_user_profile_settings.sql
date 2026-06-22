ALTER TABLE `user_profile_settings`
  ADD COLUMN `show_knowledge_on_home` tinyint(1) NOT NULL DEFAULT '1' COMMENT '首页是否显示饮食小知识' AFTER `allow_friend_feed`,
  ADD COLUMN `show_pet_on_home` tinyint(1) NOT NULL DEFAULT '1' COMMENT '首页是否显示宠物' AFTER `show_knowledge_on_home`;
