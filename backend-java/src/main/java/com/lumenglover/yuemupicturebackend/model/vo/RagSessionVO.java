package com.lumenglover.yuemupicturebackend.model.vo;

import com.lumenglover.yuemupicturebackend.model.entity.RagUserSession;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * RAG会话视图对象
 */
@Data
public class RagSessionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long id;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 会话名称（默认：新会话+时间戳）
     */
    private String sessionName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后消息时间
     */
    private Date updateTime;

    /**
     * 是否为当前活跃会话 0-否 1-是
     */
    private Integer isActive;

    /**
     * 是否删除 0-未删 1-已删
     */
    private Integer isDelete;

    /**
     * 获取会话VO
     *
     * @param ragUserSession 会话实体
     * @return 会话VO
     */
    public static RagSessionVO objToVo(RagUserSession ragUserSession) {
        if (ragUserSession == null) {
            return null;
        }
        RagSessionVO ragSessionVO = new RagSessionVO();
        BeanUtils.copyProperties(ragUserSession, ragSessionVO);
        return ragSessionVO;
    }

    /**
     * 获取会话实体
     *
     * @param ragSessionVO 会话VO
     * @return 会话实体
     */
    public static RagUserSession voToObj(RagSessionVO ragSessionVO) {
        if (ragSessionVO == null) {
            return null;
        }
        RagUserSession ragUserSession = new RagUserSession();
        BeanUtils.copyProperties(ragSessionVO, ragUserSession);
        return ragUserSession;
    }
}
