package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.RawMaterial;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RawMaterialMapper extends BaseMapper<RawMaterial> {
    List<String> selectMatchedNames(@Param("names") List<String> names);

    int insertIgnoreBatch(@Param("materials") List<RawMaterial> materials);
}
