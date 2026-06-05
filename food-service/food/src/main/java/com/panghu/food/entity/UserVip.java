package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_vip")
public class UserVip {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String vipLevel;
    private Boolean isVip;
    private LocalDateTime openedAt;
    private LocalDateTime expiresAt;
    private BigDecimal openAmount;
    private Integer dailyRecipeAnalysisLimit;
    private Integer dailyRecipeAnalysisUsed;
    private LocalDate dailyRecipeAnalysisDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
