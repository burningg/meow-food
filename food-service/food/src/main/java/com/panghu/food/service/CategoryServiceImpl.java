package com.panghu.food.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.panghu.food.entity.Category;
import com.panghu.food.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getAllCategories() {
        List<Category> categories = categoryMapper.selectList(null);
        return categories.stream().map(category -> {
            Category dto = new Category();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setSort(category.getSort());
            return dto;
        }).collect(Collectors.toList());
    }
}
