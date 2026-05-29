SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `tmp_user_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_user_new_id` (`new_id`)
);

INSERT INTO `tmp_user_id_map` (`old_id`, `new_id`)
SELECT u.id, UUID()
FROM `user` u
LEFT JOIN `tmp_user_id_map` m ON m.old_id = u.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_friend_request_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_friend_request_new_id` (`new_id`)
);

INSERT INTO `tmp_friend_request_id_map` (`old_id`, `new_id`)
SELECT fr.id, UUID()
FROM `friend_request` fr
LEFT JOIN `tmp_friend_request_id_map` m ON m.old_id = fr.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_friend_relation_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_friend_relation_new_id` (`new_id`)
);

INSERT INTO `tmp_friend_relation_id_map` (`old_id`, `new_id`)
SELECT fr.id, UUID()
FROM `friend_relation` fr
LEFT JOIN `tmp_friend_relation_id_map` m ON m.old_id = fr.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_buddy_circle_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_buddy_circle_new_id` (`new_id`)
);

INSERT INTO `tmp_buddy_circle_id_map` (`old_id`, `new_id`)
SELECT bc.id, UUID()
FROM `buddy_circle` bc
LEFT JOIN `tmp_buddy_circle_id_map` m ON m.old_id = bc.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_buddy_circle_member_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_buddy_circle_member_new_id` (`new_id`)
);

INSERT INTO `tmp_buddy_circle_member_id_map` (`old_id`, `new_id`)
SELECT bcm.id, UUID()
FROM `buddy_circle_member` bcm
LEFT JOIN `tmp_buddy_circle_member_id_map` m ON m.old_id = bcm.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_buddy_circle_invite_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_buddy_circle_invite_new_id` (`new_id`)
);

INSERT INTO `tmp_buddy_circle_invite_id_map` (`old_id`, `new_id`)
SELECT bci.id, UUID()
FROM `buddy_circle_invite` bci
LEFT JOIN `tmp_buddy_circle_invite_id_map` m ON m.old_id = bci.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_activity_feed_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_activity_feed_new_id` (`new_id`)
);

INSERT INTO `tmp_activity_feed_id_map` (`old_id`, `new_id`)
SELECT af.id, UUID()
FROM `activity_feed` af
LEFT JOIN `tmp_activity_feed_id_map` m ON m.old_id = af.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_dish_ingredient_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_dish_ingredient_new_id` (`new_id`)
);

INSERT INTO `tmp_dish_ingredient_id_map` (`old_id`, `new_id`)
SELECT di.id, UUID()
FROM `dish_ingredient` di
LEFT JOIN `tmp_dish_ingredient_id_map` m ON m.old_id = di.id
WHERE m.old_id IS NULL;

CREATE TABLE IF NOT EXISTS `tmp_dish_step_id_map` (
  `old_id` bigint NOT NULL,
  `new_id` varchar(36) NOT NULL,
  PRIMARY KEY (`old_id`),
  UNIQUE KEY `uk_tmp_dish_step_new_id` (`new_id`)
);

INSERT INTO `tmp_dish_step_id_map` (`old_id`, `new_id`)
SELECT ds.id, UUID()
FROM `dish_step` ds
LEFT JOIN `tmp_dish_step_id_map` m ON m.old_id = ds.id
WHERE m.old_id IS NULL;

ALTER TABLE `user_profile_settings` DROP FOREIGN KEY `fk_profile_settings_user`;
ALTER TABLE `dish` DROP FOREIGN KEY `fk_dish_user`;

ALTER TABLE `user` ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL;
UPDATE `user` u
JOIN `tmp_user_id_map` m ON m.old_id = u.id
SET u.id_uuid = m.new_id;

ALTER TABLE `user_profile_settings` ADD COLUMN `user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `user_profile_settings` ups
JOIN `tmp_user_id_map` m ON m.old_id = ups.user_id
SET ups.user_id_uuid = m.new_id;

ALTER TABLE `dish` MODIFY COLUMN `id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)';
ALTER TABLE `dish` ADD COLUMN `owner_user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `dish` d
JOIN `tmp_user_id_map` m ON m.old_id = d.owner_user_id
SET d.owner_user_id_uuid = m.new_id;

ALTER TABLE `dish_ingredient` ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL;
UPDATE `dish_ingredient` di
JOIN `tmp_dish_ingredient_id_map` m ON m.old_id = di.id
SET di.id_uuid = m.new_id;
ALTER TABLE `dish_ingredient` MODIFY COLUMN `dish_id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)';

ALTER TABLE `dish_step` ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL;
UPDATE `dish_step` ds
JOIN `tmp_dish_step_id_map` m ON m.old_id = ds.id
SET ds.id_uuid = m.new_id;
ALTER TABLE `dish_step` MODIFY COLUMN `dish_id` varchar(36) NOT NULL COMMENT '菜品ID(UUID)';

ALTER TABLE `friend_request`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `requester_user_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `target_user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `friend_request` fr
JOIN `tmp_friend_request_id_map` fr_map ON fr_map.old_id = fr.id
JOIN `tmp_user_id_map` req_map ON req_map.old_id = fr.requester_user_id
JOIN `tmp_user_id_map` tgt_map ON tgt_map.old_id = fr.target_user_id
SET fr.id_uuid = fr_map.new_id,
    fr.requester_user_id_uuid = req_map.new_id,
    fr.target_user_id_uuid = tgt_map.new_id;

ALTER TABLE `friend_relation`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `user_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `friend_user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `friend_relation` fr
JOIN `tmp_friend_relation_id_map` fr_map ON fr_map.old_id = fr.id
JOIN `tmp_user_id_map` user_map ON user_map.old_id = fr.user_id
JOIN `tmp_user_id_map` friend_map ON friend_map.old_id = fr.friend_user_id
SET fr.id_uuid = fr_map.new_id,
    fr.user_id_uuid = user_map.new_id,
    fr.friend_user_id_uuid = friend_map.new_id;

ALTER TABLE `buddy_circle`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `owner_user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `buddy_circle` bc
JOIN `tmp_buddy_circle_id_map` bc_map ON bc_map.old_id = bc.id
JOIN `tmp_user_id_map` user_map ON user_map.old_id = bc.owner_user_id
SET bc.id_uuid = bc_map.new_id,
    bc.owner_user_id_uuid = user_map.new_id;

ALTER TABLE `buddy_circle_member`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `circle_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `buddy_circle_member` bcm
JOIN `tmp_buddy_circle_member_id_map` bcm_map ON bcm_map.old_id = bcm.id
JOIN `tmp_buddy_circle_id_map` bc_map ON bc_map.old_id = bcm.circle_id
JOIN `tmp_user_id_map` user_map ON user_map.old_id = bcm.user_id
SET bcm.id_uuid = bcm_map.new_id,
    bcm.circle_id_uuid = bc_map.new_id,
    bcm.user_id_uuid = user_map.new_id;

ALTER TABLE `buddy_circle_invite`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `circle_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `inviter_user_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `invitee_user_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `buddy_circle_invite` bci
JOIN `tmp_buddy_circle_invite_id_map` bci_map ON bci_map.old_id = bci.id
JOIN `tmp_buddy_circle_id_map` bc_map ON bc_map.old_id = bci.circle_id
JOIN `tmp_user_id_map` inviter_map ON inviter_map.old_id = bci.inviter_user_id
JOIN `tmp_user_id_map` invitee_map ON invitee_map.old_id = bci.invitee_user_id
SET bci.id_uuid = bci_map.new_id,
    bci.circle_id_uuid = bc_map.new_id,
    bci.inviter_user_id_uuid = inviter_map.new_id,
    bci.invitee_user_id_uuid = invitee_map.new_id;

ALTER TABLE `activity_feed`
  ADD COLUMN `id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `actor_user_id_uuid` varchar(36) DEFAULT NULL,
  ADD COLUMN `circle_id_uuid` varchar(36) DEFAULT NULL;
UPDATE `activity_feed` af
JOIN `tmp_activity_feed_id_map` af_map ON af_map.old_id = af.id
JOIN `tmp_user_id_map` actor_map ON actor_map.old_id = af.actor_user_id
LEFT JOIN `tmp_buddy_circle_id_map` circle_map ON circle_map.old_id = af.circle_id
SET af.id_uuid = af_map.new_id,
    af.actor_user_id_uuid = actor_map.new_id,
    af.circle_id_uuid = circle_map.new_id;
ALTER TABLE `activity_feed` MODIFY COLUMN `dish_id` varchar(36) DEFAULT NULL COMMENT '关联菜品ID(UUID)';

ALTER TABLE `user`
  DROP PRIMARY KEY,
  DROP COLUMN `id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  ADD PRIMARY KEY (`id`);

ALTER TABLE `user_profile_settings`
  DROP PRIMARY KEY,
  DROP COLUMN `user_id`,
  CHANGE COLUMN `user_id_uuid` `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  ADD PRIMARY KEY (`user_id`);

ALTER TABLE `dish`
  DROP INDEX `idx_owner_user_id`,
  DROP COLUMN `owner_user_id`,
  CHANGE COLUMN `owner_user_id_uuid` `owner_user_id` varchar(36) NOT NULL COMMENT '所属用户ID(UUID)',
  ADD KEY `idx_owner_user_id` (`owner_user_id`);

ALTER TABLE `dish_ingredient`
  DROP PRIMARY KEY,
  DROP COLUMN `id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '食材ID(UUID)',
  ADD PRIMARY KEY (`id`);

ALTER TABLE `dish_step`
  DROP PRIMARY KEY,
  DROP COLUMN `id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '步骤ID(UUID)',
  ADD PRIMARY KEY (`id`);

ALTER TABLE `friend_request`
  DROP PRIMARY KEY,
  DROP INDEX `idx_friend_request_requester`,
  DROP INDEX `idx_friend_request_target`,
  DROP COLUMN `id`,
  DROP COLUMN `requester_user_id`,
  DROP COLUMN `target_user_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '申请ID(UUID)',
  CHANGE COLUMN `requester_user_id_uuid` `requester_user_id` varchar(36) NOT NULL COMMENT '发起人(UUID)',
  CHANGE COLUMN `target_user_id_uuid` `target_user_id` varchar(36) NOT NULL COMMENT '接收人(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_friend_request_requester` (`requester_user_id`),
  ADD KEY `idx_friend_request_target` (`target_user_id`);

ALTER TABLE `friend_relation`
  DROP PRIMARY KEY,
  DROP INDEX `uk_friend_pair`,
  DROP INDEX `idx_friend_user`,
  DROP COLUMN `id`,
  DROP COLUMN `user_id`,
  DROP COLUMN `friend_user_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '关系ID(UUID)',
  CHANGE COLUMN `user_id_uuid` `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  CHANGE COLUMN `friend_user_id_uuid` `friend_user_id` varchar(36) NOT NULL COMMENT '好友ID(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_friend_pair` (`user_id`,`friend_user_id`),
  ADD KEY `idx_friend_user` (`friend_user_id`);

ALTER TABLE `buddy_circle`
  DROP PRIMARY KEY,
  DROP INDEX `idx_circle_owner`,
  DROP COLUMN `id`,
  DROP COLUMN `owner_user_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  CHANGE COLUMN `owner_user_id_uuid` `owner_user_id` varchar(36) NOT NULL COMMENT '创建人(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_circle_owner` (`owner_user_id`);

ALTER TABLE `buddy_circle_member`
  DROP PRIMARY KEY,
  DROP INDEX `uk_circle_member`,
  DROP INDEX `idx_circle_member_user`,
  DROP COLUMN `id`,
  DROP COLUMN `circle_id`,
  DROP COLUMN `user_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '成员ID(UUID)',
  CHANGE COLUMN `circle_id_uuid` `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  CHANGE COLUMN `user_id_uuid` `user_id` varchar(36) NOT NULL COMMENT '用户ID(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_circle_member` (`circle_id`,`user_id`),
  ADD KEY `idx_circle_member_user` (`user_id`);

ALTER TABLE `buddy_circle_invite`
  DROP PRIMARY KEY,
  DROP INDEX `idx_circle_invite_circle`,
  DROP INDEX `idx_circle_invite_user`,
  DROP COLUMN `id`,
  DROP COLUMN `circle_id`,
  DROP COLUMN `inviter_user_id`,
  DROP COLUMN `invitee_user_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '邀请ID(UUID)',
  CHANGE COLUMN `circle_id_uuid` `circle_id` varchar(36) NOT NULL COMMENT '圈子ID(UUID)',
  CHANGE COLUMN `inviter_user_id_uuid` `inviter_user_id` varchar(36) NOT NULL COMMENT '邀请人(UUID)',
  CHANGE COLUMN `invitee_user_id_uuid` `invitee_user_id` varchar(36) NOT NULL COMMENT '被邀请人(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_circle_invite_circle` (`circle_id`),
  ADD KEY `idx_circle_invite_user` (`invitee_user_id`);

ALTER TABLE `activity_feed`
  DROP PRIMARY KEY,
  DROP INDEX `idx_activity_actor`,
  DROP INDEX `idx_activity_circle`,
  DROP COLUMN `id`,
  DROP COLUMN `actor_user_id`,
  DROP COLUMN `circle_id`,
  CHANGE COLUMN `id_uuid` `id` varchar(36) NOT NULL COMMENT '动态ID(UUID)',
  CHANGE COLUMN `actor_user_id_uuid` `actor_user_id` varchar(36) NOT NULL COMMENT '动态用户(UUID)',
  CHANGE COLUMN `circle_id_uuid` `circle_id` varchar(36) DEFAULT NULL COMMENT '关联圈子ID(UUID)',
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_activity_actor` (`actor_user_id`),
  ADD KEY `idx_activity_circle` (`circle_id`);

ALTER TABLE `user_profile_settings`
  ADD CONSTRAINT `fk_profile_settings_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);

ALTER TABLE `dish`
  ADD CONSTRAINT `fk_dish_user` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`);

DROP TABLE IF EXISTS `tmp_dish_step_id_map`;
DROP TABLE IF EXISTS `tmp_dish_ingredient_id_map`;
DROP TABLE IF EXISTS `tmp_activity_feed_id_map`;
DROP TABLE IF EXISTS `tmp_buddy_circle_invite_id_map`;
DROP TABLE IF EXISTS `tmp_buddy_circle_member_id_map`;
DROP TABLE IF EXISTS `tmp_buddy_circle_id_map`;
DROP TABLE IF EXISTS `tmp_friend_relation_id_map`;
DROP TABLE IF EXISTS `tmp_friend_request_id_map`;
DROP TABLE IF EXISTS `tmp_user_id_map`;

SET FOREIGN_KEY_CHECKS = 1;
