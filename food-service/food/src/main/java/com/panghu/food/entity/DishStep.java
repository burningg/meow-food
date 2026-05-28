package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("dish_step")
public class DishStep {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String dishId;

    private Integer stepNo;

    private String content;
}
