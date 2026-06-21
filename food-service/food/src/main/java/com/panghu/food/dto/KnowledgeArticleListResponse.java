package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeArticleListResponse {
    private List<KnowledgeArticleSummaryResponse> items = new ArrayList<>();
    private int page;
    private int size;
    private boolean hasMore;
}
