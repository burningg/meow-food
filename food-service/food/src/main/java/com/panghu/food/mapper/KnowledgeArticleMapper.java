package com.panghu.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.panghu.food.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeArticleMapper extends BaseMapper<KnowledgeArticle> {
    List<KnowledgeArticle> selectPublishedByPage(@Param("limit") int limit, @Param("offset") int offset);

    KnowledgeArticle selectPublishedById(@Param("id") String id);
}
