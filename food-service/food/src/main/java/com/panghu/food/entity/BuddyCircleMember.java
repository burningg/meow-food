package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buddy_circle_member")
public class BuddyCircleMember {
    private Long id;
    private Long circleId;
    private Long userId;
    private String role;
    private LocalDateTime joinedAt;
}
