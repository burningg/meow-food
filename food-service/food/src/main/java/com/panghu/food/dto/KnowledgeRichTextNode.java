package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class KnowledgeRichTextNode {
    private String type;
    private String name;
    private String text;
    private Map<String, String> attrs = new LinkedHashMap<>();
    private List<KnowledgeRichTextNode> children = new ArrayList<>();
}
