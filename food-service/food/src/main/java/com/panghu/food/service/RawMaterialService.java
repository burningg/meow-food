package com.panghu.food.service;

import java.util.List;

public interface RawMaterialService {
    void ensureRawMaterialsAsync(List<String> ingredientNames);
}
