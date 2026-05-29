package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserAccount {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
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
