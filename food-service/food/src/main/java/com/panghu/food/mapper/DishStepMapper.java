package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.DishStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishStepMapper extends BaseMapper<DishStep> {
    List<DishStep> selectByDishId(@Param("dishId") String dishId);

    void deleteByDishId(@Param("dishId") String dishId);
}
