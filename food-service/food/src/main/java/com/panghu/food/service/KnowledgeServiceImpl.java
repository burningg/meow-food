package com.panghu.food.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.panghu.food.dto.KnowledgeArticleDetailResponse;
import com.panghu.food.dto.KnowledgeArticleListResponse;
import com.panghu.food.dto.KnowledgeArticleSummaryResponse;
import com.panghu.food.dto.KnowledgeRichTextNode;
import com.panghu.food.entity.KnowledgeArticle;
import com.panghu.food.exception.ApiException;
import com.panghu.food.mapper.KnowledgeArticleMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int PREVIEW_MAX_LENGTH = 72;

    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public KnowledgeServiceImpl(KnowledgeArticleMapper knowledgeArticleMapper) {
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    @Override
    public KnowledgeArticleListResponse getPublishedArticles(Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<KnowledgeArticle> articles = knowledgeArticleMapper.selectPublishedByPage(normalizedSize + 1, offset);
        boolean hasMore = articles.size() > normalizedSize;
        List<KnowledgeArticle> currentPageArticles = hasMore
                ? articles.subList(0, normalizedSize)
                : articles;

        KnowledgeArticleListResponse response = new KnowledgeArticleListResponse();
        response.setPage(normalizedPage);
        response.setSize(normalizedSize);
        response.setHasMore(hasMore);
        response.setItems(currentPageArticles.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public KnowledgeArticleDetailResponse getArticle(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "知识不存在");
        }
        KnowledgeArticle article = knowledgeArticleMapper.selectPublishedById(id.trim());
        if (article == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "知识不存在");
        }
        return toDetailResponse(article);
    }

    private KnowledgeArticleSummaryResponse toSummaryResponse(KnowledgeArticle article) {
        List<KnowledgeRichTextNode> bodyNodes = parseBodyNodes(article.getBodyNodesJson());
        KnowledgeArticleSummaryResponse response = new KnowledgeArticleSummaryResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setCategory(article.getCategory());
        response.setImageUrl(article.getImageUrl());
        response.setBodyPreview(buildPreview(bodyNodes));
        response.setPublishedAt(article.getPublishedAt());
        return response;
    }

    private KnowledgeArticleDetailResponse toDetailResponse(KnowledgeArticle article) {
        List<KnowledgeRichTextNode> bodyNodes = parseBodyNodes(article.getBodyNodesJson());
        KnowledgeArticleDetailResponse response = new KnowledgeArticleDetailResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setCategory(article.getCategory());
        response.setImageUrl(article.getImageUrl());
        response.setBodyPreview(buildPreview(bodyNodes));
        response.setBodyNodes(bodyNodes);
        response.setPublishedAt(article.getPublishedAt());
        return response;
    }

    private List<KnowledgeRichTextNode> parseBodyNodes(String bodyNodesJson) {
        if (bodyNodesJson == null || bodyNodesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // 正文按小程序 rich-text nodes 存储，只使用通用 HTML 标签，便于小程序/H5/其他端复用同一份内容。
            List<KnowledgeRichTextNode> nodes = JSON.parseArray(bodyNodesJson, KnowledgeRichTextNode.class);
            return nodes == null ? new ArrayList<>() : nodes;
        } catch (JSONException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "知识正文格式异常");
        }
    }

    private String buildPreview(List<KnowledgeRichTextNode> bodyNodes) {
        StringBuilder builder = new StringBuilder();
        for (KnowledgeRichTextNode node : bodyNodes) {
            appendNodeText(node, builder);
            if (builder.length() >= PREVIEW_MAX_LENGTH) {
                break;
            }
        }
        String text = builder.toString().replaceAll("\\s+", " ").trim();
        if (text.length() <= PREVIEW_MAX_LENGTH) {
            return text;
        }
        return text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }

    private void appendNodeText(KnowledgeRichTextNode node, StringBuilder builder) {
        if (node == null || builder.length() >= PREVIEW_MAX_LENGTH) {
            return;
        }
        if (node.getText() != null) {
            builder.append(node.getText());
        }
        if (node.getChildren() == null) {
            return;
        }
        for (KnowledgeRichTextNode child : node.getChildren()) {
            appendNodeText(child, builder);
            if (builder.length() >= PREVIEW_MAX_LENGTH) {
                return;
            }
        }
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
