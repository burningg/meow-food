package com.panghu.food.service;

import com.panghu.food.dto.KnowledgeArticleDetailResponse;
import com.panghu.food.dto.KnowledgeArticleListResponse;
import com.panghu.food.entity.KnowledgeArticle;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.KnowledgeArticleMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceImplTest {
    private final KnowledgeArticleMapper knowledgeArticleMapper = mock(KnowledgeArticleMapper.class);
    private final KnowledgeServiceImpl knowledgeService = new KnowledgeServiceImpl(knowledgeArticleMapper);

    @Test
    void getPublishedArticlesUsesDefaultPageSizeAndChecksHasMoreInOneQuery() {
        when(knowledgeArticleMapper.selectPublishedByPage(21, 0)).thenReturn(articles(21));

        KnowledgeArticleListResponse response = knowledgeService.getPublishedArticles(null, null);

        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getItems()).hasSize(20);
        verify(knowledgeArticleMapper).selectPublishedByPage(21, 0);
    }

    @Test
    void getArticleReturnsRichTextNodes() {
        KnowledgeArticle article = article("article-1");
        when(knowledgeArticleMapper.selectPublishedById("article-1")).thenReturn(article);

        KnowledgeArticleDetailResponse response = knowledgeService.getArticle("article-1");

        assertThat(response.getTitle()).isEqualTo("晚餐太晚，怎么吃更舒服");
        assertThat(response.getImageUrl()).contains("images.unsplash.com");
        assertThat(response.getBodyNodes()).hasSize(1);
        assertThat(response.getBodyNodes().get(0).getName()).isEqualTo("p");
        assertThat(response.getBodyPreview()).contains("把份量、速度和睡前间隔调轻一点");
    }

    @Test
    void getArticleRejectsMissingArticle() {
        when(knowledgeArticleMapper.selectPublishedById("missing")).thenReturn(null);

        assertThatThrownBy(() -> knowledgeService.getArticle("missing"))
                .isInstanceOf(ApiException.class)
                .hasMessage("知识不存在");
    }

    private List<KnowledgeArticle> articles(int count) {
        List<KnowledgeArticle> articles = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            KnowledgeArticle article = article("article-" + index);
            article.setTitle("知识 " + index);
            articles.add(article);
        }
        return articles;
    }

    private KnowledgeArticle article(String id) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setId(id);
        article.setTitle("晚餐太晚，怎么吃更舒服");
        article.setCategory("饮食习惯");
        article.setImageUrl("https://images.unsplash.com/photo-1596040033229-a9821ebd058d?auto=format&fit=crop&w=900&q=80");
        article.setBodyNodesJson("[{\"name\":\"p\",\"children\":[{\"type\":\"text\",\"text\":\"把份量、速度和睡前间隔调轻一点，身体会更容易进入休息。\"}]}]");
        article.setPublishedAt(LocalDateTime.of(2026, 6, 21, 8, 0));
        return article;
    }
}
