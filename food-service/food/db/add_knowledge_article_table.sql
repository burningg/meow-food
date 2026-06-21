CREATE TABLE `knowledge_article` (
  `id` varchar(36) NOT NULL COMMENT '知识ID(UUID)',
  `title` varchar(120) NOT NULL COMMENT '题目',
  `category` varchar(50) NOT NULL COMMENT '分类',
  `image_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `body_nodes_json` text NOT NULL COMMENT '正文rich-text nodes JSON，使用通用HTML标签兼容小程序和其他端',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-发布',
  `published_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_status_published` (`status`,`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='饮食知识表';

INSERT INTO `knowledge_article` (`id`, `title`, `category`, `image_url`, `body_nodes_json`, `status`, `published_at`) VALUES
('e8f0d8b6-4f9b-46f8-9b6f-1a2b3c4d5e02', '肉蔻是什么，适合放在哪些菜里', '香料', 'https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=900&q=80', '[{"name":"p","children":[{"type":"text","text":"肉蔻通常指肉豆蔻，是肉豆蔻树种仁制成的香料。它的气味温暖，带一点木质、坚果和甜香，适合给厚重菜肴收一层柔和的尾味。"}]},{"name":"p","children":[{"type":"text","text":"用它调味时不需要多，一点点现磨粉末就能让炖肉、奶油汤、土豆泥、南瓜、肉酱和烘焙甜点更圆润。中式炖煮里，也可以和桂皮、丁香少量搭配。"}]},{"name":"ul","children":[{"name":"li","children":[{"type":"text","text":"少量现磨：香气更清楚，也更容易控制味道。"}]},{"name":"li","children":[{"type":"text","text":"晚些加入：长时间高温会让香气变钝，出锅前少量补一点更稳。"}]},{"name":"li","children":[{"type":"text","text":"只作调味：不要把肉蔻当作保健品大量食用，特殊身体情况先遵医嘱。"}]}]},{"name":"p","children":[{"type":"text","text":"如果第一次尝试，可以从一小撮开始。闻起来刚刚有暖香，但吃不出明显药味，就是比较舒服的量。"}]}]', 1, CURRENT_TIMESTAMP)
