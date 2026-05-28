package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_request")
public class FriendRequest {
    private Long id;
    private Long requesterUserId;
    private Long targetUserId;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;
}
