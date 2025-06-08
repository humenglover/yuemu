package com.lumenglover.yuemupicturebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.mapper.LoveBoardMusicAlbumMapper;
import com.lumenglover.yuemupicturebackend.model.dto.musicalbum.MusicUploadRequest;
import com.lumenglover.yuemupicturebackend.model.entity.AudioFile;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoard;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoardMusicAlbum;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.service.AudioFileService;
import com.lumenglover.yuemupicturebackend.service.LoveBoardMusicAlbumService;
import com.lumenglover.yuemupicturebackend.service.LoveBoardService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import cn.hutool.crypto.digest.DigestUtil;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 恋爱板音乐专栏服务实现类
 */
@Service
@Slf4j
public class LoveBoardMusicAlbumServiceImpl extends ServiceImpl<LoveBoardMusicAlbumMapper, LoveBoardMusicAlbum>
        implements LoveBoardMusicAlbumService {

    @Resource
    private LoveBoardService loveBoardService;

    @Resource
    private AudioFileService audioFileService;

    private static final int MAX_ALBUM_CAPACITY = 100; // 专栏最大音频数量
    private static final int MAX_USER_ALBUMS = 20; // 用户最大专栏数量

    /**
     * 检查用户专栏数量是否达到上限
     * @param userId 用户ID
     * @return 是否达到上限
     */
    private boolean checkUserAlbumLimit(Long userId) {
        QueryWrapper<LoveBoardMusicAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        long count = this.count(queryWrapper);
        return count < MAX_USER_ALBUMS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createMusicAlbum(LoveBoardMusicAlbum loveBoardMusicAlbum, Long userId, Long loveBoardId) {
        // 参数校验
        if (loveBoardMusicAlbum == null || userId == null || loveBoardId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查用户专栏数量是否达到上限
        if (!checkUserAlbumLimit(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您的专栏数量已达到上限（" + MAX_USER_ALBUMS + "个）");
        }

        // 检查恋爱板是否存在且属于该用户
        LoveBoard loveBoard = loveBoardService.getById(loveBoardId);
        if (loveBoard == null || !loveBoard.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 设置必要字段
        loveBoardMusicAlbum.setUserId(userId);
        loveBoardMusicAlbum.setLoveBoardId(loveBoardId);

        // 保存专栏
        boolean success = this.save(loveBoardMusicAlbum);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建音乐专栏失败");
        }

        return loveBoardMusicAlbum.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMusicAlbum(Long id, Long userId, Long loveBoardId) {
        // 参数校验
        if (id == null || userId == null || loveBoardId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum album = this.getById(id);
        if (album == null || !album.getUserId().equals(userId) || !album.getLoveBoardId().equals(loveBoardId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除专栏
        return this.removeById(id);
    }

    @Override
    public boolean updateMusicAlbum(LoveBoardMusicAlbum loveBoardMusicAlbum, Long userId, Long loveBoardId) {
        // 参数校验
        if (loveBoardMusicAlbum == null || loveBoardMusicAlbum.getId() == null
                || userId == null || loveBoardId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum oldAlbum = this.getById(loveBoardMusicAlbum.getId());
        if (oldAlbum == null || !oldAlbum.getUserId().equals(userId)
                || !oldAlbum.getLoveBoardId().equals(loveBoardId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新专栏
        return this.updateById(loveBoardMusicAlbum);
    }

    @Override
    public LoveBoardMusicAlbum getMusicAlbumById(Long id, Long userId, String password) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取专栏信息
        LoveBoardMusicAlbum album = this.getById(id);
        if (album == null || album.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 如果是公开专栏，直接返回
        if (album.getIsPublic() == 1) {
            return album;
        }

        // 如果是私密专栏，所有人都需要验证密码
        if (StringUtils.isNotBlank(album.getPassword())) {
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请输入专栏密码");
            }
            // 对输入的密码进行MD5加密后再比较
            String encryptedPassword = DigestUtil.md5Hex(password);
            if (!encryptedPassword.equals(album.getPassword())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "专栏密码错误");
            }
        }

        return album;
    }

    @Override
    public List<AudioFileVO> getAlbumAudios(Long albumId, Long userId, String password) {
        // 先检查专栏访问权限（包括密码验证）
        LoveBoardMusicAlbum album = this.getMusicAlbumById(albumId, userId, password);

        // 查询专栏内的音频
        QueryWrapper<AudioFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", albumId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByAsc("createTime");

        List<AudioFile> audioFiles = audioFileService.list(queryWrapper);
        return audioFiles.stream()
                .map(audioFile -> {
                    AudioFileVO audioFileVO = new AudioFileVO();
                    audioFileVO.setId(audioFile.getId());
                    audioFileVO.setUserId(audioFile.getUserId());
                    audioFileVO.setFileName(audioFile.getFileName());
                    audioFileVO.setFileUrl(audioFile.getFileUrl());
                    audioFileVO.setFileSize(audioFile.getFileSize());
                    audioFileVO.setDuration(audioFile.getDuration());
                    audioFileVO.setMimeType(audioFile.getMimeType());
                    audioFileVO.setCoverUrl(audioFile.getCoverUrl());
                    audioFileVO.setTitle(audioFile.getTitle());
                    audioFileVO.setDescription(audioFile.getDescription());
                    audioFileVO.setArtist(audioFile.getArtist());
                    audioFileVO.setAlbum(audioFile.getAlbum());
                    audioFileVO.setGenre(audioFile.getGenre());
                    audioFileVO.setViewCount(audioFile.getViewCount());
                    audioFileVO.setLikeCount(audioFile.getLikeCount());
                    audioFileVO.setCreateTime(audioFile.getCreateTime());
                    audioFileVO.setUpdateTime(audioFile.getUpdateTime());
                    // 设置默认值而不是null
                    audioFileVO.setDuration(audioFile.getDuration() != null ? audioFile.getDuration() : 0);
                    audioFileVO.setViewCount(audioFile.getViewCount() != null ? audioFile.getViewCount() : 0L);
                    audioFileVO.setLikeCount(audioFile.getLikeCount() != null ? audioFile.getLikeCount() : 0L);
                    return audioFileVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<LoveBoardMusicAlbum> listMusicAlbums(Long loveBoardId, Long current, Long pageSize) {
        // 参数校验
        if (loveBoardId == null || current == null || pageSize == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 分页查询
        QueryWrapper<LoveBoardMusicAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("loveBoardId", loveBoardId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");

        return this.page(new Page<>(current, pageSize), queryWrapper);
    }

    @Override
    public boolean setAlbumPassword(Long albumId, String newPassword, Long userId) {
        // 参数校验
        if (albumId == null || StringUtils.isBlank(newPassword) || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum album = this.getById(albumId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 加密密码
        String encryptedPassword = DigestUtil.md5Hex(newPassword);
        // 设置密码
        album.setPassword(encryptedPassword);
        album.setIsPublic(0); // 设置密码后变为私密专栏
        return this.updateById(album);
    }

    @Override
    public boolean updateAlbumPassword(Long albumId, String oldPassword, String newPassword, Long userId) {
        // 参数校验
        if (albumId == null || StringUtils.isBlank(oldPassword)
                || StringUtils.isBlank(newPassword) || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum album = this.getById(albumId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 验证旧密码
        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword);
        if (!encryptedOldPassword.equals(album.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }

        // 加密新密码
        String encryptedNewPassword = DigestUtil.md5Hex(newPassword);
        // 更新密码
        album.setPassword(encryptedNewPassword);
        return this.updateById(album);
    }

    @Override
    public boolean removeAlbumPassword(Long albumId, String oldPassword, Long userId) {
        // 参数校验
        if (albumId == null || StringUtils.isBlank(oldPassword) || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum album = this.getById(albumId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 验证旧密码
        String encryptedOldPassword = DigestUtil.md5Hex(oldPassword);
        if (!encryptedOldPassword.equals(album.getPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        // 移除密码
        album.setPassword(null);
        album.setIsPublic(1); // 移除密码后变为公开专栏
        return this.updateById(album);
    }

    @Override
    public boolean deleteAudioFromAlbum(Long albumId, Long audioId, Long userId) {
        // 参数校验
        if (albumId == null || audioId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum album = this.getById(albumId);
        if (album == null || !album.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该专栏");
        }

        // 检查音频是否存在且属于该专栏
        AudioFile audioFile = audioFileService.getById(audioId);
        if (audioFile == null || !audioFile.getSpaceId().equals(albumId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "音频不存在或不属于该专栏");
        }

        // 删除音频
        return audioFileService.removeById(audioId);
    }

    @Override
    public boolean checkAlbumCapacity(Long albumId) {
        if (albumId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询专栏当前音频数量
        QueryWrapper<AudioFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", albumId);
        queryWrapper.eq("isDelete", 0);
        long count = audioFileService.count(queryWrapper);

        return count < MAX_ALBUM_CAPACITY;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AudioFileVO uploadMusic(MusicUploadRequest musicUploadRequest, Long userId) {
        // 参数校验
        if (musicUploadRequest == null || musicUploadRequest.getFile() == null ||
                musicUploadRequest.getFile().isEmpty() || musicUploadRequest.getAlbumId() == null ||
                userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 检查专栏是否存在且属于该用户
        LoveBoardMusicAlbum musicAlbum = this.getById(musicUploadRequest.getAlbumId());
        if (musicAlbum == null || musicAlbum.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "音乐专栏不存在");
        }

        // 检查专栏所有权
        if (!musicAlbum.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权上传音乐到该专栏");
        }

        // 检查专栏音频数量是否达到上限
        if (!checkAlbumCapacity(musicUploadRequest.getAlbumId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "专栏音频数量已达到上限（" + MAX_ALBUM_CAPACITY + "首）");
        }

        try {
            // 设置默认值
            String title = musicUploadRequest.getTitle();
            if (StringUtils.isBlank(title)) {
                title = musicUploadRequest.getFile().getOriginalFilename();
                // 移除文件扩展名
                if (title != null && title.contains(".")) {
                    title = title.substring(0, title.lastIndexOf("."));
                }
            }

            // 使用音频服务上传文件
            AudioFileVO audioFileVO = audioFileService.uploadAudio(
                    musicUploadRequest.getFile(),
                    userId,
                    title,
                    musicUploadRequest.getDescription(),
                    musicUploadRequest.getArtist(),
                    musicUploadRequest.getAlbum(),
                    musicUploadRequest.getGenre(),
                    musicUploadRequest.getAlbumId()
            );

            // 设置封面URL
            if (StringUtils.isNotBlank(musicUploadRequest.getCoverUrl())) {
                audioFileVO.setCoverUrl(musicUploadRequest.getCoverUrl());
                // 更新音频文件的封面URL
                AudioFile audioFile = new AudioFile();
                audioFile.setId(audioFileVO.getId());
                audioFile.setCoverUrl(musicUploadRequest.getCoverUrl());
                audioFileService.updateById(audioFile);
            }

            // 确保返回完整的数据
            if (audioFileVO != null) {
                audioFileVO.setDuration(audioFileVO.getDuration() != null ? audioFileVO.getDuration() : 0);
                audioFileVO.setViewCount(0L);
                audioFileVO.setLikeCount(0L);
                audioFileVO.setUserId(userId);
            }

            return audioFileVO;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("音乐上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "音乐上传失败");
        }
    }
}
