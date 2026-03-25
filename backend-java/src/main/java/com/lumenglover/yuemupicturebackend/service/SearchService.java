package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.dto.search.SearchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.UserSearchRecord;
import com.lumenglover.yuemupicturebackend.model.vo.HotSearchVO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SearchService {
    /**
     * 获取热门搜索关键词（仅关键词列表）
     */
    List<String> getHotSearchKeywords(String type, Integer size);

    /**
     * 获取热门搜索关键词（详细信息）
     */
    List<HotSearchVO> getHotSearchWithDetails(String type, Integer size);

    /**
     * 统一搜索接口
     * @param searchRequest
     * @return
     */
    Page<?> doSearch(SearchRequest searchRequest);

    /**
     * 获取搜索建议
     * @param searchText 搜索文本
     * @param type 搜索类型
     * @param size 返回数量
     * @return 搜索建议列表
     */
    List<HotSearchVO> getSuggestions(String searchText, String type, Integer size);

    /**
     * 获取用户搜索历史
     * @param userId 用户ID
     * @param type 搜索类型
     * @param size 返回数量
     * @return 搜索历史列表
     */
    List<UserSearchRecord> getUserSearchHistory(Long userId, String type, Integer size);

    /**
     * 删除用户指定类型的搜索历史记录
     * @param userId 用户ID
     * @param type 搜索类型
     * @return 是否删除成功
     */
    boolean deleteUserSearchHistoryByType(Long userId, String type);

    /**
     * 删除用户所有搜索历史记录
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUserSearchHistory(Long userId);

    /**
     * 获取猜你想搜的数据
     * @param userId 用户ID，可为空
     * @param type 搜索类型
     * @param size 返回数量
     * @return 推荐搜索词列表
     */
    List<HotSearchVO> getGuessYouWantToSearch(Long userId, String type, Integer size);
}
