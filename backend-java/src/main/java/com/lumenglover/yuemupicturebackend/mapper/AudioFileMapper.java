package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 音频文件Mapper接口
 */
@Mapper
public interface AudioFileMapper extends BaseMapper<AudioFile> {
}
