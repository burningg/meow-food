package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.CirclePlanVisibleUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CirclePlanVisibleUserMapper extends BaseMapper<CirclePlanVisibleUser> {
    @Select("""
            SELECT CASE
                WHEN COUNT(*) = 0 THEN 1
                WHEN SUM(CASE WHEN user_id = #{userId} THEN 1 ELSE 0 END) > 0 THEN 1
                ELSE 0
            END
            FROM circle_plan_visible_user
            WHERE plan_id = #{planId}
            """)
    Integer canUserViewPlan(@Param("planId") String planId, @Param("userId") String userId);
}
