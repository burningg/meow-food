package com.panghu.food.web;

import com.panghu.food.dto.KnowledgeArticleDetailResponse;
import com.panghu.food.dto.KnowledgeArticleListResponse;
import com.panghu.food.service.KnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ResponseEntity<KnowledgeArticleListResponse> getArticles(@RequestParam(required = false) Integer page,
                                                                    @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(knowledgeService.getPublishedArticles(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeArticleDetailResponse> getArticle(@PathVariable String id) {
        return ResponseEntity.ok(knowledgeService.getArticle(id));
    }
}
