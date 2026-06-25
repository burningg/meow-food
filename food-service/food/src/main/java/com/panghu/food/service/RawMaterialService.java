package com.panghu.food.service;

import com.panghu.food.dto.RawMaterialResponse;

import java.util.List;

public interface RawMaterialService {
    void ensureRawMaterialsAsync(List<String> ingredientNames);

    List<RawMaterialResponse> findMatchedRawMaterials(List<String> ingredientNames);
}
