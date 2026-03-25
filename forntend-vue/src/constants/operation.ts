// 操作枚举类对应的前端常量（定义不同操作对应的数值）
export const OPERATION_ENUM = {
  DELETE: 0,
  APPROVE: 1,
  REJECT: 2
};

// 操作对应的文案映射（将数值映射到对应的文字描述）
export const OPERATION_MAP = {
  0: '删除',
  1: '通过',
  2: '拒绝'
};

// 操作下拉表单选项（根据文案映射生成适合下拉框使用的选项数组）
export const OPERATION_OPTIONS = Object.keys(OPERATION_MAP).map((key) => {
  return {
    label: OPERATION_MAP[key],
    value: key
  };
});
