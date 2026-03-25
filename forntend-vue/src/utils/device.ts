export function getDeviceType(): 'mobile' | 'pc' {
  // 获取当前窗口的宽度，优先使用 window.innerWidth，如果不存在则依次尝试 document.documentElement.clientWidth 和 document.body.clientWidth
  const screenWidth: number = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;

  // 定义一个阈值，小于该宽度的设备被认为是移动端，可根据实际需求调整
  const maxMobileWidth: number = 768;

  // 根据屏幕宽度判断设备类型
  return screenWidth <= maxMobileWidth? 'mobile' : 'pc';
}
// export function getDeviceType(): 'mobile' | 'pc' {
//   const userAgent: string = navigator.userAgent.toLowerCase();
//   const mobileKeywords: string[] = [
//     'mobile',
//     'android',
//     'iphone',
//     'ipad',
//     'ipod',
//     'windows phone',
//     'blackberry',
//     'ucbrowser',
//     'fennec',
//     'opera mobi',
//     'opera mini',
//     'iemobile'
//   ];
//   const isMatchKeyword: boolean = mobileKeywords.some((keyword: string) => userAgent.includes(keyword));
//
//   const screenWidth: number = (window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth) as number;
//   const maxMobileWidth: number = 768; // 假设这个宽度作为移动端的阈值，可根据实际情况调整
//
//   return isMatchKeyword && screenWidth <= maxMobileWidth? 'mobile' : 'pc';
// }
