package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("circle_plan")
public class CirclePlan {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String circleId;
    private LocalDate planDate;
    private String title;
    private String creatorUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
