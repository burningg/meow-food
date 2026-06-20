package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.dto.DishDetailResponse;
import com.panghu.food.dto.DishSummaryResponse;
import com.panghu.food.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    List<DishSummaryResponse> selectByCategoryId(@Param("categoryId") String categoryId,
                                                 @Param("ownerUserId") String ownerUserId);

    DishDetailResponse selectDishDetailById(@Param("id") String id);

    List<DishSummaryResponse> selectFeatured(@Param("categoryId") String categoryId, @Param("limit") int limit);

    List<DishSummaryResponse> selectRecent(@Param("limit") int limit);

    List<DishSummaryResponse> selectByOwnerUserId(@Param("ownerUserId") String ownerUserId);

    List<DishSummaryResponse> selectVisibleByOwnerUserIdAndCircleId(@Param("ownerUserId") String ownerUserId,
                                                                    @Param("circleId") String circleId);

    List<DishSummaryResponse> selectAllActive();

    List<DishSummaryResponse> selectByIds(@Param("ids") List<String> ids);

    @Select("SELECT MAX(created_at) FROM dish WHERE owner_user_id = #{ownerUserId} AND status = 1")
    LocalDateTime selectLatestCreatedAtByOwnerUserId(@Param("ownerUserId") String ownerUserId);
}
