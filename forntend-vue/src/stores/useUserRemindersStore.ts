import { defineStore } from 'pinia';

interface State {
  // 存储用户已关闭的提醒类型
  closedReminders: string[];
}

export const useUserRemindersStore = defineStore('userReminders', {
  state: (): State => ({
    closedReminders: [],
  }),

  actions: {
    // 初始化时从localStorage加载数据
    init() {
      const stored = localStorage.getItem('userClosedReminders');
      if (stored) {
        try {
          this.closedReminders = JSON.parse(stored);
        } catch (e) {
          console.error('Failed to parse userClosedReminders from localStorage', e);
          this.closedReminders = [];
        }
      }
    },

    // 检查指定提醒是否已被关闭
    isReminderClosed(key: string): boolean {
      return this.closedReminders.includes(key);
    },

    // 标记指定提醒为已关闭
    closeReminder(key: string) {
      if (!this.closedReminders.includes(key)) {
        this.closedReminders.push(key);
        this.saveToLocalStorage();
      }
    },

    // 标记指定提醒为未关闭
    openReminder(key: string) {
      const index = this.closedReminders.indexOf(key);
      if (index > -1) {
        this.closedReminders.splice(index, 1);
        this.saveToLocalStorage();
      }
    },

    // 保存到localStorage
    saveToLocalStorage() {
      try {
        localStorage.setItem('userClosedReminders', JSON.stringify(this.closedReminders));
      } catch (e) {
        console.error('Failed to save userClosedReminders to localStorage', e);
      }
    },
  },
});