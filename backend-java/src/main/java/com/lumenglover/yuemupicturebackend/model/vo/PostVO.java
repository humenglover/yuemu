package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.Post;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PostVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签JSON数组
     */
    private String tags;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 状态 0-待审核 1-已发布 2-已拒绝
     */
    private Integer status;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;



    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 当前用户是否点赞
     */
    private Integer isLiked;

    /**
     * 当前用户是否分享
     */
    private Integer isShared;

    /**
     * 当前用户是否收藏
     */
    private Integer isFavorited;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 收藏数
     */
    private Long favoriteCount;

    /**
     * 热榜分数
     */
    private Double hotScore;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 是否为草稿：0-非草稿 1-草稿
     */
    private Integer isDraft;

    /**
     * 是否允许收藏：1-允许、0-禁止
     */
    private Integer allowCollect;

    /**
     * 是否允许点赞：1-允许、0-禁止
     */
    private Integer allowLike;

    /**
     * 是否允许评论：1-允许、0-禁止
     */
    private Integer allowComment;

    /**
     * 是否允许分享：1-允许、0-禁止
     */
    private Integer allowShare;

    private List<String> tagList;

    private static final long serialVersionUID = 1L;

    /**
     * 【关键修正】实体类(Post)转VO类(PostVO)
     */
    public static PostVO objToVo(Post post) { // 入参从PostVO改为Post
        if (post == null) {
            return null;
        }
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);

        // 如果 tags 不为 null，则转换为 tagList
        if (post.getTags() != null) {
            try {
                List<String> tagList = cn.hutool.json.JSONUtil.toList(post.getTags(), String.class);
                postVO.setTagList(tagList);
            } catch (Exception e) {
                // 转换失败，设置为空列表
                postVO.setTagList(new ArrayList<>());
            }
        } else {
            postVO.setTagList(new ArrayList<>());
        }

        return postVO;
    }

    /**
     * 【关键修正】VO类转实体类
     */
    public static Post voToObj(PostVO postVO) {
        if (postVO == null) {
            return null;
        }
        Post post = new Post();
        BeanUtils.copyProperties(postVO, post);
        return post;
    }
}
