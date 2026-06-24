package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.CirclePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CirclePlanMapper extends BaseMapper<CirclePlan> {
    @Select("SELECT MAX(created_at) FROM circle_plan WHERE creator_user_id = #{userId}")
    LocalDateTime selectLatestCreatedAtByCreatorUserId(@Param("userId") String userId);

    @Select("""
            SELECT COUNT(cpr.id)
            FROM circle_plan cp
            INNER JOIN circle_plan_recipe cpr ON cpr.plan_id = cp.id
            WHERE cp.creator_user_id = #{userId}
              AND cp.plan_date BETWEEN #{startDate} AND #{endDate}
            """)
    Long countRecipesCreatedByUserInPlanDateRange(@Param("userId") String userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT DISTINCT cp.*
            FROM circle_plan cp
            INNER JOIN circle_plan_recipe cpr ON cpr.plan_id = cp.id
            WHERE cpr.added_by_user_id = #{userId}
              AND cp.plan_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY cp.plan_date ASC, cp.created_at ASC, cp.id ASC
            """)
    List<CirclePlan> selectPlansAddedByUserInPlanDateRange(@Param("userId") String userId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);
}
