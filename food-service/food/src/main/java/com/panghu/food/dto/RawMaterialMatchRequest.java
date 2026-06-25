package com.panghu.food.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RawMaterialMatchRequest {
    private List<String> names = new ArrayList<>();
}
