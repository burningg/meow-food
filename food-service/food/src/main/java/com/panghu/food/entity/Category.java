package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class Category {
    private String id;

    private String name;

    private Integer sort = 0;

    private LocalDateTime createdAt;
}
