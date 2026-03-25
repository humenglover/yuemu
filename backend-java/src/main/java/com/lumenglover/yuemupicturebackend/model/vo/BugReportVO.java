package com.lumenglover.yuemupicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.lumenglover.yuemupicturebackend.model.entity.BugReport;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * bug报告视图
 *
 * @author 鹿梦
 */
@Data
public class BugReportVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * bug标题
     */
    private String title;

    /**
     * 详细
     */
    private String description;

    /**
     * bug类型
     */
    private Integer bugType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 截图URL数组
     */
    private List<String> screenshotUrls;

    /**
     * 出现问题的网站URL
     */
    private String websiteUrl;

    /**
     * 解决时间
     */
    private Date resolvedTime;

    /**
     * 解决方案或说明
     */
    private String resolution;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 包装类转对象
     *
     * @param bugReportVO
     * @return
     */
    public static BugReport voToObj(BugReportVO bugReportVO) {
        if (bugReportVO == null) {
            return null;
        }
        BugReport obj = new BugReport();
        BeanUtils.copyProperties(bugReportVO, obj);
        return obj;
    }

    /**
     * 对象转包装类
     *
     * @param bugReport
     * @return
     */
    public static BugReportVO objToVo(BugReport bugReport) {
        if (bugReport == null) {
            return null;
        }
        BugReportVO bugReportVO = new BugReportVO();
        BeanUtils.copyProperties(bugReport, bugReportVO);

        // 处理截图URL数组的转换
        if (bugReport.getScreenshotUrls() != null) {
            try {
                bugReportVO.setScreenshotUrls(JSONUtil.toList(bugReport.getScreenshotUrls(), String.class));
            } catch (Exception e) {
                // 如果解析失败，设置为空列表
            }
        }

        return bugReportVO;
    }
}
