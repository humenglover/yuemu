/**
 * YOLO 标签翻译工具类
 */

// COCO 数据集标签翻译映射
export const YOKO_LABEL_MAP: Record<string, string> = {
  person: '人',
  bicycle: '自行车',
  car: '汽车',
  motorcycle: '摩托车',
  airplane: '飞机',
  bus: '公交车',
  train: '火车',
  truck: '卡车',
  boat: '轮船',
  'traffic light': '红绿灯',
  'fire hydrant': '消防栓',
  'stop sign': '停止标志',
  'parking meter': '停车计时器',
  bench: '长凳',
  bird: '鸟',
  cat: '猫',
  dog: '狗',
  horse: '马',
  sheep: '羊',
  cow: '牛',
  elephant: '大象',
  bear: '熊',
  zebra: '斑马',
  giraffe: '长颈鹿',
  backpack: '背包',
  umbrella: '雨伞',
  handbag: '手提包',
  tie: '领带',
  suitcase: '手提箱',
  frisbee: '飞碟',
  skis: '滑雪板',
  snowboard: '单板滑雪',
  'sports ball': '运动球类',
  kite: '风筝',
  'baseball bat': '棒球棒',
  'baseball glove': '棒球手套',
  skateboard: '滑板',
  surfboard: '冲浪板',
  'tennis racket': '网球拍',
  bottle: '瓶子',
  'wine glass': '高脚杯',
  cup: '杯子',
  fork: '叉子',
  knife: '刀',
  spoon: '勺子',
  bowl: '碗',
  banana: '香蕉',
  apple: '苹果',
  sandwich: '三明治',
  orange: '橙子',
  broccoli: '西兰花',
  carrot: '胡萝卜',
  'hot dog': '热狗',
  pizza: '比萨',
  donut: '甜甜圈',
  cake: '蛋糕',
  chair: '椅子',
  couch: '沙发',
  'potted plant': '盆栽',
  bed: '床',
  'dining table': '餐桌',
  toilet: '马桶',
  tv: '电视',
  laptop: '笔记本电脑',
  mouse: '鼠标',
  remote: '遥控器',
  keyboard: '键盘',
  'cell phone': '手机',
  microwave: '微波炉',
  oven: '烤箱',
  toaster: '烤面包机',
  sink: '洗手池',
  refrigerator: '冰箱',
  book: '书',
  clock: '时钟',
  vase: '花瓶',
  scissors: '剪刀',
  'teddy bear': '泰迪熊',
  'hair drier': '吹风机',
  toothbrush: '牙刷',
};

/**
 * 将 aiLabels 统一转为数组（兼容数组和 JSON 字符串两种格式）
 */
const parseLabels = (labels: string[] | string | undefined | null): string[] => {
  if (!labels) return [];
  if (Array.isArray(labels)) return labels;
  if (typeof labels === 'string') {
    const trimmed = labels.trim();
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) return parsed;
      } catch { /* ignore */ }
    }
    // 单个标签
    return trimmed ? [trimmed] : [];
  }
  return [];
};

/**
 * 翻译标签列表（兼容数组和 JSON 字符串格式）
 */
export const translateLabels = (labels: string[] | string | undefined | null): string[] => {
  const arr = parseLabels(labels);
  if (arr.length === 0) return [];
  return arr.map((label) => YOKO_LABEL_MAP[label] || label);
};

/**
 * 检查是否包含敏感标签（如：人像）
 */
export const hasSensitiveLabel = (labels: string[] | string | undefined | null): boolean => {
  const arr = parseLabels(labels);
  if (arr.length === 0) return false;
  const sensitiveLabels = ['person'];
  return arr.some((label) => sensitiveLabels.indexOf(label) !== -1);
};

/**
 * 获取敏感内容的警告提示
 */
export const getSensitiveWarning = (): string => {
  return '提示：AI 检测到图片中包含人物，请确保已获得相关权利人的授权，并注意遵守肖像权与版权相关法律法规。';
};
