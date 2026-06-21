package com.panghu.food.service;

import com.panghu.food.dto.KnowledgeArticleDetailResponse;
import com.panghu.food.dto.KnowledgeArticleListResponse;

public interface KnowledgeService {
    KnowledgeArticleListResponse getPublishedArticles(Integer page, Integer size);

    KnowledgeArticleDetailResponse getArticle(String id);
}
