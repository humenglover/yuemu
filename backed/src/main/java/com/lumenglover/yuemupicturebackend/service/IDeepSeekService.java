package com.lumenglover.yuemupicturebackend.service;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.lumenglover.yuemupicturebackend.model.vo.AiChatVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IDeepSeekService {

    /**
     * 提问生成回复
     */
    String generateResponse(String query, HttpServletRequest request);

    Page<AiChatVO> getChatHistory(HttpServletRequest request, int current, int pageSize);

}
