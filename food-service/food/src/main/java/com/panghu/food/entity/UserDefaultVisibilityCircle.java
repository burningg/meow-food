package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_default_visibility_circle")
public class UserDefaultVisibilityCircle {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String circleId;
    private LocalDateTime createdAt;
}
