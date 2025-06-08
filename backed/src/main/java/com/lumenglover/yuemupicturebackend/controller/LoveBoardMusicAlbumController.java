package com.lumenglover.yuemupicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lumenglover.yuemupicturebackend.common.BaseResponse;
import com.lumenglover.yuemupicturebackend.common.ResultUtils;
import com.lumenglover.yuemupicturebackend.exception.BusinessException;
import com.lumenglover.yuemupicturebackend.exception.ErrorCode;
import com.lumenglover.yuemupicturebackend.model.dto.musicalbum.MusicAlbumPasswordRequest;
import com.lumenglover.yuemupicturebackend.model.dto.musicalbum.MusicUploadRequest;
import com.lumenglover.yuemupicturebackend.model.entity.LoveBoardMusicAlbum;
import com.lumenglover.yuemupicturebackend.model.entity.User;
import com.lumenglover.yuemupicturebackend.model.vo.AudioFileVO;
import com.lumenglover.yuemupicturebackend.service.LoveBoardMusicAlbumService;
import com.lumenglover.yuemupicturebackend.service.UserService;
import com.lumenglover.yuemupicturebackend.service.AudioFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 恋爱板音乐专栏接口
 */
@RestController
@RequestMapping("/love-board/music-album")
@Slf4j
public class LoveBoardMusicAlbumController {

    @Resource
    private LoveBoardMusicAlbumService loveBoardMusicAlbumService;

    @Resource
    private UserService userService;

    @Resource
    private AudioFileService audioFileService;

    /**
     * 创建音乐专栏
     */
    @PostMapping("/add")
    public BaseResponse<Long> createMusicAlbum(@RequestBody LoveBoardMusicAlbum musicAlbum,
                                               @RequestParam("loveBoardId") long loveBoardId,
                                               HttpServletRequest request) {
        if (musicAlbum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long albumId = loveBoardMusicAlbumService.createMusicAlbum(musicAlbum, loginUser.getId(), loveBoardId);
        return ResultUtils.success(albumId);
    }

    /**
     * 删除音乐专栏
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> removeMusicAlbum(@RequestParam("id") long id,
                                                  @RequestParam("loveBoardId") long loveBoardId,
                                                  HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = loveBoardMusicAlbumService.deleteMusicAlbum(id, loginUser.getId(), loveBoardId);
        return ResultUtils.success(result);
    }

    /**
     * 更新音乐专栏
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> modifyMusicAlbum(@RequestBody LoveBoardMusicAlbum musicAlbum,
                                                  @RequestParam("loveBoardId") long loveBoardId,
                                                  HttpServletRequest request) {
        if (musicAlbum == null || musicAlbum.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = loveBoardMusicAlbumService.updateMusicAlbum(musicAlbum, loginUser.getId(), loveBoardId);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取音乐专栏
     */
    @GetMapping("/get")
    public BaseResponse<LoveBoardMusicAlbum> fetchMusicAlbumById(@RequestParam("id") long id,
                                                                 @RequestParam(value = "password", required = false) String password,
                                                                 HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.isLogin(request);
        Long userId = loginUser != null ? loginUser.getId() : null;
        LoveBoardMusicAlbum musicAlbum = loveBoardMusicAlbumService.getMusicAlbumById(id, userId, password);
        return ResultUtils.success(musicAlbum);
    }

    /**
     * 获取专栏内的音频列表
     */
    @GetMapping("/audios")
    public BaseResponse<List<AudioFileVO>> fetchAlbumAudios(@RequestParam("albumId") long albumId,
                                                            @RequestParam(value = "password", required = false) String password,
                                                            HttpServletRequest request) {
        if (albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.isLogin(request);
        Long userId = loginUser != null ? loginUser.getId() : null;
        List<AudioFileVO> audioList = loveBoardMusicAlbumService.getAlbumAudios(albumId, userId, password);
        return ResultUtils.success(audioList);
    }

    /**
     * 分页获取音乐专栏列表
     */
    @GetMapping("/list")
    public BaseResponse<Page<LoveBoardMusicAlbum>> fetchMusicAlbums(
            @RequestParam("current") long current,
            @RequestParam("pageSize") long pageSize,
            @RequestParam("loveBoardId") long loveBoardId) {
        if (current <= 0 || pageSize <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<LoveBoardMusicAlbum> musicAlbumPage = loveBoardMusicAlbumService.listMusicAlbums(loveBoardId, current, pageSize);
        return ResultUtils.success(musicAlbumPage);
    }

    /**
     * 设置专栏密码
     */
    @PostMapping("/password/set")
    public BaseResponse<Boolean> setMusicAlbumPassword(@RequestBody MusicAlbumPasswordRequest request,
                                                       HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null || StringUtils.isBlank(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = loveBoardMusicAlbumService.setAlbumPassword(request.getAlbumId(),
                request.getNewPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新专栏密码
     */
    @PostMapping("/password/update")
    public BaseResponse<Boolean> modifyMusicAlbumPassword(@RequestBody MusicAlbumPasswordRequest request,
                                                          HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null ||
                StringUtils.isBlank(request.getOldPassword()) || StringUtils.isBlank(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = loveBoardMusicAlbumService.updateAlbumPassword(request.getAlbumId(),
                request.getOldPassword(), request.getNewPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 移除专栏密码
     */
    @PostMapping("/password/remove")
    public BaseResponse<Boolean> removeMusicAlbumPassword(@RequestBody MusicAlbumPasswordRequest request,
                                                          HttpServletRequest httpRequest) {
        if (request == null || request.getAlbumId() == null || StringUtils.isBlank(request.getOldPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = loveBoardMusicAlbumService.removeAlbumPassword(request.getAlbumId(),
                request.getOldPassword(), loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 上传音乐到专栏
     */
    @PostMapping("/upload")
    public BaseResponse<AudioFileVO> uploadMusicToAlbum(
            @RequestParam("file") MultipartFile file,
            @RequestParam("albumId") Long albumId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "artist", required = false) String artist,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "coverUrl", required = false) String coverUrl,
            HttpServletRequest request) {
        // 参数校验
        if (file == null || file.isEmpty() || albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 构建上传请求DTO
        MusicUploadRequest musicUploadRequest = new MusicUploadRequest();
        musicUploadRequest.setFile(file);
        musicUploadRequest.setAlbumId(albumId);
        musicUploadRequest.setTitle(title);
        musicUploadRequest.setDescription(description);
        musicUploadRequest.setArtist(artist);
        musicUploadRequest.setAlbum(album);
        musicUploadRequest.setGenre(genre);
        musicUploadRequest.setCoverUrl(coverUrl);

        // 检查专栏权限并上传音乐
        AudioFileVO audioFileVO = loveBoardMusicAlbumService.uploadMusic(musicUploadRequest, loginUser.getId());

        return ResultUtils.success(audioFileVO);
    }

    /**
     * 删除专栏中的音频
     */
    @PostMapping("/audio/delete")
    public BaseResponse<Boolean> deleteAudioFromAlbum(
            @RequestParam("albumId") Long albumId,
            @RequestParam("audioId") Long audioId,
            HttpServletRequest request) {
        if (albumId == null || audioId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = loveBoardMusicAlbumService.deleteAudioFromAlbum(albumId, audioId, loginUser.getId());
        return ResultUtils.success(result);
    }
}
