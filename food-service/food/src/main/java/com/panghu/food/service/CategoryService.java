package com.panghu.food.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.panghu.food.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    List<Category> getAllCategories();
}