package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buddy_circle_member")
public class BuddyCircleMember {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String circleId;
    private String userId;
    private String role;
    private LocalDateTime joinedAt;
}
