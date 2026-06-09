package com.panghu.food.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
        // 分类列表按 sort 升序返回，保证前端展示顺序稳定
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)
        );
        return categories.stream().map(category -> {
            Category dto = new Category();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setSort(category.getSort());
            return dto;
        }).collect(Collectors.toList());
    }
}
