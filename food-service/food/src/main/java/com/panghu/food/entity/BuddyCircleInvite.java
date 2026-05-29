package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buddy_circle_invite")
public class BuddyCircleInvite {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String circleId;
    private String inviterUserId;
    private String inviteeUserId;
    private String status;
    private LocalDateTime createdAt;
}
