package com.lumenglover.yuemupicturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.model.dto.audio.AudioQueryRequest;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 音频文件服务接口
 */
public interface AudioFileService extends IService<AudioFile> {

    /**
     * 上传音频文件
     *
     * @param file        音频文件
     * @param userId      用户ID
     * @param title       音频标题
     * @param description 音频描述
     * @param artist      艺术家
     * @param album       专辑
     * @param genre       流派
     * @param spaceId     空间ID
     * @return 音频文件视图对象
     */
    AudioFileVO uploadAudio(MultipartFile file, Long userId, String title, String description,
                            String artist, String album, String genre, Long spaceId);

    /**
     * 删除音频文件
     *
     * @param id     音频文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteAudio(Long id, Long userId);

    /**
     * 分页获取音频列表
     *
     * @param audioQueryRequest 查询条件
     * @return 音频文件分页列表
     */
    Page<AudioFile> listAudioByPage(AudioQueryRequest audioQueryRequest);

    /**
     * 分页获取音频VO列表
     *
     * @param audioQueryRequest 查询条件
     * @param request          HTTP请求
     * @return 音频文件VO分页列表
     */
    Page<AudioFileVO> listAudioVOByPage(AudioQueryRequest audioQueryRequest, HttpServletRequest request);

    /**
     * 获取音频文件VO
     *
     * @param id      音频文件ID
     * @param request HTTP请求
     * @return 音频文件VO
     */
    AudioFileVO getAudioVOById(long id, HttpServletRequest request);
}
