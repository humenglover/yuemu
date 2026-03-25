// back.ts
import router from '@/router'; // 引入路由实例，确保路径正确

export const handleBackButton = () => {
  return new Promise<void>((resolve, reject) => {
    document.addEventListener('plusready', () => {
      const webview = plus.webview.currentWebview();
      plus.key.addEventListener('backbutton', () => {
        const currentRoute = router.currentRoute.value.path; // 获取当前路由路径
        if (currentRoute === '/') {
          // 使用window.confirm展示确认框，在浏览器环境下提供确认和取消功能
          const confirmResult = window.confirm('确认退出吗？');
          if (confirmResult) {
            webview.close(); // 用户点击确认按钮时，关闭应用
          }
        } else {
          webview.canBack((e: { canBack: boolean }) => {
            if (e.canBack) {
              webview.back(-1); // 返回上一页
            } else {
              webview.close();
            }
          });
        }
        resolve();
      });
    });
  });
};
