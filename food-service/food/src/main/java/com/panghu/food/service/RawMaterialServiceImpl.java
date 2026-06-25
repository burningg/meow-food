package com.panghu.food.service;

import com.panghu.food.entity.RawMaterial;
import com.panghu.food.mapper.RawMaterialMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RawMaterialServiceImpl implements RawMaterialService {
    private static final Logger log = LoggerFactory.getLogger(RawMaterialServiceImpl.class);

    private final RawMaterialMapper rawMaterialMapper;
    private final DishAiService dishAiService;

    public RawMaterialServiceImpl(RawMaterialMapper rawMaterialMapper, DishAiService dishAiService) {
        this.rawMaterialMapper = rawMaterialMapper;
        this.dishAiService = dishAiService;
    }

    @Override
    @Async
    public void ensureRawMaterialsAsync(List<String> ingredientNames) {
        try {
            List<String> names = normalizeNames(ingredientNames);
            if (names.isEmpty()) {
                return;
            }

            // 先批量按原名和常见名查出现有原材料，避免“蒜/大蒜”这类别名重复入库。
            Set<String> existingNames = new HashSet<>(rawMaterialMapper.selectMatchedNames(names));
            List<String> missingNames = names.stream()
                    .filter(name -> !existingNames.contains(name))
                    .collect(Collectors.toList());
            if (missingNames.isEmpty()) {
                return;
            }

            List<RawMaterial> aiMaterials = dishAiService.completeRawMaterials(missingNames);
            List<RawMaterial> materials = keepValidMissingMaterials(aiMaterials, new HashSet<>(missingNames));
            if (materials.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            for (RawMaterial material : materials) {
                material.setId(UUID.randomUUID().toString());
                material.setCreatedAt(now);
                material.setUpdatedAt(now);
            }
            rawMaterialMapper.insertIgnoreBatch(materials);
        } catch (Exception exception) {
            // 原材料资料是系统级沉淀，生成失败不能影响用户保存菜谱。
            log.warn("异步补全原材料信息失败", exception);
        }
    }

    private List<String> normalizeNames(List<String> ingredientNames) {
        if (ingredientNames == null) {
            return new ArrayList<>();
        }
        return ingredientNames.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<RawMaterial> keepValidMissingMaterials(List<RawMaterial> materials, Set<String> missingNames) {
        if (materials == null || materials.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, RawMaterial> materialByName = new LinkedHashMap<>();
        for (RawMaterial material : materials) {
            if (material == null || material.getName() == null) {
                continue;
            }
            String name = material.getName().trim();
            if (!missingNames.contains(name) || materialByName.containsKey(name)) {
                continue;
            }
            material.setName(name);
            materialByName.put(name, material);
        }
        return new ArrayList<>(materialByName.values());
    }
}
