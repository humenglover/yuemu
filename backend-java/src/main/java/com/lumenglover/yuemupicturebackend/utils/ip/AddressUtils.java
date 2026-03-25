package com.lumenglover.yuemupicturebackend.utils.ip;

import cn.hutool.core.net.NetUtil;
import cn.hutool.http.HtmlUtil;
import com.lumenglover.yuemupicturebackend.utils.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取地址类（支持灵活返回省/市/完整地址）
 *
 * @author Lion Li 示例1：只返回省份（如：陕西省）
 *     address = AddressUtils.getRealAddressByIP(ip, AddressUtils.AddressLevel.ONLY_PROVINCE);
 *
 *     // 示例2：只返回城市（如：西安市）
 *     // address = AddressUtils.getRealAddressByIP(ip, AddressUtils.AddressLevel.ONLY_CITY);
 *
 *     // 示例3：省份+城市（用" "分隔，如：陕西省 西安市）
 *     // address = AddressUtils.getRealAddressByIP(ip, AddressUtils.AddressLevel.PROVINCE_CITY, " ");
 *
 *     // 示例4：完整地址（兼容原有逻辑，如：中国|陕西省|西安市|联通）
 *     // address = AddressUtils.getRealAddressByIP(ip);
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtils {

    // 未知地址
    public static final String UNKNOWN = "XX XX";
    // 原始地址分隔符（匹配 RegionUtils 返回的 "中国|陕西省|西安市|联通" 格式）
    private static final String ORIGINAL_SPLITTER = "\\|";

    /**
     * 地址返回层级枚举（按需选择返回粒度）
     */
    public enum AddressLevel {
        ONLY_PROVINCE("仅省份"),    // 只返回省份（如：陕西省）
        ONLY_CITY("仅城市"),        // 只返回城市（如：西安市）
        FULL_ADDRESS("完整地址"),   // 完整格式（如：中国|陕西省|西安市|联通，兼容原有返回）
        PROVINCE_CITY("省+市");     // 省份+城市（如：陕西省-西安市）

        private final String desc;

        AddressLevel(String desc) {
            this.desc = desc;
        }
    }

    /**
     * 原有方法（兼容旧逻辑，默认返回完整地址）
     * 调用示例：AddressUtils.getRealAddressByIP("113.132.XX.XX")
     */
    public static String getRealAddressByIP(String ip) {
        return getRealAddressByIP(ip, AddressLevel.FULL_ADDRESS);
    }

    /**
     * 重载方法：指定地址返回层级
     * 调用示例：
     * 1. 只返回省份：AddressUtils.getRealAddressByIP("113.132.XX.XX", AddressLevel.ONLY_PROVINCE)
     * 2. 只返回城市：AddressUtils.getRealAddressByIP("113.132.XX.XX", AddressLevel.ONLY_CITY)
     */
    public static String getRealAddressByIP(String ip, AddressLevel level) {
        // 1. 参数校验：IP为空返回未知
        if (StringUtils.isBlank(ip)) {
            log.warn("获取地址失败：IP地址为空");
            return UNKNOWN;
        }

        // 2. 处理本地IP和HTML标签过滤
        ip = "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : HtmlUtil.cleanHtmlTag(ip);

        // 3. 内网IP直接返回，不查询
        if (NetUtil.isInnerIP(ip)) {
            return "内网IP";
        }

        // 4. 调用 RegionUtils 获取原始地址（格式：中国|陕西省|西安市|联通）
        String originalAddress = RegionUtils.getCityInfo(ip);

        // 5. 原始地址为空/未知，返回默认值
        if (StringUtils.isBlank(originalAddress) || UNKNOWN.equals(originalAddress)) {
            log.warn("IP: {} 未查询到地址信息", ip);
            return UNKNOWN;
        }

        // 6. 按指定层级处理地址
        return parseAddressByLevel(originalAddress, level);
    }

    /**
     * 按层级解析地址（核心逻辑）
     */
    private static String parseAddressByLevel(String originalAddress, AddressLevel level) {
        // 拆分原始地址为数组：[中国, 陕西省, 西安市, 联通]
        String[] addressParts = originalAddress.split(ORIGINAL_SPLITTER);
        int partLength = addressParts.length;

        // 安全处理：防止拆分后数组长度不足（如部分IP只返回国家+省份）
        String country = partLength > 0 ? StringUtils.trim(addressParts[0]) : "";
        String province = partLength > 1 ? StringUtils.trim(addressParts[1]) : "";
        String city = partLength > 2 ? StringUtils.trim(addressParts[2]) : "";
        String isp = partLength > 3 ? StringUtils.trim(addressParts[3]) : "";

        // 根据层级返回对应格式
        switch (level) {
            case ONLY_PROVINCE:
                // 只返回省份（无省份则返回国家）
                return StringUtils.isNotBlank(province) ? province : country;
            case ONLY_CITY:
                // 只返回城市（无城市则返回省份/国家）
                if (StringUtils.isNotBlank(city)) {
                    return city;
                } else if (StringUtils.isNotBlank(province)) {
                    return province;
                } else {
                    return country;
                }
            case PROVINCE_CITY:
                // 省份+城市（用"-"连接，无城市则只返回省份）
                if (StringUtils.isNotBlank(city)) {
                    return province + "-" + city;
                } else {
                    return StringUtils.isNotBlank(province) ? province : country;
                }
            case FULL_ADDRESS:
            default:
                // 完整地址（兼容原有格式）
                return originalAddress;
        }
    }

    /**
     * 扩展方法：自定义分隔符（如需要省份和城市用" "分隔，可调用此方法）
     * 调用示例：AddressUtils.getRealAddressByIP("113.132.XX.XX", AddressLevel.PROVINCE_CITY, " ")
     */
    public static String getRealAddressByIP(String ip, AddressLevel level, String separator) {
        if (StringUtils.isBlank(separator)) {
            separator = "-"; // 默认分隔符
        }

        // 先按层级获取基础地址
        String baseAddress = getRealAddressByIP(ip, level);

        // 处理 PROVINCE_CITY 层级的分隔符替换
        if (level == AddressLevel.PROVINCE_CITY) {
            return baseAddress.replace("-", separator);
        }

        return baseAddress;
    }
}
