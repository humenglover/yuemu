// 帖子状态枚举
export const POST_STATUS_ENUM = {
  REVIEWING: 0, // 待审核
  PASS: 1,     // 已通过
  REJECT: 2,   // 已拒绝
}

// 帖子状态映射
export const POST_STATUS_MAP = {
  [POST_STATUS_ENUM.REVIEWING]: '待审核',
  [POST_STATUS_ENUM.PASS]: '已通过',
  [POST_STATUS_ENUM.REJECT]: '已拒绝',
}

// 帖子状态选项
export const POST_STATUS_OPTIONS = [
  { label: '待审核', value: POST_STATUS_ENUM.REVIEWING },
  { label: '已通过', value: POST_STATUS_ENUM.PASS },
  { label: '已拒绝', value: POST_STATUS_ENUM.REJECT },
]
