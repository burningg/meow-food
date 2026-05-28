package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile_settings")
public class UserProfileSettings {
    @TableId
    private Long userId;
    private String defaultMenuVisibility;
    private Boolean allowFriendFeed;
    private LocalDateTime updatedAt;
}
