// tui-image-editor语言配置，汉化文字
export const localeCN = {
  Resize: "调整宽高",
  Crop: "裁剪",
  Flip: "镜像",
  Rotate: "旋转",
  Draw: "画笔",
  Shape: "形状标注",
  Icon: "图标标注",
  Text: "文字标注",
  Mask: "遮罩",
  Filter: "滤镜",

  Download: "下 载",
  Load: "上 传",

  Free: "任意线条",
  Straight: "直线",
  Color: "颜色",
  Range: "粗细",

  ZoomIn: "放大",
  ZoomOut: "缩小",
  Hand: "手掌",
  History: "历史",
  Undo: "撤销",
  Redo: "反撤销",
  Reset: "重置",
  Delete: "删除",
  DeleteAll: "全部删除",

  Bold: "加粗",
  Italic: "斜体",
  Underline: "下划线",
  Left: "左对齐",
  Center: "居中",
  Right: "右对齐",
  "Text size": "字体大小",
  Custom: "自定义",
  Square: "正方形",
  Apply: "应用",
  Cancel: "取消",
  "Flip X": "X 轴",
  "Flip Y": "Y 轴",
  Stroke: "描边",
  Fill: "填充",
  Circle: "圆",
  Triangle: "三角",
  Rectangle: "矩形",
  Arrow: "箭头",
  "Arrow-2": "箭头2",
  "Arrow-3": "箭头3",
  "Star-1": "星星1",
  "Star-2": "星星2",
  Polygon: "多边形",
  Location: "定位",
  Heart: "心形",
  Bubble: "气泡",
  "Custom icon": "自定义图标",
  "Load Mask Image": "加载蒙层图片",
  Grayscale: "灰度",
  Blur: "模糊",
  Sharpen: "锐化",
  Emboss: "浮雕",
  "Remove White": "除去白色",
  Distance: "距离",
  Brightness: "亮度",
  Noise: "噪音",
  "Color Filter": "彩色滤镜",
  Sepia: "棕色",
  Sepia2: "棕色2",
  Invert: "负片",
  Pixelate: "像素化",
  Threshold: "阈值",
  Tint: "色调",
  Multiply: "正片叠底",
  Blend: "混合色",
};

// tui-image-editor自定义主题样式
export const customTheme = {
  // image 坐上角度图片
  "common.bi.image": "", // 替换logo图片 我直接不显示
  "common.bisize.width": "0px",
  "common.bisize.height": "0px",
  "common.border": "1px solid #d5cfcf",
  "common.backgroundColor": "#fff", // 整体背景颜色

  // header（头部）
  "header.backgroundColor": "#000", // 头部的背景颜色

  // load button（上传按钮）
  "loadButton.display": "none", // 可以直接隐藏掉"上传"按钮

  // download button（下载按钮）
  "downloadButton.display": "none", // 可以直接隐藏掉"下载"按钮

  // 菜单-普通状态 - 绿色
  "menu.normalIcon.color": "#2d8cf0",
  // 菜单-选中状态 - 蓝色
  "menu.activeIcon.color": "blue",
  // 菜单-禁用状态 - 灰色
  "menu.disabledIcon.color": "grey",
  // 菜单-鼠标悬浮状态 - 黄色
  "menu.hoverIcon.color": "yellow",
};