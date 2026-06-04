package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile_settings")
public class UserProfileSettings {
    @TableId
    private String userId;
    private String defaultMenuVisibility;
    private String lastSelectedCircleId;
    private Boolean allowFriendFeed;
    private LocalDateTime updatedAt;
}
