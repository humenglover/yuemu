package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.common.PageRequest;
import com.lumenglover.yuemupicturebackend.model.entity.Tag;
import com.lumenglover.yuemupicturebackend.model.vo.TagVO;

import java.util.List;

/**
* @author 鹿梦
* @description 针对表【tag(标签)】的数据库操作Service
* @createDate 2024-12-13 17:37:29
*/
public interface TagService extends IService<Tag> {

    List<String> listTag();

    TagVO getTagVO(Tag tag);

    List<TagVO> listTagVOByPage(List<Tag> records);

    Boolean addTag(String tagName);

    Boolean deleteTag(Long id);

    List<TagVO> searchTag(String tagName);
}
