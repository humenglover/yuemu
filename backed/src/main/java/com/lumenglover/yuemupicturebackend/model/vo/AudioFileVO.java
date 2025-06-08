package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 音频文件视图对象
 */
@Data
public class AudioFileVO implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 上传用户id
     */
    private Long userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件访问地址
     */
    private String fileUrl;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 音频时长(秒)
     */
    private Integer duration;

    /**
     * 文件MIME类型
     */
    private String mimeType;

    /**
     * 封面图片URL
     */
    private String coverUrl;

    /**
     * 音频标题
     */
    private String title;

    /**
     * 音频描述
     */
    private String description;

    /**
     * 艺术家/作者
     */
    private String artist;

    /**
     * 专辑名称
     */
    private String album;

    /**
     * 音乐类型/风格
     */
    private String genre;

    /**
     * 播放次数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 上传用户信息
     */
    private UserVO user;

    /**
     * 是否已点赞 0-未点赞 1-已点赞
     */
    private Integer isLiked;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转包装类
     *
     * @param audioFile
     * @return
     */
    public static AudioFileVO objToVo(AudioFile audioFile) {
        if (audioFile == null) {
            return null;
        }
        AudioFileVO audioFileVO = new AudioFileVO();
        BeanUtils.copyProperties(audioFile, audioFileVO);
        return audioFileVO;
    }
}
