package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.exception.ThrowUtils;
import com.lumenglover.yuemupicturebackend.mapper.AudioFileMapper;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.model.dto.audio.AudioQueryRequest;
import com.lumenglover.yuemupicturebackend.service.AudioFileService;
import com.lumenglover.yuemupicturebackend.manager.upload.FileAudioUpload;
import com.lumenglover.yuemupicturebackend.manager.CosManager;
import com.lumenglover.yuemupicturebackend.constant.UserConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.core.util.StrUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 音频文件服务实现类
 */
@Service
@Slf4j
public class AudioFileServiceImpl extends ServiceImpl<AudioFileMapper, AudioFile> implements AudioFileService {

    @Autowired
    private FileAudioUpload fileAudioUpload;

    @Autowired
    private CosManager cosManager;

    private static final String UPLOAD_PATH_PREFIX = "audio";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioFileVO uploadAudio(MultipartFile file, Long userId, String title, String description,
                                   String artist, String album, String genre, Long spaceId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "音频文件不能为空");
        }

        try {
            // 使用音频上传模板进行上传
            AudioFile audioFile = fileAudioUpload.uploadAudio(file, UPLOAD_PATH_PREFIX, userId);

            // 设置额外的元数据
            audioFile.setTitle(title);
            audioFile.setDescription(description);
            audioFile.setArtist(artist);
            audioFile.setAlbum(album);
            audioFile.setGenre(genre);
            audioFile.setSpaceId(spaceId);
            audioFile.setUserId(userId);

            // 保存到数据库
            boolean saveResult = this.save(audioFile);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频文件信息保存失败");
            }

            // 转换为VO对象
            AudioFileVO audioFileVO = new AudioFileVO();
            BeanUtils.copyProperties(audioFile, audioFileVO);
            return audioFileVO;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("音频上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAudio(Long id, Long userId) {
        AudioFile audioFile = this.getById(id);
        if (audioFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "音频文件不存在");
        }

        // 只能删除自己的音频
        if (!audioFile.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除该音频文件");
        }

        try {
            // 删除COS中的文件
            cosManager.deleteObject(audioFile.getFilePath());
            // 删除数据库记录
            return this.removeById(id);
        } catch (Exception e) {
            log.error("音频文件删除失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音频文件删除失败");
        }
    }

    @Override
    public Page<AudioFile> listAudioByPage(AudioQueryRequest audioQueryRequest) {
        long current = audioQueryRequest.getCurrent();
        long size = audioQueryRequest.getPageSize();
        Page<AudioFile> audioFilePage = this.page(new Page<>(current, size),
                this.getQueryWrapper(audioQueryRequest));
        return audioFilePage;
    }

    @Override
    public Page<AudioFileVO> listAudioVOByPage(AudioQueryRequest audioQueryRequest, HttpServletRequest request) {
        long current = audioQueryRequest.getCurrent();
        long size = audioQueryRequest.getPageSize();

        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 查询数据
        Page<AudioFile> audioFilePage = this.page(new Page<>(current, size),
                this.getQueryWrapper(audioQueryRequest));

        // 转换为VO
        Page<AudioFileVO> audioFileVOPage = new Page<>(current, size, audioFilePage.getTotal());
        List<AudioFileVO> audioFileVOList = audioFilePage.getRecords().stream()
                .map(audioFile -> {
                    AudioFileVO audioFileVO = new AudioFileVO();
                    BeanUtils.copyProperties(audioFile, audioFileVO);
                    return audioFileVO;
                })
                .collect(Collectors.toList());
        audioFileVOPage.setRecords(audioFileVOList);

        return audioFileVOPage;
    }

    @Override
    public AudioFileVO getAudioVOById(long id, HttpServletRequest request) {
        AudioFile audioFile = this.getById(id);
        if (audioFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        AudioFileVO audioFileVO = new AudioFileVO();
        BeanUtils.copyProperties(audioFile, audioFileVO);
        return audioFileVO;
    }

    /**
     * 获取查询包装类
     */
    private QueryWrapper<AudioFile> getQueryWrapper(AudioQueryRequest audioQueryRequest) {
        QueryWrapper<AudioFile> queryWrapper = new QueryWrapper<>();
        if (audioQueryRequest == null) {
            return queryWrapper;
        }

        String title = audioQueryRequest.getTitle();
        String artist = audioQueryRequest.getArtist();
        String album = audioQueryRequest.getAlbum();
        String genre = audioQueryRequest.getGenre();
        Long userId = audioQueryRequest.getUserId();
        Long spaceId = audioQueryRequest.getSpaceId();
        String sortField = audioQueryRequest.getSortField();
        String sortOrder = audioQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(artist), "artist", artist);
        queryWrapper.like(StrUtil.isNotBlank(album), "album", album);
        queryWrapper.like(StrUtil.isNotBlank(genre), "genre", genre);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(spaceId != null, "spaceId", spaceId);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),
                "ascend".equals(sortOrder),
                sortField);

        return queryWrapper;
    }


}
