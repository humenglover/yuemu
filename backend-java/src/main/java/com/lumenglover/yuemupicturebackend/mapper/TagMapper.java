package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.Tag;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 鹿梦
* @description 针对表【tag(标签)】的数据库操作Mapper
* @createDate 2024-12-13 17:37:29
* @Entity generator.domain.Tag
*/
public interface TagMapper extends BaseMapper<Tag> {

    @Select("select tagName from tag where isDelete = 0")
    List<String> listTag();
}




