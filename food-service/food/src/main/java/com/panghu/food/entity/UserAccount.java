package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserAccount {
    private Long id;
    private String account;
    private String passwordHash;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private String phone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
