package com.panghu.food.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("raw_material")
public class RawMaterial {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String name;

    private String commonNames;

    private String steamTime;

    private String boilTime;

    private String fryTime;

    private String bakeTime;

    private String stirFryTime;

    private String defaultHeatTemperature;

    private String allergenFlag;

    private String nutritionInfo;

    private String substituteIngredients;

    private String category;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
