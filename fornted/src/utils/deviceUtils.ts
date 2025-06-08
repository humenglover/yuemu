// 设备和浏览器检测工具

export interface DeviceInfo {
  os: {
    name: string;
    icon: string;
  };
  browser: {
    name: string;
    icon: string;
  };
}

// 检测操作系统
function detectOS(): { name: string; icon: string } {
  const userAgent = window.navigator.userAgent.toLowerCase();

  if (userAgent.indexOf('android') > -1) {
    return { name: 'Android', icon: 'android' };
  }
  if (userAgent.indexOf('iphone') > -1 || userAgent.indexOf('ipad') > -1 || userAgent.indexOf('ipod') > -1) {
    return { name: 'iOS', icon: 'apple' };
  }
  if (userAgent.indexOf('windows') > -1) {
    return { name: 'Windows', icon: 'windows' };
  }
  if (userAgent.indexOf('mac os') > -1) {
    return { name: 'macOS', icon: 'apple' };
  }
  if (userAgent.indexOf('linux') > -1) {
    return { name: 'Linux', icon: 'linux' };
  }

  return { name: 'Unknown', icon: 'question-circle' };
}

// 检测浏览器
function detectBrowser(): { name: string; icon: string } {
  const userAgent = window.navigator.userAgent.toLowerCase();

  if (userAgent.indexOf('micromessenger') > -1) {
    return { name: 'WeChat', icon: 'weixin' };
  }
  if (userAgent.indexOf('edge') > -1) {
    return { name: 'Edge', icon: 'edge' };
  }
  if (userAgent.indexOf('firefox') > -1) {
    return { name: 'Firefox', icon: 'firefox' };
  }
  if (userAgent.indexOf('safari') > -1 && userAgent.indexOf('chrome') === -1) {
    return { name: 'Safari', icon: 'safari' };
  }
  if (userAgent.indexOf('chrome') > -1) {
    return { name: 'Chrome', icon: 'chrome' };
  }

  return { name: 'Unknown', icon: 'question-circle' };
}

// 获取设备信息
export function getDeviceInfo(): DeviceInfo {
  return {
    os: detectOS(),
    browser: detectBrowser()
  };
}
