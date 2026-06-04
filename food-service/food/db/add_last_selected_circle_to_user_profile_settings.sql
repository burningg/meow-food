ALTER TABLE `user_profile_settings`
ADD COLUMN `last_selected_circle_id` varchar(36) DEFAULT NULL COMMENT '上次选中的圈子ID'
AFTER `default_menu_visibility`;
