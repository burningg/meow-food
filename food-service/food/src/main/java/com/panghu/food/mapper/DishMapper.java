package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    List<DishSummaryResponse> selectByCategoryId(@Param("categoryId") String categoryId,
                                                 @Param("ownerUserId") Long ownerUserId);

    DishDetailResponse selectDishDetailById(@Param("id") String id);

    List<DishSummaryResponse> selectFeatured(@Param("categoryId") String categoryId, @Param("limit") int limit);

    List<DishSummaryResponse> selectRecent(@Param("limit") int limit);

    List<DishSummaryResponse> selectByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    List<DishSummaryResponse> selectAllActive();
}
