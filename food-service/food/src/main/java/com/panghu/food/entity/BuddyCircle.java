package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("buddy_circle")
public class BuddyCircle {
    private Long id;
    private String name;
    private String description;
    private Long ownerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
