package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buddy_circle_invite")
public class BuddyCircleInvite {
    private Long id;
    private Long circleId;
    private Long inviterUserId;
    private Long inviteeUserId;
    private String status;
    private LocalDateTime createdAt;
}
