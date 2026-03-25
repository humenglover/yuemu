package com.lumenglover.yuemupicturebackend.model.dto.post;

import com.lumenglover.yuemupicturebackend.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 帖子 DTO
 */
@Data
public class PostDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String category;

    private String tags;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;

    private Long shareCount;

    private Integer status;

    private String reviewMessage;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;



    private UserVO user;

    private Integer isLiked;

    private Integer isShared;

    private Double hotScore;

    // 用于推荐系统 - 相似度分数
    private Double similarityScore;

    // 用于推荐系统 - 行为权重
    private Integer behaviorWeight;

    // 封面图URL
    private String coverUrl;
}
