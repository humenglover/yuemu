package com.lumenglover.yuemupicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lumenglover.yuemupicturebackend.model.entity.AiChat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @description 针对表【aichat(聊天消息表)】的数据库操作Mapper
 */
@Mapper
public interface AiChatMapper extends BaseMapper<AiChat> {
    void batchInsert(List<AiChat> chatMessages);
}
