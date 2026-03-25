package com.lumenglover.yuemupicturebackend.model.enums;

import lombok.Getter;

/**
 * 网站分类枚举
 */
@Getter
public enum SiteTypeEnum {
    PERSONAL_BLOG("个人博客", "personal_blog"),
    TECH_COMMUNITY("技术社区", "tech_community"),
    RESOURCE_SHARING("资源分享", "resource_sharing"),
    DESIGN_INSPIRATION("设计灵感", "design_inspiration"),
    TOOL_NAVIGATION("工具导航", "tool_navigation"),
    NEWS_INFORMATION("新闻资讯", "news_information"),
    LEARNING_EDUCATION("学习教育", "learning_education"),
    LIFE_SHARING("生活分享", "life_sharing"),
    OTHERS("其他", "others");

    private final String name;
    private final String value;

    SiteTypeEnum(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public static SiteTypeEnum getByValue(String value) {
        for (SiteTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return OTHERS;
    }
}
