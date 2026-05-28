package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_relation")
public class FriendRelation {
    private Long id;
    private Long userId;
    private Long friendUserId;
    private LocalDateTime createdAt;
}
