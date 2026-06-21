package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeArticleSummaryResponse {
    private String id;
    private String title;
    private String category;
    private String imageUrl;
    private String bodyPreview;
    private LocalDateTime publishedAt;
}
