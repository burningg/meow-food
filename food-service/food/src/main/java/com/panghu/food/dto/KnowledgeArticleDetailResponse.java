package com.panghu.food.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeArticleDetailResponse {
    private String id;
    private String title;
    private String category;
    private String imageUrl;
    private String bodyPreview;
    private List<KnowledgeRichTextNode> bodyNodes = new ArrayList<>();
    private LocalDateTime publishedAt;
}
