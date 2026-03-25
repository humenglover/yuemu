package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.Category;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author 鹿梦
 * @description 针对表【category(分类)】的数据库操作Mapper
 * @createDate 2024-12-13 17:37:23
 * @Entity generator.domain.Category
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    @Select("select categoryName from category where isDelete = 0 and type = #{type}")
    List<String> listCategoryByType(@Param("type") Integer type);
}




