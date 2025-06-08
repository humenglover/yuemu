export class CommonUtil {
  static isMobile(): boolean {
    return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
  }

  static randomNum(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1)) + min
  }
}
