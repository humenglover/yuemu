import { message } from 'ant-design-vue';

// 防抖函数
const debounce = (fn: Function, delay: number) => {
  let timer: NodeJS.Timeout | null = null;
  return function (this: any, ...args: any[]) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
};

// 显示警告消息
const showWarning = debounce(() => {
  // message.warning('为了系统安全，该操作已被禁用');
}, 1000);

// 创建安全防护
export const createSecurityShield = () => {
  if (process.env.NODE_ENV !== 'production') {
    return;
  }

  // 禁用右键菜单
  document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    showWarning();
  });

  // 禁用开发者工具快捷键
  const blockDevTools = (e: KeyboardEvent) => {
    const isDevToolKey =
      e.key === 'F12' ||
      (e.ctrlKey && e.shiftKey && ['I', 'J', 'C'].includes(e.key)) ||
      (e.ctrlKey && e.key === 'U') ||
      (e.metaKey && e.altKey && e.key === 'I'); // Mac快捷键

    if (isDevToolKey) {
      e.preventDefault();
      showWarning();
    }
  };

  document.addEventListener('keydown', blockDevTools);

  // 监听开发者工具状态
  const devToolsCheck = () => {
    const threshold = 160;
    const widthThreshold = window.outerWidth - window.innerWidth > threshold;
    const heightThreshold = window.outerHeight - window.innerHeight > threshold;

    if (widthThreshold || heightThreshold) {
      showWarning();
    }
  };

  window.addEventListener('resize', debounce(devToolsCheck, 500));
};
