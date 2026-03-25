/**
 * 浏览时长记录工具类
 * 用于记录用户在特定内容上的浏览时长，并按梯度策略上报
 */
import { reportViewDurationUsingPost } from '@/api/viewRecordController';

export class ViewDurationTracker {
  private viewDuration = 0; // 当前累积的浏览时长（秒）
  private viewStartTime: number | null = null; // 当前浏览开始时间戳
  private durationTimer: number | null = null; // 计时器ID
  private targetType: number; // 目标类型（1-图片，2-帖子等）
  private targetId: string | number | null = null; // 目标ID
  private userId: number | null = null; // 用户ID
  // 最小上报增量阈值：新增时长达到该值才上报（秒）
  private readonly minReportIncrement = 10;
  // 梯度上报配置（单位：秒）- 键：累计浏览时长(秒)，值：上报间隔(秒)
  private readonly reportGradients = [
    { duration: 0, interval: 10 },     // 0秒以上，每10秒上报（初始值）
    { duration: 60, interval: 20 },    // 1分钟以上，每20秒上报
    { duration: 180, interval: 30 },   // 3分钟以上，每30秒上报
    { duration: 300, interval: 60 },   // 5分钟以上，每1分钟上报
    { duration: 600, interval: 300 },  // 10分钟以上，每5分钟上报
    { duration: 1800, interval: 600 }, // 30分钟以上，每10分钟上报
    { duration: 3600, interval: 1800 } // 1小时以上，每30分钟上报
  ];

  constructor(targetType: number) {
    this.targetType = targetType;
  }

  /**
   * 从本地存储获取之前未发送的浏览时长
   */
  private getStoredDuration(targetId: string | number): number {
    if (typeof targetId === 'number') {
      targetId = targetId.toString();
    }
    const stored = localStorage.getItem(`view_duration_${this.targetType}_${targetId}`);
    return stored ? parseInt(stored, 10) : 0;
  }

  /**
   * 保存浏览时长到本地存储
   */
  private storeDuration(targetId: string | number, duration: number) {
    if (typeof targetId === 'number') {
      targetId = targetId.toString();
    }
    localStorage.setItem(`view_duration_${this.targetType}_${targetId}`, duration.toString());
  }

  /**
   * 清除本地存储的浏览时长
   */
  private clearStoredDuration(targetId: string | number) {
    if (typeof targetId === 'number') {
      targetId = targetId.toString();
    }
    localStorage.removeItem(`view_duration_${this.targetType}_${targetId}`);
  }

  /**
   * 根据当前累积时长获取对应的上报间隔（秒）
   */
  private getReportInterval(currentTotalDuration: number): number {
    for (let i = this.reportGradients.length - 1; i >= 0; i--) {
      const gradient = this.reportGradients[i];
      if (currentTotalDuration >= gradient.duration) {
        return gradient.interval;
      }
    }
    return this.reportGradients[0].interval;
  }

  /**
   * 上报浏览时长，并设置下一次上报定时器
   */
  private async reportViewDuration() {
    if (!this.targetId || !this.userId) return;

    try {
      // 计算当前时间段的新增浏览时长
      let currentPeriodDuration = 0;
      if (this.viewStartTime) {
        const currentTime = Date.now();
        currentPeriodDuration = Math.floor((currentTime - this.viewStartTime) / 1000);
        this.viewStartTime = currentTime;
      }

      // 1. 新增时长未达阈值，不触发上报
      if (currentPeriodDuration < this.minReportIncrement) {
        this.viewDuration += currentPeriodDuration;
        this.setNextReportTimer();
        return;
      }

      // 2. 达到阈值，累计时长并上报
      this.viewDuration += currentPeriodDuration;
      const requestData = [{
        targetId: this.targetId,
        targetType: this.targetType,
        duration: currentPeriodDuration * 1000,
        clientTimestamp: Date.now(),
        userId: this.userId
      }];

      const res = await reportViewDurationUsingPost(requestData);
      if (res.data.code === 0) {
        console.log(`${this.targetType === 1 ? '图片' : '帖子'} ${this.targetId} 新增浏览时长 ${currentPeriodDuration} 秒已上报，累计 ${this.viewDuration} 秒`);
        this.storeDuration(this.targetId, this.viewDuration);
      } else {
        console.error('浏览时长上报失败:', res.data.message);
        this.storeDuration(this.targetId, this.viewDuration);
      }

    } catch (error) {
      console.error('浏览时长上报错误:', error);
      this.storeDuration(this.targetId, this.viewDuration);
    }

    this.setNextReportTimer();
  }

  /**
   * 设置下一次上报定时器
   */
  private setNextReportTimer() {
    if (this.durationTimer) clearTimeout(this.durationTimer);
    const nextInterval = this.getReportInterval(this.viewDuration);
    this.durationTimer = window.setTimeout(() => this.reportViewDuration(), nextInterval * 1000);
  }

  /**
   * 初始化浏览时长记录
   */
  public init(targetId: string | number, userId: number) {
    if (this.durationTimer) clearTimeout(this.durationTimer);

    this.targetId = targetId;
    this.userId = userId;
    this.viewDuration = this.getStoredDuration(targetId);
    this.viewStartTime = Date.now();

    const initialInterval = this.getReportInterval(this.viewDuration);
    this.durationTimer = window.setTimeout(() => this.reportViewDuration(), initialInterval * 1000);
    console.log(`开始记录${this.targetType === 1 ? '图片' : '帖子'} ${targetId}的浏览时长，初始上报间隔：${initialInterval}秒`);
  }

  /**
   * 清理浏览时长记录
   */
  public cleanup() {
    if (this.durationTimer) clearTimeout(this.durationTimer);
    this.durationTimer = null;

    if (this.targetId && this.userId && this.viewStartTime) {
      const currentTime = Date.now();
      const currentPeriodDuration = Math.floor((currentTime - this.viewStartTime) / 1000);
      this.viewDuration += currentPeriodDuration;
      this.viewStartTime = null;

      if (this.viewDuration > 0) {
        this.tryFinalReport();
        this.storeDuration(this.targetId, this.viewDuration);
        console.log(`${this.targetType === 1 ? '图片' : '帖子'} ${this.targetId} 最终累计浏览时长 ${this.viewDuration} 秒已保存到本地`);
      }
    }
  }

  /**
   * 最后一次尝试上报（同步）
   */
  private async tryFinalReport() {
    if (!this.targetId || !this.userId || this.viewDuration <= 0) return;

    try {
      const requestData = [{
        targetId: this.targetId,
        targetType: this.targetType,
        duration: this.viewDuration * 1000,
        clientTimestamp: Date.now(),
        userId: this.userId
      }];

      const res = await reportViewDurationUsingPost(requestData);
      if (res.data.code === 0) {
        console.log(`${this.targetType === 1 ? '图片' : '帖子'} ${this.targetId} 最终浏览时长 ${this.viewDuration} 秒上报成功`);
        this.clearStoredDuration(this.targetId);
        this.viewDuration = 0;
      }
    } catch (error) {
      console.error('最终浏览时长上报失败，已保存到本地:', error);
    }
  }

  /**
   * 获取当前累积的浏览时长（秒）
   */
  public getCurrentDuration(): number {
    let currentDuration = this.viewDuration;
    if (this.viewStartTime) {
      const currentTime = Date.now();
      currentDuration += Math.floor((currentTime - this.viewStartTime) / 1000);
    }
    return currentDuration;
  }

  /**
   * 手动触发一次上报（可外部调用）
   */
  public async manualReport(): Promise<boolean> {
    if (!this.targetId || !this.userId) return false;

    if (this.durationTimer) clearTimeout(this.durationTimer);

    if (this.viewStartTime) {
      const currentTime = Date.now();
      const currentPeriodDuration = Math.floor((currentTime - this.viewStartTime) / 1000);
      this.viewDuration += currentPeriodDuration;
      this.viewStartTime = currentTime;
    }

    if (this.viewDuration < this.minReportIncrement) {
      this.setNextReportTimer();
      return false;
    }

    try {
      const requestData = [{
        targetId: this.targetId,
        targetType: this.targetType,
        duration: this.viewDuration * 1000,
        clientTimestamp: Date.now(),
        userId: this.userId
      }];

      const res = await reportViewDurationUsingPost(requestData);
      if (res.data.code === 0) {
        console.log(`手动上报${this.targetType === 1 ? '图片' : '帖子'} ${this.targetId} 浏览时长 ${this.viewDuration} 秒成功`);
        this.clearStoredDuration(this.targetId);
        this.viewDuration = 0;
        this.setNextReportTimer();
        return true;
      }
    } catch (error) {
      console.error('手动上报浏览时长失败:', error);
    }

    this.setNextReportTimer();
    return false;
  }
}
