package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.DishIngredient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishIngredientMapper extends BaseMapper<DishIngredient> {
    List<DishIngredient> selectByDishId(@Param("dishId") String dishId);

    void deleteByDishId(@Param("dishId") String dishId);
}
