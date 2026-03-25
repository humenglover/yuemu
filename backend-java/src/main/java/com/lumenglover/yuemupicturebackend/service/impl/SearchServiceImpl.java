package com.lumenglover.yuemupicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.lumenglover.yuemupicturebackend.esdao.EsSearchKeywordDao;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.HotSearchMapper;
import com.lumenglover.yuemupicturebackend.mapper.UserSearchRecordMapper;
import com.lumenglover.yuemupicturebackend.model.dto.search.SearchRequest;
import com.lumenglover.yuemupicturebackend.model.entity.*;
import com.lumenglover.yuemupicturebackend.model.entity.es.EsSearchKeyword;
import com.lumenglover.yuemupicturebackend.model.enums.SpaceLevelEnum;
import com.lumenglover.yuemupicturebackend.model.vo.*;
import com.lumenglover.yuemupicturebackend.service.*;
import com.lumenglover.yuemupicturebackend.utils.DomainReplaceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    private static final String PICTURE_INDEX = "picture";
    private static final String USER_INDEX = "user";
    private static final String POST_INDEX = "post";
    private static final String SPACE_INDEX = "space";
    private static final String SEARCH_KEYWORD_INDEX = "search_keyword";
    private static final String HOT_SEARCH_CACHE_KEY = "hot_search:%s";
    private static final long CACHE_EXPIRE_TIME = 15 * 60;  // 15分钟
    private static final String HOT_SEARCH_REALTIME_KEY = "hot:search:realTime:%s"; // ZSet

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private UserService userService;

    @Resource
    private PostAttachmentService postAttachmentService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private EsSearchKeywordDao esSearchKeywordDao;

    @Resource
    private HotSearchMapper hotSearchMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserSearchRecordMapper userSearchRecordMapper;

    @Resource
    private UserFollowsService userfollowsService;

    /**
     * 记录搜索关键词
     */
    private void recordSearchKeyword(String searchText, String type) {
        try {
            // 实时计数：Redis ZSet 记录24h搜索量
            try {
                String zsetKey = String.format(HOT_SEARCH_REALTIME_KEY, StringUtils.isBlank(type) ? "all" : type);
                stringRedisTemplate.opsForZSet().incrementScore(zsetKey, searchText, 1d);
                // 设置过期时间为24小时滚动窗口
                stringRedisTemplate.expire(zsetKey, java.time.Duration.ofHours(24));
            } catch (Exception e) {
                log.warn("实时搜索量记录失败 type={}, keyword={}", type, searchText, e);
            }

            // 查找是否存在该关键词记录（仅在ES中查找，不立即更新）
            EsSearchKeyword keyword = esSearchKeywordDao.findByTypeAndKeyword(type, searchText);

            if (keyword != null) {
                // 更新搜索次数（仅在Redis中记录增量）
                stringRedisTemplate.boundHashOps("es_keyword_increment:" + type)
                        .increment(searchText, 1);
            } else {
                // 新增关键词记录（在Redis中记录新词）
                stringRedisTemplate.boundSetOps("es_keyword_new:" + type)
                        .add(searchText);
            }
        } catch (Exception e) {
            log.error("记录搜索关键词失败", e);
        }
    }

    /**
     * 记录用户搜索关键词
     */
    private void recordUserSearchKeyword(String searchText, String type, Long userId) {
        try {
            // 将用户搜索记录暂存到Redis中，等待批量处理
            UserSearchRecord record = new UserSearchRecord();
            record.setUserId(userId);
            record.setKeyword(searchText);
            record.setType(type);
            Date now = new Date();
            record.setSearchTime(now);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setIsDelete(0);

            // 将记录序列化为字符串并存储到Redis列表中
            String recordStr = String.format("%d|%s|%s|%d|%d",
                    record.getUserId(),
                    record.getKeyword(),
                    record.getType(),
                    record.getSearchTime().getTime(),
                    record.getCreateTime().getTime());

            stringRedisTemplate.boundListOps("user:search:record").leftPush(recordStr);
        } catch (Exception e) {
            log.error("暂存用户搜索关键词失败, userId={}, keyword={}, type={}", userId, searchText, type, e);
        }
    }

    /**
     * 获取热门搜索（优先从Redis获取，其次MySQL，最后ES）
     */
    @Override
    public List<String> getHotSearchKeywords(String type, Integer size) {
        String cacheKey = String.format(HOT_SEARCH_CACHE_KEY, type);
        try {
            // 0. 先从Redis缓存获取综合计算后的热门词
            List<String> cachedList = stringRedisTemplate.opsForList().range(cacheKey, 0, size - 1);
            if (cachedList != null && !cachedList.isEmpty()) {
                return cachedList;
            }

            // 1. 综合计算Redis实时数据和MySQL历史数据
            List<HotSearchVO> combinedList = getCombinedHotSearchData(type, size);

            // 提取关键词列表
            List<String> resultList = combinedList.stream()
                    .map(HotSearchVO::getKeyword)
                    .collect(Collectors.toList());

            // 如果没有结果，返回默认的热门搜索词
            if (resultList.isEmpty()) {
                resultList = getDefaultHotSearchKeywords(type, size);
            }

            // 更新缓存
            if (!resultList.isEmpty()) {
                updateHotSearchCache(type, resultList);
            }

            return resultList;
        } catch (Exception e) {
            log.error("获取热门搜索失败", e);
            // 出现异常时返回默认热门搜索词
            return getDefaultHotSearchKeywords(type, size);
        }
    }

    /**
     * 综合计算Redis实时数据和MySQL历史数据
     */
    private List<HotSearchVO> getCombinedHotSearchData(String type, Integer size) {
        List<HotSearchVO> result = new ArrayList<>();
        Map<String, HotSearchVO> keywordMap = new HashMap<>();

        try {
            // 1. 从Redis实时热门搜索获取（权重70%）
            String zsetKey = String.format(HOT_SEARCH_REALTIME_KEY, StringUtils.isBlank(type) ? "all" : type);
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> zsetTop =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, size * 2 - 1);

            if (zsetTop != null && !zsetTop.isEmpty()) {
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : zsetTop) {
                    HotSearchVO vo = new HotSearchVO();
                    vo.setKeyword(tuple.getValue());
                    vo.setRealTimeCount(tuple.getScore() != null ? tuple.getScore().longValue() : 0L);
                    // Redis中只有实时数据，历史数据暂时设为0
                    vo.setCount(0L);
                    vo.setTrend(0.0);
                    // 计算分数：实时搜索量 * 0.7
                    vo.setScore(tuple.getScore() != null ? tuple.getScore() * 0.7 : 0.0);
                    keywordMap.put(tuple.getValue(), vo);
                }
            }

            // 2. 从MySQL获取历史热门搜索（权重30%）
            // 先尝试从Redis缓存中获取MySQL热门词
            String mysqlTopKey = String.format("hot_search:mysql_top:%s", type);
            Map<Object, Object> mysqlTopMap = stringRedisTemplate.opsForHash().entries(mysqlTopKey);

            if (mysqlTopMap != null && !mysqlTopMap.isEmpty()) {
                // 从Redis缓存中获取MySQL热门词
                for (Map.Entry<Object, Object> entry : mysqlTopMap.entrySet()) {
                    String keyword = (String) entry.getKey();
                    Long count = Long.parseLong((String) entry.getValue());

                    HotSearchVO vo = keywordMap.get(keyword);
                    if (vo == null) {
                        vo = new HotSearchVO();
                        vo.setKeyword(keyword);
                        vo.setRealTimeCount(0L);
                        vo.setTrend(0.0);
                        // 只有历史数据，实时数据为0
                        vo.setCount(count);
                        // 计算分数：历史搜索量 * 0.3
                        vo.setScore(count != null ? count * 0.3 : 0.0);
                        keywordMap.put(keyword, vo);
                    } else {
                        // 更新已存在的词，加上历史搜索量的贡献
                        vo.setCount(count);
                        // 重新计算综合分数：实时搜索量 * 0.7 + 历史搜索量 * 0.3
                        double score = (vo.getRealTimeCount() * 0.7) + (count != null ? count * 0.3 : 0.0);
                        vo.setScore(score);
                    }
                }
            } else {
                // Redis缓存中没有MySQL热门词，直接从MySQL获取
                Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
                List<HotSearch> hotSearchList = hotSearchMapper.getHotSearchAfter(type, startTime, size * 2);

                for (HotSearch hotSearch : hotSearchList) {
                    HotSearchVO vo = keywordMap.get(hotSearch.getKeyword());
                    if (vo == null) {
                        vo = new HotSearchVO();
                        vo.setKeyword(hotSearch.getKeyword());
                        vo.setRealTimeCount(0L);
                        vo.setTrend(hotSearch.getTrend() != null ? hotSearch.getTrend() : 0.0);
                        // 只有历史数据，实时数据为0
                        vo.setCount(hotSearch.getCount());
                        // 计算分数：历史搜索量 * 0.3
                        vo.setScore(hotSearch.getCount() != null ? hotSearch.getCount() * 0.3 : 0.0);
                        keywordMap.put(hotSearch.getKeyword(), vo);
                    } else {
                        // 更新已存在的词，加上历史搜索量的贡献
                        vo.setCount(hotSearch.getCount());
                        vo.setTrend(hotSearch.getTrend() != null ? hotSearch.getTrend() : 0.0);
                        // 重新计算综合分数：实时搜索量 * 0.7 + 历史搜索量 * 0.3
                        double score = (vo.getRealTimeCount() * 0.7) + (hotSearch.getCount() != null ? hotSearch.getCount() * 0.3 : 0.0);
                        vo.setScore(score);
                    }
                }
            }

            // 转换为列表并排序
            result.addAll(keywordMap.values());
            result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 限制返回数量
            if (result.size() > size) {
                result = result.subList(0, size);
            }
        } catch (Exception e) {
            log.error("综合计算热门搜索数据失败", e);
        }

        return result;
    }

    /**
     * 获取热门搜索（详细信息）
     */
    @Override
    public List<HotSearchVO> getHotSearchWithDetails(String type, Integer size) {
        List<HotSearchVO> result = new ArrayList<>();
        try {
            // 综合计算Redis实时数据和MySQL历史数据
            result = getCombinedHotSearchData(type, size);

            // 如果仍然没有数据，使用默认数据
            if (result.isEmpty()) {
                List<String> defaultKeywords = getDefaultHotSearchKeywords(type, size);
                for (String keyword : defaultKeywords) {
                    HotSearchVO vo = new HotSearchVO();
                    vo.setKeyword(keyword);
                    vo.setCount(0L);
                    vo.setRealTimeCount(0L);
                    vo.setTrend(0.0);
                    vo.setScore(0.0);
                    vo.setReason("默认推荐");
                    result.add(vo);
                }
            }

            // 按分数排序
            result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 限制返回数量
            if (result.size() > size) {
                result = result.subList(0, size);
            }

            // 设置标签
            for (HotSearchVO vo : result) {
                if (vo.getTrend() != null && vo.getTrend() > 0.5) {
                    vo.setReason("上升");
                } else if (vo.getTrend() != null && vo.getTrend() < -0.3) {
                    vo.setReason("下降");
                } else if (vo.getScore() != null && vo.getScore() > 100) {
                    vo.setReason("热门");
                } else if (vo.getReason() == null) {
                    vo.setReason("稳定");
                }
            }
        } catch (Exception e) {
            log.error("获取热门搜索详细信息失败", e);
            // 出现异常时返回默认数据
            try {
                List<String> defaultKeywords = getDefaultHotSearchKeywords(type, size);
                for (String keyword : defaultKeywords) {
                    HotSearchVO vo = new HotSearchVO();
                    vo.setKeyword(keyword);
                    vo.setCount(0L);
                    vo.setRealTimeCount(0L);
                    vo.setTrend(0.0);
                    vo.setScore(0.0);
                    vo.setReason("默认推荐");
                    result.add(vo);
                }
            } catch (Exception ex) {
                log.error("获取默认热门搜索失败", ex);
            }
        }

        return result;
    }

    /**
     * 获取默认的热门搜索词
     */
    private List<String> getDefaultHotSearchKeywords(String type, Integer size) {
        try {
            // 从MySQL中获取搜索次数最多的记录
            List<HotSearch> hotSearchList = hotSearchMapper.selectList(
                    new QueryWrapper<HotSearch>()
                            .eq("type", type)
                            .eq("isDelete", 0)
                            .orderByDesc("count")
                            .last("LIMIT " + size)
            );

            // 如果有数据，返回搜索次数最多的关键词
            if (!hotSearchList.isEmpty()) {
                return hotSearchList.stream()
                        .map(HotSearch::getKeyword)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("从MySQL获取默认热门搜索词失败，使用预设默认词", e);
        }

        // 如果MySQL中没有数据或查询失败，返回预设的默认关键词
        List<String> defaultKeywords;
        if ("space".equals(type) || "user".equals(type)) {
            // 对于空间和用户，返回默认关键词 "鹿梦"
            defaultKeywords = Arrays.asList("鹿梦");
        } else {
            // 对于帖子和图片，返回与图片相关的默认关键词
            defaultKeywords = Arrays.asList(
                    "风景", "动物", "城市", "自然", "艺术",
                    "黑白", "抽象", "肖像", "摄影", "插画",
                    "时尚", "美食", "旅行", "运动", "科技"
            );
        }

        // 限制返回数量
        return defaultKeywords.stream().limit(size).collect(Collectors.toList());
    }

    /**
     * 更新热门搜索缓存
     */
    private void updateHotSearchCache(String type, List<String> keywords) {
        String cacheKey = String.format(HOT_SEARCH_CACHE_KEY, type);
        try {
            // 使用RedisTemplate的直接操作方式
            // 删除旧的缓存
            stringRedisTemplate.delete(cacheKey);

            // 添加新的缓存
            if (!keywords.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(cacheKey, keywords);
            }

            // 设置过期时间（13-15分钟随机）
            long expireSeconds = CACHE_EXPIRE_TIME + RandomUtil.randomInt(0, 120);
            stringRedisTemplate.expire(cacheKey, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("更新热门搜索缓存失败", e);
        }
    }

    @Override
    public Page<?> doSearch(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        String type = searchRequest.getType();
        Long userId = searchRequest.getUserId(); // 获取用户ID

        // 校验参数
        if (StringUtils.isBlank(searchText)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        // 记录搜索关键词
        recordSearchKeyword(searchText, type);

        // 如果有用户ID，记录用户搜索记录
        if (userId != null) {
            recordUserSearchKeyword(searchText, type, userId);
        }

        // 执行搜索
        switch (type) {
            case "picture":
                return searchPicture(searchRequest);
            case "user":
                return searchUser(searchRequest);
            case "post":
                return searchPost(searchRequest);
            case "space":
                return searchSpace(searchRequest);
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }
    }

    /**
     * 搜索图片
     */
    private Page<PictureVO> searchPicture(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();

        // 构建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                // 搜索条件 - 使用多字段搜索提高匹配度
                .should(QueryBuilders.multiMatchQuery(searchText, "name", "name.ik", "name.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "introduction", "introduction.ik", "introduction.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "tags", "tags.ik", "tags.standard"))
                // 大幅提高名称精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("name", searchText).boost(5.0f))
                // 提高名称普通匹配的权重
                .should(QueryBuilders.matchQuery("name", searchText).boost(3.0f))
                // 提高简介精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("introduction", searchText).boost(2.0f))
                // 处理数字搜索 - 如果搜索文本是数字，添加对ID的精确匹配
                .should(QueryBuilders.wildcardQuery("name", "*" + searchText + "*").boost(2.5f))
                .should(QueryBuilders.wildcardQuery("introduction", "*" + searchText + "*").boost(1.5f))
                .should(QueryBuilders.wildcardQuery("tags", "*" + searchText + "*").boost(1.2f));

        // 尝试将搜索文本转换为图片ID
        try {
            Long pictureId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", pictureId));
        } catch (NumberFormatException ignored) {
            // 如果不是数字，尝试在名称中搜索包含该数字的记录
            if (searchText.matches(".*\\d+.*")) {
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("name", "*" + searchText + "*").boost(3.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("introduction", "*" + searchText + "*").boost(2.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("tags", "*" + searchText + "*").boost(1.5f));
            }
        }
        boolQueryBuilder.minimumShouldMatch(1)
                // 必要条件：已通过审核、未删除、非草稿
                .must(QueryBuilders.termQuery("reviewStatus", 1))
                .must(QueryBuilders.termQuery("isDelete", 0))
                .must(QueryBuilders.termQuery("isDraft", 0))
                // 只搜索公共空间的图片（spaceId为null）
                .mustNot(QueryBuilders.existsQuery("spaceId"));

        // 构建搜索查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(current - 1, pageSize))
                // 按匹配度和创建时间排序
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .build();

        // 执行搜索
        SearchHits<Picture> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                Picture.class,
                IndexCoordinates.of(PICTURE_INDEX)
        );

        // 获取搜索结果并转换为PictureVO
        List<PictureVO> pictureVOList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(picture -> {
                    PictureVO pictureVO = new PictureVO();
                    pictureVO.setId(picture.getId());
                    pictureVO.setUserId(picture.getUserId());
                    pictureVO.setName(picture.getName());
                    pictureVO.setIntroduction(picture.getIntroduction());
                    pictureVO.setUrl(DomainReplaceUtil.replaceDomain(picture.getUrl()));
                    // 修复tags字段类型转换问题
                    if (picture.getTags() != null) {
                        pictureVO.setTags(Arrays.asList(picture.getTags().split(",")));
                    } else {
                        pictureVO.setTags(new ArrayList<>());
                    }
                    pictureVO.setReviewStatus(picture.getReviewStatus());
                    pictureVO.setCreateTime(picture.getCreateTime());
                    pictureVO.setUpdateTime(picture.getUpdateTime());

                    // 添加统计字段
                    pictureVO.setViewCount(picture.getViewCount());
                    pictureVO.setCommentCount(picture.getCommentCount());
                    pictureVO.setLikeCount(picture.getLikeCount());
                    pictureVO.setShareCount(picture.getShareCount());
                    pictureVO.setPicSize(picture.getPicSize());
                    pictureVO.setPicWidth(picture.getPicWidth());
                    pictureVO.setPicHeight(picture.getPicHeight());
                    pictureVO.setPicScale(picture.getPicScale());
                    pictureVO.setPicFormat(picture.getPicFormat());
                    pictureVO.setPicColor(picture.getPicColor());
                    pictureVO.setThumbnailUrl(picture.getThumbnailUrl());
                    pictureVO.setCategory(picture.getCategory());
                    pictureVO.setSpaceId(picture.getSpaceId());
                    pictureVO.setEditTime(picture.getEditTime());
                    pictureVO.setReviewMessage(picture.getReviewMessage());

                    // 获取并设置用户信息
                    User user = userService.getById(picture.getUserId());
                    if (user != null) {
                        pictureVO.setUser(userService.getUserVO(user));
                    }

                    return pictureVO;
                })
                .collect(Collectors.toList());

        return new PageImpl<PictureVO>(pictureVOList, PageRequest.of(current - 1, pageSize), searchHits.getTotalHits());
    }

    /**
     * 搜索用户
     */
    private Page<UserVO> searchUser(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();

        // 构建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.multiMatchQuery(searchText, "userName", "userName.ik", "userName.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "userAccount", "userAccount.ik", "userAccount.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "userProfile", "userProfile.ik", "userProfile.standard"))
                // 大幅提高用户名精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("userName", searchText).boost(5.0f))
                // 提高账号名精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("userAccount", searchText).boost(5.0f))
                // 提高用户名普通匹配的权重
                .should(QueryBuilders.matchQuery("userName", searchText).boost(3.0f))
                // 提高账号名普通匹配的权重
                .should(QueryBuilders.matchQuery("userAccount", searchText).boost(3.0f))
                // 处理数字和英文搜索
                .should(QueryBuilders.wildcardQuery("userName", "*" + searchText + "*").boost(2.5f))
                .should(QueryBuilders.wildcardQuery("userAccount", "*" + searchText + "*").boost(2.5f))
                .should(QueryBuilders.wildcardQuery("userProfile", "*" + searchText + "*").boost(1.5f));

        // 尝试将搜索文本转换为用户ID
        try {
            Long userId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", userId));
        } catch (NumberFormatException ignored) {
            // 如果不是数字，尝试在字段中搜索包含该数字的记录
            if (searchText.matches(".*\\d+.*")) {
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("userName", "*" + searchText + "*").boost(3.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("userAccount", "*" + searchText + "*").boost(3.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("userProfile", "*" + searchText + "*").boost(2.0f));
            }
        }

        boolQueryBuilder.minimumShouldMatch(1)
                .must(QueryBuilders.termQuery("isDelete", 0));

        // 构建搜索查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(current - 1, pageSize))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .build();

        // 执行搜索
        SearchHits<User> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                User.class,
                IndexCoordinates.of(USER_INDEX)
        );

        // 获取搜索结果并转换为UserVO
        List<UserVO> userVOList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(user -> {
                    UserVO userVO = userService.getUserVO(user);  // 使用UserService的脱敏方法
                    // 获取并设置用户的关注和粉丝数量
                    FollowersAndFansVO followersAndFansVO = userfollowsService.getFollowAndFansCount(user.getId());
                    if (followersAndFansVO != null) {
                        userVO.setFansCount(followersAndFansVO.getFansCount());
                        userVO.setFollowCount(followersAndFansVO.getFollowCount());
                    }
                    return userVO;
                })
                .collect(Collectors.toList());

        return new PageImpl<UserVO>(userVOList, PageRequest.of(current - 1, pageSize), searchHits.getTotalHits());
    }

    /**
     * 搜索帖子
     */
    private Page<Post> searchPost(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();

        // 构建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.multiMatchQuery(searchText, "title", "title.ik", "title.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "content", "content.ik", "content.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "tags", "tags.ik", "tags.standard"))
                .should(QueryBuilders.multiMatchQuery(searchText, "category", "category.ik", "category.standard"))
                // 大幅提高标题精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("title", searchText).boost(5.0f))
                // 提高标题普通匹配的权重
                .should(QueryBuilders.matchQuery("title", searchText).boost(3.0f))
                // 提高内容精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("content", searchText).boost(2.0f))
                // 处理数字和英文搜索
                .should(QueryBuilders.wildcardQuery("title", "*" + searchText + "*").boost(2.5f))
                .should(QueryBuilders.wildcardQuery("content", "*" + searchText + "*").boost(1.5f))
                .should(QueryBuilders.wildcardQuery("tags", "*" + searchText + "*").boost(1.2f))
                .should(QueryBuilders.wildcardQuery("category", "*" + searchText + "*").boost(1.2f));

        // 尝试将搜索文本转换为帖子ID
        try {
            Long postId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", postId));
        } catch (NumberFormatException ignored) {
            // 如果不是数字，尝试在字段中搜索包含该数字的记录
            if (searchText.matches(".*\\d+.*")) {
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("title", "*" + searchText + "*").boost(3.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("content", "*" + searchText + "*").boost(2.0f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("tags", "*" + searchText + "*").boost(1.5f));
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("category", "*" + searchText + "*").boost(1.5f));
            }
        }
        boolQueryBuilder.minimumShouldMatch(1)
                .must(QueryBuilders.termQuery("isDelete", 0))
                .must(QueryBuilders.termQuery("status", 1)); // 只搜索已发布的帖子

        // 修改排序逻辑，优先考虑标题匹配度，然后是综合权重
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(current - 1, pageSize))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                // 标题匹配度权重最高
                .withSort(SortBuilders.fieldSort("_score").order(SortOrder.DESC))
                // 使用加权排序：点赞数30%，评论数25%，浏览量20%，分享数15%，标题匹配度10%
                .withSort(SortBuilders.scriptSort(
                        new Script(
                                ScriptType.INLINE,
                                "painless",
                                "doc['likeCount'].value * 0.3 + doc['commentCount'].value * 0.25 + " +
                                        "doc['viewCount'].value * 0.2 + doc['shareCount'].value * 0.15 + " +
                                        "_score * 0.1",
                                new HashMap<>()
                        ),
                        ScriptSortBuilder.ScriptSortType.NUMBER
                ).order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .build();

        // 执行搜索
        SearchHits<Post> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                Post.class,
                IndexCoordinates.of(POST_INDEX)
        );

        // 获取搜索结果
        List<Post> postList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 批量获取帖子的附件
        if (!postList.isEmpty()) {
            // 获取所有帖子ID
            List<Long> postIds = postList.stream()
                    .map(Post::getId)
                    .collect(Collectors.toList());

            // 批量查询附件
            List<PostAttachment> attachments = postAttachmentService.list(
                    new QueryWrapper<PostAttachment>()
                            .in("postId", postIds)
                            .eq("type", 1)  // 只获取图片类型的附件
                            .orderByAsc("position")  // 按位置排序
            );

            // 构建帖子ID到附件列表的映射
            Map<Long, List<PostAttachment>> postAttachmentMap = attachments.stream()
                    .collect(Collectors.groupingBy(PostAttachment::getPostId));

            // 填充帖子信息
            postList.forEach(post -> {
                // 获取并设置用户信息
                User user = userService.getById(post.getUserId());
                if (user != null) {
                    post.setUser(userService.getUserVO(user));
                }

                // 清空内容，只在详情页显示
                post.setContent(null);
            });
        }

        return new PageImpl<>(postList, PageRequest.of(current - 1, pageSize), searchHits.getTotalHits());
    }

    /**
     * 搜索空间
     */
    private Page<SpaceVO> searchSpace(SearchRequest searchRequest) {
        String searchText = searchRequest.getSearchText();
        Integer current = searchRequest.getCurrent();
        Integer pageSize = searchRequest.getPageSize();

        // 构建布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.multiMatchQuery(searchText, "spaceName", "spaceName.ik", "spaceName.standard"))
                // 大幅提高空间名称精确匹配的权重
                .should(QueryBuilders.matchPhraseQuery("spaceName", searchText).boost(5.0f))
                // 提高空间名称普通匹配的权重
                .should(QueryBuilders.matchQuery("spaceName", searchText).boost(3.0f))
                // 处理数字和英文搜索
                .should(QueryBuilders.wildcardQuery("spaceName", "*" + searchText + "*").boost(2.5f));

        // 尝试将搜索文本转换为空间ID
        try {
            Long spaceId = Long.parseLong(searchText);
            boolQueryBuilder.should(QueryBuilders.termQuery("id", spaceId));
        } catch (NumberFormatException ignored) {
            // 如果不是数字，尝试在字段中搜索包含该数字的记录
            if (searchText.matches(".*\\d+.*")) {
                boolQueryBuilder.should(QueryBuilders.wildcardQuery("spaceName", "*" + searchText + "*").boost(3.0f));
            }
        }
        boolQueryBuilder.minimumShouldMatch(1)
                .must(QueryBuilders.termQuery("isDelete", 0))
                // 只搜索团队空间
                .must(QueryBuilders.termQuery("spaceType", 1));

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(PageRequest.of(current - 1, pageSize))
                .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .build();

        SearchHits<Space> searchHits = elasticsearchRestTemplate.search(
                searchQuery,
                Space.class,
                IndexCoordinates.of(SPACE_INDEX)
        );

        List<Space> spaceList = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 填充用户信息并转换为VO
        List<SpaceVO> spaceVOList = new ArrayList<>();
        if (!spaceList.isEmpty()) {
            // 获取所有空间ID
            List<Long> spaceIds = spaceList.stream()
                    .map(Space::getId)
                    .collect(Collectors.toList());

            // 批量查询实际的空间数据
            List<Space> actualSpaces = spaceService.listByIds(spaceIds);
            Map<Long, Space> spaceMap = actualSpaces.stream()
                    .collect(Collectors.toMap(Space::getId, space -> space));

            // 获取所有用户ID
            Set<Long> userIds = spaceList.stream()
                    .map(Space::getUserId)
                    .collect(Collectors.toSet());

            // 批量查询用户信息
            Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.groupingBy(User::getId));

            // 批量查询空间成员数量
            Map<Long, Long> spaceMemberCountMap = spaceList.stream()
                    .collect(Collectors.toMap(
                            Space::getId,
                            space -> {
                                QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
                                queryWrapper.eq("spaceId", space.getId())
                                        .eq("status", 1);  // 只统计已通过的成员
                                return spaceUserService.count(queryWrapper);
                            }
                    ));

            // 转换为VO并填充信息
            spaceVOList = spaceList.stream()
                    .map(space -> {
                        SpaceVO spaceVO = SpaceVO.objToVo(space);

                        // 从数据库获取实际的空间数据
                        Space actualSpace = spaceMap.get(space.getId());
                        if (actualSpace != null) {
                            spaceVO.setTotalSize(actualSpace.getTotalSize());
                            spaceVO.setTotalCount(actualSpace.getTotalCount());
                        }

                        // 填充空间级别相关信息
                        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
                        if (spaceLevelEnum != null) {
                            spaceVO.setMaxSize(spaceLevelEnum.getMaxSize());
                            spaceVO.setMaxCount(spaceLevelEnum.getMaxCount());
                        }

                        // 设置默认值（只在实际值为null时设置）
                        spaceVO.setTotalSize(spaceVO.getTotalSize() != null ? spaceVO.getTotalSize() : 0L);
                        spaceVO.setTotalCount(spaceVO.getTotalCount() != null ? spaceVO.getTotalCount() : 0L);

                        // 填充用户信息
                        Long userId = space.getUserId();
                        if (userIdUserListMap.containsKey(userId)) {
                            User user = userIdUserListMap.get(userId).get(0);
                            UserVO userVO = userService.getUserVO(user);
                            // 设置用户默认值
                            userVO.setUserProfile(userVO.getUserProfile() != null ? userVO.getUserProfile() : "");
                            spaceVO.setUser(userVO);
                        }

                        // 填充成员数量
                        spaceVO.setMemberCount(spaceMemberCountMap.getOrDefault(space.getId(), 0L));

                        return spaceVO;
                    })
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(spaceVOList, PageRequest.of(current - 1, pageSize), searchHits.getTotalHits());
    }

    @Override
    public List<HotSearchVO> getSuggestions(String searchText, String type, Integer size) {
        List<HotSearchVO> suggestions = new ArrayList<>();

        try {
            // 1. 从Redis实时热门搜索获取建议
            String zsetKey = String.format(HOT_SEARCH_REALTIME_KEY, type);
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> hotKeywords =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(zsetKey, 0, size - 1);

            if (hotKeywords != null && !hotKeywords.isEmpty()) {
                // 获取热门词的搜索次数和趋势
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : hotKeywords) {
                    String keyword = tuple.getValue();
                    if (keyword.contains(searchText)) {
                        HotSearchVO vo = new HotSearchVO();
                        vo.setKeyword(keyword);
                        // 获取搜索次数
                        Double score = tuple.getScore();
                        vo.setRealTimeCount(score != null ? score.longValue() : 0L);
                        vo.setCount(vo.getRealTimeCount());
                        vo.setTrend(0.0); // 实时数据暂无趋势
                        vo.setScore(score != null ? score : 0.0);
                        vo.setReason("热门搜索");
                        suggestions.add(vo);
                    }
                }
            }

            // 2. 如果建议数量不足，从MySQL热门搜索获取
            if (suggestions.size() < size) {
                Date startTime = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
                List<HotSearch> hotSearchList = hotSearchMapper.getHotSearchAfter(type, startTime, size);

                for (HotSearch hotSearch : hotSearchList) {
                    // 检查是否已包含该关键词
                    boolean exists = suggestions.stream()
                            .anyMatch(vo -> vo.getKeyword().equals(hotSearch.getKeyword()));

                    if (!exists && hotSearch.getKeyword().contains(searchText)) {
                        HotSearchVO vo = new HotSearchVO();
                        vo.setKeyword(hotSearch.getKeyword());
                        vo.setCount(hotSearch.getCount());
                        vo.setRealTimeCount(hotSearch.getRealTimeCount());
                        vo.setTrend(hotSearch.getTrend());
                        // 计算综合分数
                        double score = (hotSearch.getRealTimeCount() != null ? hotSearch.getRealTimeCount() : 0L) * 0.7 +
                                (hotSearch.getCount() != null ? hotSearch.getCount() : 0L) * 0.3;
                        vo.setScore(score);
                        vo.setReason("热门推荐");
                        suggestions.add(vo);
                    }

                    if (suggestions.size() >= size) {
                        break;
                    }
                }
            }

            // 3. 如果建议数量仍然不足，从ES获取
            if (suggestions.size() < size) {
                List<EsSearchKeyword> esKeywords = esSearchKeywordDao
                        .findByTypeAndUpdateTimeAfterOrderByCountDesc(type,
                                new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));

                for (EsSearchKeyword esKeyword : esKeywords) {
                    // 检查是否已包含该关键词
                    boolean exists = suggestions.stream()
                            .anyMatch(vo -> vo.getKeyword().equals(esKeyword.getKeyword()));

                    if (!exists && esKeyword.getKeyword().contains(searchText)) {
                        HotSearchVO vo = new HotSearchVO();
                        vo.setKeyword(esKeyword.getKeyword());
                        vo.setCount(esKeyword.getCount());
                        vo.setRealTimeCount(0L);
                        vo.setTrend(0.0);
                        vo.setScore(esKeyword.getCount() != null ? esKeyword.getCount() : 0.0);
                        vo.setReason("搜索推荐");
                        suggestions.add(vo);
                    }

                    if (suggestions.size() >= size) {
                        break;
                    }
                }
            }

            // 按分数排序
            suggestions.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 限制返回数量
            if (suggestions.size() > size) {
                suggestions = suggestions.subList(0, size);
            }
        } catch (Exception e) {
            log.error("获取搜索建议失败", e);
        }

        return suggestions;
    }

    @Override
    public List<UserSearchRecord> getUserSearchHistory(Long userId, String type, Integer size) {
        if (userId == null || size == null || size <= 0) {
            return new ArrayList<>();
        }

        try {
            return userSearchRecordMapper.listRecentByUserAndType(userId, type, size);
        } catch (Exception e) {
            log.error("获取用户搜索历史失败, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteUserSearchHistoryByType(Long userId, String type) {
        if (userId == null || StringUtils.isBlank(type)) {
            return false;
        }

        try {
            userSearchRecordMapper.deleteUserSearchHistoryByType(userId, type);
            return true;
        } catch (Exception e) {
            log.error("删除用户搜索历史失败, userId={}, type={}", userId, type, e);
            return false;
        }
    }

    @Override
    public boolean deleteUserSearchHistory(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            userSearchRecordMapper.deleteUserSearchHistory(userId);
            return true;
        } catch (Exception e) {
            log.error("删除用户所有搜索历史失败, userId={}", userId, e);
            return false;
        }
    }

    /**
     * 获取猜你想搜的数据
     * 基于用户的搜索记录，加上一些随机的搜索数据，如果没有搜索记录，则随机返回一些热门词
     * @param userId 用户ID，可为空
     * @param type 搜索类型
     * @param size 返回数量
     * @return 推荐搜索词列表
     */
    @Override
    public List<HotSearchVO> getGuessYouWantToSearch(Long userId, String type, Integer size) {
        List<HotSearchVO> result = new ArrayList<>();

        try {
            // 默认返回数量
            if (size == null || size <= 0) {
                size = 10;
            }

            // 1. 如果用户已登录，优先基于用户搜索记录生成推荐
            if (userId != null) {
                List<UserSearchRecord> userSearchHistory = getUserSearchHistory(userId, type, size);

                // 如果用户有搜索记录
                if (!userSearchHistory.isEmpty()) {
                    // 收集用户搜索过的关键词
                    Set<String> userKeywords = userSearchHistory.stream()
                            .map(UserSearchRecord::getKeyword)
                            .collect(Collectors.toSet());

                    // 从热门搜索中筛选相关词
                    List<HotSearchVO> hotSearches = getHotSearchWithDetails(type, size * 2);

                    // 优先添加用户未搜索过的热门词
                    for (HotSearchVO hotSearch : hotSearches) {
                        if (result.size() >= size) {
                            break;
                        }
                        if (!userKeywords.contains(hotSearch.getKeyword())) {
                            // 添加一个标记表明这是推荐词
                            hotSearch.setReason("基于您的搜索推荐");
                            result.add(hotSearch);
                        }
                    }

                    // 如果还不够数量，添加一些随机热门词
                    if (result.size() < size) {
                        List<String> defaultKeywords = getDefaultHotSearchKeywords(type, size);
                        for (String keyword : defaultKeywords) {
                            if (result.size() >= size) {
                                break;
                            }
                            if (!userKeywords.contains(keyword)) {
                                HotSearchVO vo = new HotSearchVO();
                                vo.setKeyword(keyword);
                                vo.setCount(0L);
                                vo.setRealTimeCount(0L);
                                vo.setTrend(0.0);
                                vo.setScore(0.0);
                                vo.setReason("热门推荐");
                                result.add(vo);
                            }
                        }
                    }
                } else {
                    // 用户没有搜索记录，返回热门词
                    result = getHotSearchWithDetails(type, size);
                    // 为每个结果添加推荐理由
                    for (HotSearchVO vo : result) {
                        if (vo.getReason() == null) {
                            vo.setReason("热门推荐");
                        }
                    }
                }
            } else {
                // 2. 用户未登录，返回热门搜索词
                result = getHotSearchWithDetails(type, size);
                // 为每个结果添加推荐理由
                for (HotSearchVO vo : result) {
                    if (vo.getReason() == null) {
                        vo.setReason("热门推荐");
                    }
                }
            }

            // 如果结果为空，添加默认推荐词
            if (result.isEmpty()) {
                List<String> defaultKeywords = getDefaultHotSearchKeywords(type, size);
                for (String keyword : defaultKeywords) {
                    if (result.size() >= size) {
                        break;
                    }
                    HotSearchVO vo = new HotSearchVO();
                    vo.setKeyword(keyword);
                    vo.setCount(0L);
                    vo.setRealTimeCount(0L);
                    vo.setTrend(0.0);
                    vo.setScore(0.0);
                    vo.setReason("默认推荐");
                    result.add(vo);
                }
            }

            // 按分数排序
            result.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 限制返回数量
            if (result.size() > size) {
                result = result.subList(0, size);
            }
        } catch (Exception e) {
            log.error("获取猜你想搜数据失败", e);
            // 出现异常时返回默认数据
            try {
                List<String> defaultKeywords = getDefaultHotSearchKeywords(type, size);
                for (String keyword : defaultKeywords) {
                    if (result.size() >= size) {
                        break;
                    }
                    HotSearchVO vo = new HotSearchVO();
                    vo.setKeyword(keyword);
                    vo.setCount(0L);
                    vo.setRealTimeCount(0L);
                    vo.setTrend(0.0);
                    vo.setScore(0.0);
                    vo.setReason("默认推荐");
                    result.add(vo);
                }
            } catch (Exception ex) {
                log.error("获取默认猜你想搜数据失败", ex);
            }
        }

        return result;
    }
}
