// 设备类型枚举
export const DEVICE_TYPE_ENUM = {
  MOBILE: 'mobile',
  PC: 'pc'
} as const;

// 设备类型文本映射，用于将枚举值对应到更友好的文本描述
export const DEVICE_TYPE_MAP: Record<string, string> = {
  'mobile':'移动端',
  'pc': '电脑端'
};

// 设备类型选项映射，可用于生成下拉选项等场景（类似之前空间级别选项映射的形式）
export const DEVICE_TYPE_OPTIONS = Object.keys(DEVICE_TYPE_MAP).map((key) => {
  return {
    label: DEVICE_TYPE_MAP[key],
    value: key
  };
});
