package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String title;

    private String category;

    private String imageUrl;

    private String bodyNodesJson;

    private Integer status = 1;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
