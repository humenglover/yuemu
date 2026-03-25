<template>
  <div id="SettingView">
    <!-- 主内容区 -->
    <div class="setting-container">
      <!-- 用户信息卡片 -->
      <div class="user-card" @click="openModal">
        <div class="avatar-wrapper">
          <a-avatar
            :src="loginUserStore.loginUser.userAvatar || getDefaultAvatar(loginUserStore.loginUser.userName)"
            :size="72"
            class="user-avatar"
          />
          <div class="edit-overlay">
            <i class="fas fa-pen edit-icon"></i>
          </div>
        </div>
        <div class="user-info">
          <div class="user-name">{{ loginUserStore.loginUser.userName }}</div>
          <div class="user-id">ID: {{ loginUserStore.loginUser.id }}</div>
          <div class="user-stats">
            <span class="stat-item" @click.stop="handleFollowClick">
              <strong>{{ followAndFans.followCount || 0 }}</strong> 关注
            </span>
            <span class="divider">|</span>
            <span class="stat-item" @click.stop="handleFansClick">
              <strong>{{ followAndFans.fansCount || 0 }}</strong> 粉丝
            </span>
          </div>
        </div>
        <i class="fas fa-chevron-right arrow-icon"></i>
      </div>

      <!-- 重构：二级折叠菜单功能区 -->
      <div class="function-accordion">
        <!-- 1. 账号安全（默认展开） -->
        <div class="accordion-panel">
          <div class="accordion-header" @click="togglePanel('account')">
            <div class="header-left">
              <i class="fas fa-lock panel-icon"></i>
              <span class="panel-title">账号安全</span>
            </div>
            <div class="header-right">
              <i v-if="activePanel === 'account'" class="fas fa-chevron-down panel-arrow"></i>
              <i v-else class="fas fa-chevron-right panel-arrow"></i>
            </div>
          </div>
          <div class="accordion-content" v-show="activePanel === 'account'">
            <div class="list-item" @click="modifyPasswordOpen = true">
              <div class="item-icon-wrapper">
                <i class="fas fa-key item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">修改密码</div>
                <div class="item-desc">保障账号安全</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="changeEmailOpen = true">
              <div class="item-icon-wrapper">
                <i class="fas fa-envelope item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">修改邮箱</div>
                <div class="item-desc">{{ myMessage.email || '未绑定' }}</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="handleWxItemClick">
              <div class="item-icon-wrapper wechat-icon">
                <i class="fab fa-weixin item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">微信绑定</div>
                <div class="item-desc">{{ loginUserStore.loginUser.hasBindWx ? '已绑定微信' : '未绑定微信，支持扫码快速登录' }}</div>
              </div>
              <div v-if="loginUserStore.loginUser.hasBindWx" class="bind-status success">已绑定</div>
              <i v-else class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item">
              <div class="item-icon-wrapper system-icon">
                <i class="fas fa-desktop item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">多端登录控制</div>
                <div class="item-desc">{{ myMessage.allowMultiDeviceLogin ? '允许多设备登录' : '仅单设备登录' }}</div>
              </div>
              <a-switch
                :checked="myMessage.allowMultiDeviceLogin === 1"
                size="small"
                @change="toggleMultiDeviceLogin"
              />
            </div>
            <div class="list-item logout-item" @click="logoutModalOpen = true">
              <div class="item-icon-wrapper logout-icon">
                <i class="fas fa-sign-out-alt item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">退出登录</div>
                <div class="item-desc">下次需要重新登录才能继续使用</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item destroy-item" @click="secureDestroyOpen = true">
              <div class="item-icon-wrapper destroy-icon">
                <i class="fas fa-trash-alt item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text danger-text">注销账号</div>
                <div class="item-desc danger-text">永久删除所有数据</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>
        </div>

        <!-- 2. 个性化设置 -->
        <div class="accordion-panel">
          <div class="accordion-header" @click="togglePanel('personal')">
            <div class="header-left">
              <i class="fas fa-user panel-icon"></i>
              <span class="panel-title">个性化设置</span>
            </div>
            <div class="header-right">
              <i v-if="activePanel === 'personal'" class="fas fa-chevron-down panel-arrow"></i>
              <i v-else class="fas fa-chevron-right panel-arrow"></i>
            </div>
          </div>
          <div class="accordion-content" v-show="activePanel === 'personal'">
            <div class="list-item" @click="themeStore.toggleTheme">
              <div class="item-icon-wrapper system-icon">
                <i class="fas fa-lightbulb item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">主题切换</div>
                <div class="item-desc">{{ themeStore.isDarkTheme ? '当前：深色模式' : '当前：浅色模式' }}</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="showStreamRecommendations">
              <div class="item-icon-wrapper system-icon">
                <i class="fas fa-cog item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">消息流推荐</div>
                <div class="item-desc">个性化配置</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>
        </div>

        <!-- 3. 内容管理 -->
        <div class="accordion-panel">
          <div class="accordion-header" @click="togglePanel('content')">
            <div class="header-left">
              <i class="fas fa-file-alt panel-icon"></i>
              <span class="panel-title">内容管理</span>
            </div>
            <div class="header-right">
              <i v-if="activePanel === 'content'" class="fas fa-chevron-down panel-arrow"></i>
              <i v-else class="fas fa-chevron-right panel-arrow"></i>
            </div>
          </div>
          <div class="accordion-content" v-show="activePanel === 'content'">
            <div class="list-item" @click="handleLoveBoard">
              <div class="item-icon-wrapper love-icon">
                <i class="fas fa-heart item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">恋爱画板</div>
                <div class="item-desc">记录美好瞬间</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="showCalendar = true">
              <div class="item-icon-wrapper sign-icon">
                <i class="fas fa-calendar item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">成长足迹</div>
                <div class="item-desc">查看签到记录</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="handlePrivacySettingClick">
              <div class="item-icon-wrapper privacy-icon">
                <i class="fas fa-shield-alt item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">隐私设置</div>
                <div class="item-desc">管理个人隐私权限</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="handleReportCenter">
              <div class="item-icon-wrapper system-icon">
                <i class="fas fa-exclamation-circle item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">举报中心</div>
                <div class="item-desc">举报不当内容及查看处理结果</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
            <div class="list-item" @click="handleBrowseHistory">
              <div class="item-icon-wrapper system-icon">
                <i class="fas fa-history item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">浏览历史</div>
                <div class="item-desc">查看我的浏览记录</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>
        </div>

        <!-- 4. 系统服务 -->
        <div class="accordion-panel">
          <div class="accordion-header" @click="togglePanel('system')">
            <div class="header-left">
              <i class="fas fa-info-circle panel-icon"></i>
              <span class="panel-title">系统服务</span>
            </div>
            <div class="header-right">
              <i v-if="activePanel === 'system'" class="fas fa-chevron-down panel-arrow"></i>
              <i v-else class="fas fa-chevron-right panel-arrow"></i>
            </div>
          </div>
          <div class="accordion-content" v-show="activePanel === 'system'">
            <div class="list-item" @click="aboutUsOpen = true">
              <div class="item-icon-wrapper about-icon">
                <i class="fas fa-info-circle item-icon"></i>
              </div>
              <div class="item-content">
                <div class="item-text">关于悦木</div>
                <div class="item-desc">了解我们</div>
              </div>
              <i class="fas fa-chevron-right arrow-icon"></i>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑资料弹窗 (X-style 重构版) -->
    <div v-if="open" class="modal-mask glass-mask" @click="open = false">
      <div class="modal-content profile-edit-modal" @click.stop>
        <div class="modal-header compact-header">
          <span class="modal-title">编辑资料</span>
          <i class="fas fa-times close-btn" @click="open = false"></i>
        </div>

        <!-- 沉浸式顶部：封面图 + 叠加头像 -->
        <div class="profile-hero-section">
          <!-- 封面图 -->
          <div class="hero-cover" @click="showBgFileUploadDialog">
            <img v-if="myMessage.homepageBg" :src="myMessage.homepageBg" class="cover-image" alt="背景图" />
            <div v-else class="cover-placeholder"></div>
            <div class="cover-overlay">
              <i class="fas fa-camera"></i>
            </div>
            <!-- 隐藏的 input -->
            <input type="file" ref="bgFileInput" style="display: none" accept="image/*" @change="handleBgImageUpload" />
          </div>

          <!-- 头像 -->
          <div class="hero-avatar-wrapper">
            <div class="avatar-upload-circle" @click="showFileUploadDialog">
              <img :src="myMessage.userAvatar || getDefaultAvatar(myMessage.userName)" class="hero-avatar" alt="头像" />
              <div class="avatar-overlay">
                <i class="fas fa-camera"></i>
              </div>
            </div>
            <input type="file" ref="fileInput" style="display: none" accept="image/*" @change="handleAvatarUpload" />
          </div>
        </div>

        <div class="modal-body scrollable-body form-compact">
          <div class="form-item">
            <label class="form-label">昵称</label>
            <input v-model="myMessage.userName" class="form-input soft-input" type="text" placeholder="请输入昵称" maxlength="30" />
            <div class="char-count">{{ myMessage.userName ? myMessage.userName.length : 0 }}/30</div>
          </div>

          <div class="form-item">
            <label class="form-label">简介</label>
            <textarea v-model="myMessage.userProfile" class="form-input soft-input textarea-input" placeholder="介绍一下自己" maxlength="520" rows="3"></textarea>
            <div class="char-count">{{ myMessage.userProfile ? myMessage.userProfile.length : 0 }}/520</div>
          </div>

          <div class="form-item">
            <label class="form-label">性别</label>
            <div class="gender-segmented-control">
              <div class="segment-pill" :class="{ 'active': myMessage.gender === '' }" @click="myMessage.gender = ''">保密</div>
              <div class="segment-pill" :class="{ 'active': myMessage.gender === '男' }" @click="myMessage.gender = '男'">男</div>
              <div class="segment-pill" :class="{ 'active': myMessage.gender === '女' }" @click="myMessage.gender = '女'">女</div>
              <div class="segment-pill" :class="{ 'active': myMessage.gender === '其他' }" @click="myMessage.gender = '其他'">其他</div>
            </div>
          </div>

          <div class="form-item row-group">
            <div class="flex-1">
              <label class="form-label">生日</label>
              <a-date-picker v-model:value="birthdayDate" class="form-input soft-input w-full date-picker-custom" placeholder="请选择生日" format="YYYY-MM-DD" value-format="YYYY-MM-DD" :bordered="false" />
            </div>
            <div class="flex-1">
              <label class="form-label">地区</label>
              <div class="input-with-button location-group">
                <input v-model="myMessage.region" class="form-input soft-input" type="text" placeholder="所在地区" maxlength="50" />
                <button class="location-btn inset-btn" @click="getLocation" :disabled="locationLoading" title="获取定位">
                  <i v-if="!locationLoading" class="fas fa-map-marker-alt"></i>
                  <i v-else class="fas fa-spinner fa-spin"></i>
                </button>
              </div>
            </div>
          </div>

          <div class="modal-footer-actions">
            <button class="pill-submit-btn" @click="editProfile">保存修改</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="modifyPasswordOpen" class="modal-mask" @click="modifyPasswordOpen = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">修改密码</span>
          <i class="fas fa-times close-btn" @click="modifyPasswordOpen = false"></i>
        </div>
        <div class="modal-body scrollable-body form-compact">
          <!-- 针对微信用户的密码提示 -->
          <div class="notice-card info-notice">
            <div class="notice-header">
              <i class="fas fa-info-circle"></i>
              <span>微信用户须知</span>
            </div>
            <p class="notice-desc">
              若您是通过微信快捷入驻，初始密码默认为 <span class="highlight-text">12345678</span>。请使用此密码作为“当前密码”进行修改。
            </p>
          </div>

          <div class="form-item">
            <label class="form-label">当前密码</label>
            <input v-model="modifyPasswordFormData.oldPassword" class="soft-input" type="password" placeholder="请输入当前密码" />
          </div>
          <div class="form-item">
            <label class="form-label">新密码</label>
            <input v-model="modifyPasswordFormData.newPassword" class="soft-input" type="password" placeholder="8-20位字母数字组合" />
          </div>
          <div class="form-item">
            <label class="form-label">确认新密码</label>
            <input v-model="modifyPasswordFormData.checkPassword" class="soft-input" type="password" placeholder="再次输入新密码" />
          </div>

          <div class="modal-footer-actions">
            <button class="pill-submit-btn" @click="submitPasswordForm">确认修改</button>
            <div class="footer-link-wrapper">
              <button class="link-btn" @click="handleForgotPassword">忘记密码？</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 修改邮箱弹窗 -->
    <div v-if="changeEmailOpen" class="modal-mask" @click="changeEmailOpen = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">修改邮箱</span>
          <i class="fas fa-times close-btn" @click="changeEmailOpen = false"></i>
        </div>
        <div class="modal-body scrollable-body form-compact">
          <div class="form-item">
            <label class="form-label">新邮箱地址</label>
            <input v-model="changeEmailForm.newEmail" class="soft-input" type="email" placeholder="请输入新邮箱" />
          </div>
          <div class="form-item">
            <label class="form-label">验证码</label>
            <div class="verify-group">
              <input v-model="changeEmailForm.code" class="soft-input verify-input" type="text" placeholder="6位验证码" maxlength="6" />
              <button
                class="send-code-btn pill-send-btn"
                :disabled="!!countdown"
                @click="sendEmailCode"
              >
                {{ countdown ? `${countdown}s` : '获取' }}
              </button>
            </div>
          </div>

          <div class="modal-footer-actions">
            <button class="pill-submit-btn" @click="handleChangeEmail">确认修改</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 关于悦木弹窗 -->
    <div v-if="aboutUsOpen" class="modal-mask" @click="aboutUsOpen = false">
      <div class="modal-content about-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">关于悦木</span>
          <i class="fas fa-times close-btn" @click="aboutUsOpen = false"></i>
        </div>
        <div class="modal-body">
          <div class="app-logo">
            <i class="fas fa-heart logo-icon"></i>
          </div>
          <h3>悦木图片分享</h3>
          <p class="version">Version 1.0.0</p>
          <p class="desc">记录生活，分享感动</p>
          <div class="divider"></div>
          <p class="info-item">
            <i class="fas fa-envelope"></i>
            <span>联系邮箱：<a href="mailto:109484028@qq.com">109484028@qq.com</a></span>
          </p>
          <p class="info-item">
            <i class="fas fa-globe"></i>
            <a href="https://beian.miit.gov.cn/" target="_blank">{{ getBeianNumber() }}</a>
          </p>
          <p class="copyright">© {{ currentYear }} 鹿梦. All rights reserved.</p>
          <div class="about-buttons">
            <button class="about-btn" @click="handleAuClick">了解作者</button>
            <button class="about-btn" @click="openOfficialDocs">官方文档</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 消息流推荐弹窗 -->
    <div v-if="streamRecommendationsOpen" class="modal-mask" @click="streamRecommendationsOpen = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">消息流推荐</span>
          <i class="fas fa-times close-btn" @click="streamRecommendationsOpen = false"></i>
        </div>
        <div class="modal-body">
          <div class="setting-section">
            <div class="setting-item">
              <div class="layout-preview-container">
                <!-- 瀑布流预览 -->
                <div
                  class="layout-option"
                  :class="{ active: streamLayout === 'waterfall' }"
                  @click="selectLayout('waterfall')"
                >
                  <div class="preview-title">瀑布流</div>
                  <div class="preview-content waterfall-preview">
                    <div class="preview-item tall"></div>
                    <div class="preview-item short"></div>
                    <div class="preview-item medium"></div>
                    <div class="preview-item short"></div>
                    <div class="preview-item tall"></div>
                    <div class="preview-item medium"></div>
                  </div>
                  <div class="layout-description">高度不一，自然流动</div>
                </div>

                <!-- 多列等高预览 -->
                <div
                  v-if="layoutOptions.find(option => option.value === 'grid')"
                  class="layout-option"
                  :class="{ active: streamLayout === 'grid' }"
                  @click="selectLayout('grid')"
                >
                  <div class="preview-title">多列等高</div>
                  <div class="preview-content grid-preview">
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                  </div>
                  <div class="layout-description">整齐划一，等高排列</div>
                  <!-- 等高比例选择 -->
                  <div v-if="streamLayout === 'grid'" class="aspect-ratio-selector">
                    <div class="ratio-options">
                      <div
                        v-for="ratio in aspectRatioOptions"
                        :key="ratio.value"
                        class="ratio-option"
                        :class="{ active: gridAspectRatio === ratio.value }"
                        @click.stop="selectAspectRatio(ratio.value)"
                      >
                        {{ ratio.label }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 单列展示预览 -->
                <div
                  v-if="layoutOptions.find(option => option.value === 'single')"
                  class="layout-option"
                  :class="{ active: streamLayout === 'single' }"
                  @click="selectLayout('single')"
                >
                  <div class="preview-title">单列展示</div>
                  <div class="preview-content single-preview">
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                    <div class="preview-item"></div>
                  </div>
                  <div class="layout-description">单列浏览，专注体验</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 成长足迹弹窗（重构版） -->
    <div v-if="showCalendar" class="modal-mask" @click="showCalendar = false">
      <div class="modal-content calendar-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">成长足迹</span>
          <a-select v-model:value="selectedYear" :options="yearOptions" class="year-selector" @change="fetchSignInData" />
          <i class="fas fa-times close-btn" @click="showCalendar = false"></i>
        </div>
        <div class="modal-body calendar-body">
          <!-- 重构日历：月份分行展示 -->
          <div class="calendar-wrapper">
            <div class="month-grid">
              <!-- 循环渲染12个月 -->
              <div class="month-item" v-for="month in 12" :key="month">
                <div class="month-title">{{ month }}月</div>
                <div class="week-header">
                  <span class="week-day">一</span>
                  <span class="week-day">二</span>
                  <span class="week-day">三</span>
                  <span class="week-day">四</span>
                  <span class="week-day">五</span>
                  <span class="week-day">六</span>
                  <span class="week-day">日</span>
                </div>
                <div class="date-grid">
                  <!-- 计算每月1号是星期几 -->
                  <div
                    class="date-item empty"
                    v-for="(empty, idx) in getMonthStartWeek(month)"
                    :key="`empty-${month}-${idx}`"
                  ></div>
                  <!-- 渲染当月日期 -->
                  <div
                    class="date-item"
                    :class="{ signed: isSigned(month, day) }"
                    v-for="day in getDaysInMonth(month)"
                    :key="`date-${month}-${day}`"
                  >
                    {{ day }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认退出弹窗 -->
    <div v-if="logoutModalOpen" class="modal-mask" @click="logoutModalOpen = false">
      <div class="confirm-content" @click.stop>
        <div class="confirm-title">确认退出登录？</div>
        <div class="confirm-desc">退出后需要重新登录才能继续使用</div>
        <div class="confirm-btns">
          <button class="cancel-btn" @click="logoutModalOpen = false">取消</button>
          <button class="confirm-btn" @click="handleLogoutConfirm">确认退出</button>
        </div>
      </div>
    </div>

    <!-- 注销账号确认 -->
    <div v-if="logoutConfirmOpen" class="modal-mask" @click="logoutConfirmOpen = false">
      <div class="confirm-content danger-confirm" @click.stop>
        <div class="confirm-title">确认注销账号？</div>
        <div class="confirm-desc">此操作不可撤销，所有数据将被永久删除</div>
        <div class="confirm-btns">
          <button class="cancel-btn" @click="logoutConfirmOpen = false">取消</button>
          <button class="confirm-btn danger-btn" @click="confirmLogout">确认注销</button>
        </div>
      </div>
    </div>

    <!-- 安全注销弹窗 -->
    <div v-if="secureDestroyOpen" class="modal-mask" @click="secureDestroyOpen = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <span class="modal-title">安全注销账号</span>
          <i class="fas fa-times close-btn" @click="secureDestroyOpen = false"></i>
        </div>
        <div class="modal-body scrollable-body form-compact">
          <div class="form-item">
            <label class="form-label">当前密码</label>
            <input v-model="secureDestroyForm.userPassword" class="soft-input" type="password" placeholder="请输入当前密码" />
          </div>
          <div class="form-item">
            <label class="form-label">邮箱验证码</label>
            <div class="verify-group">
              <input v-model="secureDestroyForm.code" class="soft-input verify-input" type="text" placeholder="6位验证码" maxlength="6" />
              <button
                class="send-code-btn pill-send-btn"
                :disabled="!!destroyCodeCountdown"
                @click="sendDestroyCode"
              >
                {{ destroyCodeCountdown ? `${destroyCodeCountdown}s` : '获取' }}
              </button>
            </div>
          </div>

          <div class="notice-card warning-notice">
            <div class="notice-header">
              <i class="fas fa-exclamation-triangle"></i>
              <span>安全提示</span>
            </div>
            <p class="notice-desc">为了您的账户安全，注销前需进行身份验证。此操作不可撤销。</p>
          </div>

          <div class="modal-footer-actions vertical-actions">
            <button class="pill-submit-btn danger-pill-btn" @click="confirmSecureDestroy">确认注销</button>
            <button class="pill-cancel-btn" @click="secureDestroyOpen = false">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户权限设置弹窗 -->
    <div v-if="userPermissionSettingOpen" class="modal-mask" @click="userPermissionSettingOpen = false">
      <div class="modal-content permission-setting-modal" @click.stop>
        <div class="modal-header">
          <span class="modal-title">隐私权限设置</span>
          <i class="fas fa-times close-btn" @click="userPermissionSettingOpen = false"></i>
        </div>
        <div class="modal-body permission-setting-body">
          <UserPermissionSetting
            v-if="userPermissionSettingOpen"
            :user-id="loginUserStore.loginUser.id"
            :initial-permissions="{
              allowPrivateChat: myMessage.allowPrivateChat,
              allowFollow: myMessage.allowFollow,
              showFollowList: myMessage.showFollowList,
              showFansList: myMessage.showFansList
            }"
            @saved="() => { userPermissionSettingOpen = false; message.success('权限设置保存成功') }"
            @cancelled="() => userPermissionSettingOpen = false"
          />
        </div>
      </div>
    </div>

    <!-- 微信绑定弹窗 -->
    <div v-if="bindWxModalOpen" class="modal-mask" @click="closeWxBindModal">
      <div class="modal-content wx-bind-modal" @click.stop>
        <div class="modal-header">
          <span class="modal-title">微信绑定</span>
          <i class="fas fa-times close-btn" @click="closeWxBindModal"></i>
        </div>
        <div class="modal-body">
          <div class="wx-bind-info" style="text-align: center;">
            <div class="qr-code-wrapper" style="margin: 0 auto 20px;">
              <img src="@/assets/wx.png" alt="公众号二维码" class="qr-code" />
            </div>
            <p style="font-size: 14px; color: var(--text-secondary); margin-bottom: 24px;">微信扫码关注，并发送下方验证码</p>

            <div class="code-display" @click="fetchWxBindReqCode" style="background: var(--bg-color-secondary); border: 1px dashed var(--border-color); border-radius: 12px; padding: 12px 24px; display: inline-block; cursor: pointer; transition: all 0.3s;">
              <span class="code-text" :class="{ 'highlight': wxBindReqCode !== '获取中...' && wxBindReqCode !== '已过期' }" style="font-size: 32px; font-weight: 700; letter-spacing: 4px; color: var(--text-secondary); transition: color 0.3s;">
                {{ wxBindReqCode }}
              </span>
            </div>
            <p class="refresh-tip" v-if="wxBindReqCode === '已过期'" @click="fetchWxBindReqCode" style="font-size: 13px; color: var(--primary-color); cursor: pointer; margin-top: 12px;">点击重新获取验证码</p>

            <div style="margin-top: 24px; display: flex; align-items: center; justify-content: center; gap: 8px; background: rgba(var(--primary-rgb), 0.04); padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(var(--primary-rgb), 0.1);">
              <span style="font-size: 11px; background: #10b981; color: #fff; padding: 2px 6px; border-radius: 4px; font-weight: 600;">极速绑定</span>
              <span style="font-size: 12px; color: var(--text-secondary);">发送后网页将自动完成绑定</span>
            </div>

            <p class="step-hint" style="margin-top: 24px; font-size: 12px; color: var(--text-tertiary); border-top: 1px dashed var(--border-color); padding-top: 12px;">
              * 微信快捷入驻用户的初始密码默认为 <span style="color: var(--primary-color);">12345678</span>
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 微信解绑验证 -->
    <div v-if="unbindWxConfirmOpen" class="modal-mask" @click="closeWxUnbindModal">
      <div class="modal-content wx-bind-modal" @click.stop>
        <div class="modal-header">
          <span class="modal-title">微信安全解绑</span>
          <i class="fas fa-times close-btn" @click="closeWxUnbindModal"></i>
        </div>
        <div class="modal-body">
          <div class="wx-bind-info" style="text-align: center;">
            <div class="qr-code-wrapper" style="margin: 0 auto 20px;">
              <img src="@/assets/wx.png" alt="公众号二维码" class="qr-code" />
            </div>
            <p style="font-size: 14px; color: var(--text-secondary); margin-bottom: 24px;">出于安全考虑，请使用当前绑定的微信扫码并发送下方验证码</p>

            <div class="code-display" @click="fetchWxUnbindReqCode" style="background: var(--bg-color-secondary); border: 1px dashed var(--border-color); border-radius: 12px; padding: 12px 24px; display: inline-block; cursor: pointer; transition: all 0.3s;">
              <span class="code-text" :class="{ 'highlight': wxUnbindReqCode !== '获取中...' && wxUnbindReqCode !== '已过期' }" style="font-size: 32px; font-weight: 700; letter-spacing: 4px; color: var(--text-secondary); transition: color 0.3s;">
                {{ wxUnbindReqCode }}
              </span>
            </div>
            <p class="refresh-tip" v-if="wxUnbindReqCode === '已过期'" @click="fetchWxUnbindReqCode" style="font-size: 13px; color: var(--primary-color); cursor: pointer; margin-top: 12px;">点击重新获取验证码</p>

            <div style="margin-top: 24px; display: flex; align-items: center; justify-content: center; gap: 8px; background: rgba(239, 68, 68, 0.04); padding: 8px 16px; border-radius: 8px; border: 1px solid rgba(239, 68, 68, 0.1);">
              <span style="font-size: 11px; background: #ef4444; color: #fff; padding: 2px 6px; border-radius: 4px; font-weight: 600;">验证解绑</span>
              <span style="font-size: 12px; color: var(--text-secondary);">发送后网页将自动解绑账号</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 头像裁剪组件 -->
    <AvatarCropper
      ref="avatarCropperRef"
      :imageUrl="tempImageUrl"
      @success="handleCroppedAvatar"
    />
  </div>
</template>

<script setup lang="ts">
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { useThemeStore } from '@/stores/useThemeStore'
import { ref, onMounted, computed, reactive, onBeforeUnmount, watch, provide } from 'vue'
import { addUserSignInUsingPost, userLogoutUsingPost } from '@/api/userController'
import { message, Switch } from 'ant-design-vue'
import {
  changePasswordUsingPost,
  updateUserUsingPost,
  userDestroyUsingPost,
  updateUserAvatarUsingPost,
  getUserSignInRecordUsingGet,
  getEmailCodeUsingPost,
  changeEmailUsingPost,
  getDestroyCodeUsingPost,
  userDestroySecureUsingPost,
  updateUserMultiDeviceLoginUsingPost,
  getUserMultiDeviceLoginUsingGet,
  reqWxBindCodeUsingGet,
  checkWxBindStatusUsingGet,
  reqWxUnbindCodeUsingGet,
  checkWxUnbindStatusUsingGet,
} from '@/api/userController'
import { uploadPostImageUsingPost } from '@/api/pictureController'
import router from '@/router'
import type { UserModifyPassWord } from '@/api/API.ts'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { getFollowAndFansCountUsingPost } from '@/api/userFollowsController'
import { getUserPermissionsUsingGet } from '@/api/userController'
import AvatarCropper from '@/components/AvatarCropper.vue'
import UserPermissionSetting from '@/components/UserPermissionSetting.vue'
import { getCurrentYear } from '@/utils/date'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

// 状态管理
const loginUserStore = useLoginUserStore()
const themeStore = useThemeStore()
const open = ref(false)
const modifyPasswordOpen = ref(false)
const changeEmailOpen = ref(false)
const aboutUsOpen = ref(false)
const logoutModalOpen = ref(false)
const logoutConfirmOpen = ref(false)
const showCalendar = ref(false)
const streamRecommendationsOpen = ref(false)
const bindWxModalOpen = ref(false)
const unbindWxConfirmOpen = ref(false)

// 微信绑定相关状态
const wxBindReqCode = ref<string>('获取中...')
const wxBindSceneId = ref<string>('')
let wxBindCheckTimer: any = null

const fetchWxBindReqCode = async () => {
  wxBindReqCode.value = '获取中...'
  const res = await reqWxBindCodeUsingGet()
  if (res.data.code === 0 && res.data.data) {
    wxBindReqCode.value = res.data.data.code as string
    wxBindSceneId.value = res.data.data.sceneId as string
    startWxBindCheckTimer()
  } else {
    message.error('获取验证码失败: ' + res.data.message)
    wxBindReqCode.value = '已过期'
  }
}

const startWxBindCheckTimer = () => {
  stopWxBindCheckTimer()
  wxBindCheckTimer = setInterval(async () => {
    if (!wxBindSceneId.value) return
    const res = await checkWxBindStatusUsingGet({ sceneId: wxBindSceneId.value })
    if (res.data.code === 0) {
      if (res.data.data) {
        // 返回了 true 说明绑定成功
        stopWxBindCheckTimer()
        message.success('绑定成功')
        bindWxModalOpen.value = false
        // 更新前端状态
        const newUserInfo = { ...loginUserStore.loginUser, mpOpenId: 'bound', hasBindWx: true }
        loginUserStore.setLoginUser(newUserInfo)
      }
    } else {
      stopWxBindCheckTimer()
      message.error(res.data.message || '验证码已过期，请刷新重试')
      wxBindReqCode.value = '已过期'
    }
  }, 2000)
}

const stopWxBindCheckTimer = () => {
  if (wxBindCheckTimer) {
    clearInterval(wxBindCheckTimer)
    wxBindCheckTimer = null
  }
}

const handleOpenBindWx = () => {
  bindWxModalOpen.value = true
  fetchWxBindReqCode()
}

const closeWxBindModal = () => {
  bindWxModalOpen.value = false
  stopWxBindCheckTimer()
}

// 微信解绑相关状态
const wxUnbindReqCode = ref<string>('获取中...')
const wxUnbindSceneId = ref<string>('')
let wxUnbindCheckTimer: any = null

const fetchWxUnbindReqCode = async () => {
  wxUnbindReqCode.value = '获取中...'
  const res = await reqWxUnbindCodeUsingGet()
  if (res.data.code === 0 && res.data.data) {
    wxUnbindReqCode.value = res.data.data.code as string
    wxUnbindSceneId.value = res.data.data.sceneId as string
    startWxUnbindCheckTimer()
  } else {
    message.error('获取解绑验证码失败: ' + res.data.message)
    wxUnbindReqCode.value = '已过期'
  }
}

const startWxUnbindCheckTimer = () => {
  stopWxUnbindCheckTimer()
  wxUnbindCheckTimer = setInterval(async () => {
    if (!wxUnbindSceneId.value) return
    const res = await checkWxUnbindStatusUsingGet({ sceneId: wxUnbindSceneId.value })
    if (res.data.code === 0) {
      if (res.data.data) {
        stopWxUnbindCheckTimer()
        message.success('解绑成功，下次登录请使用您的邮箱或悦木号')
        unbindWxConfirmOpen.value = false
        const newUserInfo = { ...loginUserStore.loginUser, mpOpenId: null, hasBindWx: false }
        loginUserStore.setLoginUser(newUserInfo)
      }
    } else {
      stopWxUnbindCheckTimer()
      message.error(res.data.message || '验证码已过期或验证失败')
      wxUnbindReqCode.value = '已过期'
    }
  }, 2000)
}

const stopWxUnbindCheckTimer = () => {
  if (wxUnbindCheckTimer) {
    clearInterval(wxUnbindCheckTimer)
    wxUnbindCheckTimer = null
  }
}

const handleOpenUnbindWx = () => {
  unbindWxConfirmOpen.value = true
  fetchWxUnbindReqCode()
}

const closeWxUnbindModal = () => {
  unbindWxConfirmOpen.value = false
  stopWxUnbindCheckTimer()
}

// 页面卸载时清理定时器
onBeforeUnmount(() => {
  stopWxBindCheckTimer()
  stopWxUnbindCheckTimer()
})

const handleWxItemClick = () => {
  if (loginUserStore.loginUser.hasBindWx) {
    handleOpenUnbindWx()
  } else {
    handleOpenBindWx()
  }
}

// 安全注销相关状态
const secureDestroyOpen = ref(false)
const secureDestroyForm = reactive({
  userPassword: '',
  code: ''
})
const destroyCodeCountdown = ref(0)
let destroyCodeTimer: NodeJS.Timeout | null = null

// 用户权限设置相关状态
const userPermissionSettingOpen = ref(false)

// 加载用户权限设置
const loadUserPermissions = async () => {
  try {
    const response = await getUserPermissionsUsingGet({ userId: loginUserStore.loginUser.id })
    if (response.data.code === 0 && response.data) {
      console.log('用户权限数据:', response.data)
      // 将权限数据合并到用户信息中
      Object.assign(myMessage.value, {
        allowPrivateChat: response.data.data.allowPrivateChat,
        allowFollow: response.data.data.allowFollow,
        showFollowList: response.data.data.showFollowList,
        showFansList: response.data.data.showFansList
      })

      // 同时更新登录用户存储中的权限信息
      const updatedUserInfo = { ...loginUserStore.loginUser }
      updatedUserInfo.allowPrivateChat = response.data.data.allowPrivateChat
      updatedUserInfo.allowFollow = response.data.data.allowFollow
      updatedUserInfo.showFollowList = response.data.data.showFollowList
      updatedUserInfo.showFansList = response.data.data.showFansList

      loginUserStore.setLoginUser(updatedUserInfo)
    }
  } catch (error) {
    console.error('加载用户权限失败:', error)
  }
}

// 加载用户多设备登录设置
const loadUserMultiDeviceLoginSetting = async () => {
  try {
    const response = await getUserMultiDeviceLoginUsingGet({ userId: loginUserStore.loginUser.id })
    if (response.data.code === 0 && response.data) {
      console.log('用户多设备登录设置:', response.data)
      // 更新用户多设备登录设置
      myMessage.value.allowMultiDeviceLogin = response.data.data

      // 同时更新登录用户存储中的多设备登录设置
      const updatedUserInfo = { ...loginUserStore.loginUser }
      updatedUserInfo.allowMultiDeviceLogin = response.data.data

      loginUserStore.setLoginUser(updatedUserInfo)
    }
  } catch (error) {
    console.error('加载用户多设备登录设置失败:', error)
  }
}

// 重构：折叠面板状态管理
const activePanel = ref('account') // 默认展开账号安全面板
const togglePanel = (panelKey: string) => {
  if (activePanel.value === panelKey) {
    activePanel.value = '' // 再次点击关闭
  } else {
    activePanel.value = panelKey // 切换到对应面板
  }
}

// 修复：打开编辑资料弹窗的方法
const openModal = () => {
  open.value = true
}

// 用户信息
const myMessage = ref({
  userName: loginUserStore.loginUser.userName,
  id: loginUserStore.loginUser.id,
  userAccount: loginUserStore.loginUser.userAccount,
  userProfile: loginUserStore.loginUser.userProfile || '',
  userRole: loginUserStore.loginUser.userRole,
  userAvatar: loginUserStore.loginUser.userAvatar,
  email: loginUserStore.loginUser.email,
  gender: loginUserStore.loginUser.gender || '',
  region: loginUserStore.loginUser.region || '',
  birthday: loginUserStore.loginUser.birthday || '',
  userTags: loginUserStore.loginUser.userTags || '',
  homepageBg: loginUserStore.loginUser.homepageBg || '',
  personalSign: loginUserStore.loginUser.personalSign || '',
  allowPrivateChat: loginUserStore.loginUser.allowPrivateChat ?? 1,
  allowFollow: loginUserStore.loginUser.allowFollow ?? 1,
  showFollowList: loginUserStore.loginUser.showFollowList ?? 1,
  showFansList: loginUserStore.loginUser.showFansList ?? 1,
  allowMultiDeviceLogin: loginUserStore.loginUser.allowMultiDeviceLogin ?? 1,
})

// 更新用户权限信息
const updateUserInfo = (newUserInfo: any) => {
  myMessage.value = {
    userName: newUserInfo.userName,
    id: newUserInfo.id,
    userAccount: newUserInfo.userAccount,
    userProfile: newUserInfo.userProfile || '',
    userRole: newUserInfo.userRole,
    userAvatar: newUserInfo.userAvatar,
    email: newUserInfo.email,
    gender: newUserInfo.gender || '',
    region: newUserInfo.region || '',
    birthday: newUserInfo.birthday || '',
    userTags: newUserInfo.userTags || '',
    homepageBg: newUserInfo.homepageBg || '',
    personalSign: newUserInfo.personalSign || '',
    allowPrivateChat: newUserInfo.allowPrivateChat ?? 1,
    allowFollow: newUserInfo.allowFollow ?? 1,
    showFollowList: newUserInfo.showFollowList ?? 1,
    showFansList: newUserInfo.showFansList ?? 1,
    allowMultiDeviceLogin: newUserInfo.allowMultiDeviceLogin ?? 1,
  }
}

// 生日日期
const birthdayDate = computed({
  get: () => myMessage.value.birthday || null,
  set: (value: string | null) => {
    myMessage.value.birthday = value || ''
  }
})

// 关注粉丝数据
const followAndFans = ref({ followCount: 0, fansCount: 0 })

// 定位相关
const locationLoading = ref(false)

// 消息流推荐相关
const streamLayout = ref('waterfall') // 默认瀑布流
const gridAspectRatio = ref('4:3') // 默认等高比例
const layoutOptions = ref([
  { label: '瀑布流', value: 'waterfall' },
  { label: '多列等高', value: 'grid' },
  { label: '单列展示', value: 'single' }
])

// 等高比例选项
const aspectRatioOptions = ref([
  { label: '4:3', value: '4:3' },
  { label: '3:4', value: '3:4' },
  { label: '1:1', value: '1:1' }
])

// 提供流布局设置给子组件
provide('streamLayout', streamLayout)
provide('gridAspectRatio', gridAspectRatio)

const getFollowAndFansCount = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    const res = await getFollowAndFansCountUsingPost({ id: loginUserStore.loginUser.id })
    if (res.data.code === 0) {
      followAndFans.value = res.data.data || { followCount: 0, fansCount: 0 }
    }
  } catch (err) {
    console.error('获取关注粉丝数失败：', err)
  }
}

const editProfile = async () => {
  if (!myMessage.value.userName) {
    return message.warning('昵称不能为空')
  }

  // 验证字符数限制
  if (myMessage.value.userName && myMessage.value.userName.length > 30) {
    return message.warning('昵称不能超过15个汉字')
  }
  if (myMessage.value.userProfile && myMessage.value.userProfile.length > 520) {
    return message.warning('简介不能超过520个字符')
  }
  if (myMessage.value.region && myMessage.value.region.length > 50) {
    return message.warning('地区不能超过25个汉字')
  }

  try {
    const res = await updateUserUsingPost(myMessage.value)
    if (res.data.code === 0) {
      message.success('修改成功')
      // 更新登录用户存储中的信息，确保其他组件也能获取到最新信息
      const updatedUserInfo = { ...loginUserStore.loginUser }
      Object.assign(updatedUserInfo, myMessage.value)
      loginUserStore.setLoginUser(updatedUserInfo)
      open.value = false
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (err) {
    console.error('编辑资料失败：', err)
    message.error('修改失败，请重试')
  }
}

// 修改密码
const modifyPasswordFormData = ref<UserModifyPassWord>({
  oldPassword: '',
  newPassword: '',
  checkPassword: '',
})

const submitPasswordForm = async () => {
  // 表单校验
  if (!modifyPasswordFormData.value.oldPassword) return message.warning('请输入当前密码')
  if (!modifyPasswordFormData.value.newPassword) return message.warning('请输入新密码')
  if (modifyPasswordFormData.value.newPassword.length < 8) return message.warning('新密码至少8位')
  if (modifyPasswordFormData.value.newPassword !== modifyPasswordFormData.value.checkPassword) {
    return message.warning('两次密码输入不一致')
  }

  try {
    const res = await changePasswordUsingPost({
      id: loginUserStore.loginUser.id,
      oldPassword: modifyPasswordFormData.value.oldPassword,
      newPassword: modifyPasswordFormData.value.newPassword,
      checkPassword: modifyPasswordFormData.value.checkPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功')
      modifyPasswordOpen.value = false
      // 重置表单
      modifyPasswordFormData.value = { oldPassword: '', newPassword: '', checkPassword: '' }
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (err) {
    console.error('修改密码失败：', err)
    message.error('修改失败，请重试')
  }
}

// 修改邮箱
const changeEmailForm = reactive({ newEmail: '', code: '' })
const countdown = ref(0)
let timer: NodeJS.Timeout | null = null

const sendEmailCode = async () => {
  if (!changeEmailForm.newEmail) return message.warning('请输入新邮箱')
  // 简单邮箱校验
  const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
  if (!emailReg.test(changeEmailForm.newEmail)) return message.warning('请输入有效的邮箱地址')

  try {
    const res = await getEmailCodeUsingPost({ email: changeEmailForm.newEmail, type: 'changeEmail' })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      countdown.value = 60
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer!)
          timer = null
        }
      }, 1000)
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch (error: any) {
    console.error('发送验证码失败：', error)
    message.error(error.response?.data?.message || '发送失败')
  }
}

const handleChangeEmail = async () => {
  if (!changeEmailForm.newEmail) return message.warning('请输入新邮箱')
  if (!changeEmailForm.code) return message.warning('请输入验证码')

  try {
    const res = await changeEmailUsingPost({
      newEmail: changeEmailForm.newEmail,
      code: changeEmailForm.code,
      id: loginUserStore.loginUser.id
    })
    if (res.data.code === 0) {
      message.success('邮箱修改成功')
      changeEmailOpen.value = false
      // 更新本地邮箱
      myMessage.value.email = changeEmailForm.newEmail
      // 更新登录用户存储中的邮箱信息，确保其他组件也能获取到最新信息
      const updatedUserInfo = { ...loginUserStore.loginUser }
      updatedUserInfo.email = changeEmailForm.newEmail
      loginUserStore.setLoginUser(updatedUserInfo)
      // 重置表单
      changeEmailForm.newEmail = ''
      changeEmailForm.code = ''
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error: any) {
    console.error('修改邮箱失败：', error)
    message.error(error.response?.data?.message || '修改失败')
  }
}

// 头像上传
const fileInput = ref<HTMLInputElement | null>(null)
const bgFileInput = ref<HTMLInputElement | null>(null)
const avatarCropperRef = ref()
const tempImageUrl = ref('')

const showFileUploadDialog = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleAvatarUpload = (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    // 限制文件大小（2MB）
    if (file.size > 2 * 1024 * 1024) {
      return message.warning('头像文件大小不能超过2MB')
    }
    tempImageUrl.value = URL.createObjectURL(file)
    avatarCropperRef.value?.openModal()
    target.value = '' // 重置input
  }
}

const handleCroppedAvatar = async (file: File) => {
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('id', loginUserStore.loginUser.id.toString())

    const res = await updateUserAvatarUsingPost(
      { id: loginUserStore.loginUser.id },
      {},
      file,
      { 'Content-Type': 'multipart/form-data' }
    )
    if (res.data.code === 0) {
      message.success('头像上传成功')
      myMessage.value.userAvatar = res.data.data
      // 更新登录用户存储中的头像信息，确保其他组件也能获取到最新头像
      const updatedUserInfo = { ...loginUserStore.loginUser }
      updatedUserInfo.userAvatar = res.data.data
      loginUserStore.setLoginUser(updatedUserInfo)
      avatarCropperRef.value?.closeModal()
    }
  } catch (err) {
    console.error('上传头像失败：', err)
    message.error('头像上传失败')
  }
}

// 背景图上传
const showBgFileUploadDialog = () => {
  if (bgFileInput.value) {
    bgFileInput.value.click()
  }
}

const handleBgImageUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) {
    // 限制文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
      return message.warning('背景图文件大小不能超过5MB')
    }

    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('id', loginUserStore.loginUser.id.toString())

      // 显示上传中提示
      const hide = message.loading('背景图上传中...', 0)

      const res = await uploadPostImageUsingPost({}, {}, file)

      // 关闭上传中提示
      setTimeout(hide, 100)

      if (res.data.code === 0) {
        message.success('背景图上传成功')
        myMessage.value.homepageBg = res.data.data.url
      } else {
        message.error(res.data.message || '背景图上传失败')
      }
    } catch (err) {
      console.error('上传背景图失败：', err)
      message.error('背景图上传失败')
    }

    target.value = '' // 重置input
  }
}

// 签到日历 - 重构版
const selectedYear = ref(new Date().getFullYear())
const signInData = ref<number[]>([])

const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return Array.from({ length: 3 }, (_, i) => ({
    label: `${currentYear - i}年`,
    value: currentYear - i
  }))
})

const fetchSignInData = async () => {
  try {
    const res = await getUserSignInRecordUsingGet({ year: selectedYear.value })
    if (res.data.code === 0) {
      signInData.value = res.data.data || []
    }
  } catch (err) {
    console.error('获取签到记录失败：', err)
    signInData.value = []
  }
}

// 日历工具方法
// 计算某月有多少天
const getDaysInMonth = (month: number): number => {
  return new Date(selectedYear.value, month, 0).getDate()
}

// 计算某月1号是星期几（返回：0=周日，1=周一... 这里转为：周一=0，周日=6）
const getMonthStartWeek = (month: number): number => {
  const firstDay = new Date(selectedYear.value, month - 1, 1).getDay()
  return firstDay === 0 ? 6 : firstDay - 1
}

// 判断某天是否已签到
const isSigned = (month: number, day: number): boolean => {
  // 拼接日期格式
  const targetDate = `${selectedYear.value}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`
  return signInData.value.some(item => {
    const itemDate = new Date(selectedYear.value, 0, item).toISOString().split('T')[0]
    return itemDate === targetDate
  })
}

// 自动签到
const autoSignIn = async () => {
  if (!loginUserStore.loginUser.id) return
  try {
    await addUserSignInUsingPost()
    await fetchSignInData()
  } catch (err) {
    console.error('自动签到失败：', err)
  }
}

// 获取当前位置
const getLocation = async () => {
  if (locationLoading.value) return

  locationLoading.value = true

  try {
    const response = await fetch('https://api.myip.la/cn?json')
    const data = await response.json()

    if (data && data.location && data.location.province) {
      myMessage.value.region = data.location.province
      message.success(`定位成功: ${data.location.province}`)
    } else {
      myMessage.value.region = '北京'
      message.info('定位成功: 北京')
    }
  } catch (err) {
    console.error('定位失败:', err)
    myMessage.value.region = '北京'
    message.warning('定位失败，已使用默认位置')
  } finally {
    locationLoading.value = false
  }
}

// 退出登录
const handleLogoutConfirm = async () => {
  try {
    await userLogoutUsingPost()
    loginUserStore.setLoginUser({ userName: '未登录', id: 0 })
    message.success('退出成功')
    router.push('/user/login')
  } catch (err) {
    console.error('退出登录失败：', err)
    message.error('退出失败，请重试')
  }
}

// 注销账号
// 发送注销验证码
const sendDestroyCode = async () => {
  if (!secureDestroyForm.userPassword) return message.warning('请输入当前密码')

  // 先验证密码是否正确
  try {
    const pwdCheckRes = await changePasswordUsingPost({
      id: loginUserStore.loginUser.id,
      oldPassword: secureDestroyForm.userPassword,
      newPassword: secureDestroyForm.userPassword, // 使用相同密码作为新密码，只是验证旧密码
      checkPassword: secureDestroyForm.userPassword,
    })

    if (pwdCheckRes.data.code !== 0) {
      // 密码错误，不允许发送验证码
      message.error('当前密码错误，无法发送验证码')
      return
    }
  } catch (pwdError: any) {
    console.error('密码验证失败：', pwdError)
    message.error('当前密码错误，无法发送验证码')
    return
  }

  try {
    const res = await getDestroyCodeUsingPost()
    if (res.data.code === 0) {
      message.success('验证码已发送至您的邮箱')
      // 开始倒计时
      destroyCodeCountdown.value = 60
      destroyCodeTimer = setInterval(() => {
        destroyCodeCountdown.value--
        if (destroyCodeCountdown.value <= 0) {
          clearInterval(destroyCodeTimer!)
          destroyCodeTimer = null
        }
      }, 1000)
    } else {
      message.error(res.data.message || '发送失败')
    }
  } catch (error: any) {
    console.error('发送注销验证码失败：', error)
    message.error(error.response?.data?.message || '发送失败')
  }
}

// 确认安全注销
const confirmSecureDestroy = async () => {
  if (!secureDestroyForm.userPassword) return message.warning('请输入当前密码')
  if (!secureDestroyForm.code) return message.warning('请输入验证码')

  try {
    const res = await userDestroySecureUsingPost({
      userPassword: secureDestroyForm.userPassword,
      code: secureDestroyForm.code
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('账号已安全注销')
      loginUserStore.logout()
      router.push('/user/login')
      secureDestroyOpen.value = false
      // 重置表单
      secureDestroyForm.userPassword = ''
      secureDestroyForm.code = ''
    } else {
      message.error('注销失败：' + res.data.message)
    }
  } catch (err) {
    console.error('安全注销失败：', err)
    message.error('注销失败，请重试')
  }
}

const confirmLogout = async () => {
  try {
    const res = await userDestroyUsingPost({ id: loginUserStore.loginUser.id })
    if (res.data.code === 0) {
      message.success('注销成功')
      loginUserStore.logout()
      router.push('/user/login')
    } else {
      message.error('注销失败：' + res.data.message)
    }
    logoutConfirmOpen.value = false
  } catch (err) {
    console.error('注销账号失败：', err)
    message.error('注销失败，请重试')
    logoutConfirmOpen.value = false
  }
}

// 切换多设备登录设置
const toggleMultiDeviceLogin = async (checked: boolean) => {
  try {
    const response = await updateUserMultiDeviceLoginUsingPost({ userId: loginUserStore.loginUser.id, allowMultiDeviceLogin: checked ? 1 : 0 })
    if (response.data.code === 0) {
      message.success(checked ? '已允许多设备登录' : '已禁止多设备登录')
      // 更新本地用户信息
      myMessage.value.allowMultiDeviceLogin = checked ? 1 : 0

      // 同时更新登录用户存储中的多设备登录设置
      const updatedUserInfo = { ...loginUserStore.loginUser }
      updatedUserInfo.allowMultiDeviceLogin = checked ? 1 : 0

      loginUserStore.setLoginUser(updatedUserInfo)
    } else {
      message.error('设置失败：' + response.data.message)
      // 恢复原来的值
      myMessage.value.allowMultiDeviceLogin = !checked ? 1 : 0
    }
  } catch (error) {
    console.error('更新多设备登录设置失败：', error)
    message.error('设置失败，请重试')
    // 恢复原来的值
    myMessage.value.allowMultiDeviceLogin = !checked ? 1 : 0
  }
}

// 辅助方法
const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}`
}

const handleFollowClick = () => {
  router.push({ path: '/follow-list', query: { tab: 'follow' } })
}

const handleFansClick = () => {
  router.push({ path: '/follow-list', query: { tab: 'fans' } })
}

// 修复：恋爱画板点击无反应 - 增加路由跳转校验 + 错误提示
const handleLoveBoard = () => {
  router.push('/loveboard')
}

const handleForgotPassword = () => {
  modifyPasswordOpen.value = false
  router.push('/user/reset-password')
}

const handleAuClick = () => {
  window.open('http://yuemustory.fun/', '_blank')
}

const openOfficialDocs = () => {
  window.open('https://official.yuemutuku.com', '_blank')
}

const handlePrivacySettingClick = async () => {
  console.log('点击了隐私设置按钮');
  // 确保加载最新的权限设置
  await loadUserPermissions()
  userPermissionSettingOpen.value = true
}

const handleReportCenter = () => {
  router.push('/user/report-center')
}

const handleBrowseHistory = () => {
  router.push('/browse-history')
}

// 根据环境获取备案号
const getBeianNumber = () => {
  // 从环境变量获取环境类型
  const env = import.meta.env.VITE_APP_ENV || 'development'

  if (env === 'production') {
    return '陇ICP备2024012699号-3'
  } else {
    // 开发和测试环境都使用-1号
    return '陇ICP备2024012699号-1'
  }
}

// 显示消息流推荐
const showStreamRecommendations = () => {
  streamRecommendationsOpen.value = true
  // 根据设备类型过滤可选项
  filterLayoutOptions()
  // 从localStorage加载设置
  loadStreamLayout()
}

// 保存流布局设置
const saveStreamLayout = () => {
  localStorage.setItem('streamLayout', streamLayout.value)
  // 保存等高比例设置
  if (streamLayout.value === 'grid') {
    localStorage.setItem('gridAspectRatio', gridAspectRatio.value)
  }
  // 触发事件通知其他组件设置已更改
  window.dispatchEvent(new CustomEvent('streamLayoutChanged', {
    detail: {
      layout: streamLayout.value,
      aspectRatio: streamLayout.value === 'grid' ? gridAspectRatio.value : undefined
    }
  }))
}

// 选择布局
const selectLayout = (layout: string) => {
  streamLayout.value = layout
  saveStreamLayout()
}

// 选择等高比例
const selectAspectRatio = (ratio: string) => {
  gridAspectRatio.value = ratio
  saveStreamLayout()
}

// 加载流布局设置
const loadStreamLayout = () => {
  const savedLayout = localStorage.getItem('streamLayout')
  if (savedLayout) {
    streamLayout.value = savedLayout
  }
  // 加载等高比例设置
  const savedAspectRatio = localStorage.getItem('gridAspectRatio')
  if (savedAspectRatio) {
    gridAspectRatio.value = savedAspectRatio
  }
}

// 根据设备类型过滤布局选项
const filterLayoutOptions = async () => {
  const deviceType = await getDeviceType()
  if (deviceType === DEVICE_TYPE_ENUM.PC) {
    // PC端显示瀑布流和多列等高
    layoutOptions.value = [
      { label: '瀑布流', value: 'waterfall' },
      { label: '多列等高', value: 'grid' }
    ]

    // 如果当前设置是单列展示，则重置为瀑布流
    if (streamLayout.value === 'single') {
      streamLayout.value = 'waterfall'
    }
  } else {
    // 移动端只显示瀑布流和单列展示，不显示多列等高
    layoutOptions.value = [
      { label: '瀑布流', value: 'waterfall' },
      { label: '单列展示', value: 'single' }
    ]

    // 如果当前设置是多列等高，则重置为瀑布流
    if (streamLayout.value === 'grid') {
      streamLayout.value = 'waterfall'
    }
  }
}

const currentYear = computed(() => getCurrentYear())

// 监听登录状态变化
watch(() => loginUserStore.loginUser, (newVal) => {
  updateUserInfo(newVal);
}, { deep: true })

// 生命周期
onMounted(async () => {
  await getFollowAndFansCount()
  if (loginUserStore.loginUser.id) {
    await autoSignIn()
    // 获取用户权限设置
    await loadUserPermissions()
    // 获取用户多设备登录设置
    await loadUserMultiDeviceLoginSetting()
  }
  // 加载流布局设置
  loadStreamLayout()
  // 初始化echarts
  VChart.config = { echarts }
  await filterLayoutOptions()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
#SettingView {
  margin: 4px;
  background: var(--post-background);
  min-height: 92vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 主容器 - 优化间距 */
.setting-container {
  max-width: 750px;
  background: var(--post-background);
  margin: 0 auto;
  padding-bottom: 80px; /* 为底部可能存在的导航栏预留空间 */
}

/* 用户卡片 - 美化样式 */
.user-card {
  background-color: var(--card-background);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  cursor: pointer;

  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
}

.user-card:hover {
  box-shadow: 0 4px 12px var(--shadow-color);
  background-color: var(--hover-background);
}

.avatar-wrapper {
  position: relative;
  margin-right: 20px;
}

.edit-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background-color: var(--link-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--card-background);
  cursor: pointer;
  transition: var(--theme-transition);
}

.edit-icon {
  color: var(--text-other);
  font-size: 14px;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.user-id {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.user-stats {
  font-size: 14px;
  color: var(--text-secondary);
}

.stat-item {
  cursor: pointer;
  transition: color 0.2s;
}

.stat-item:hover {
  color: var(--link-color);
}

.stat-item strong {
  font-weight: 600;
  color: var(--text-primary);
}

.divider {
  margin: 0 12px;
  color: var(--border-color);
}

.arrow-icon {
  font-size: 16px;
  color: var(--text-secondary);
  margin-left: auto;
  transition: var(--theme-transition);
}

/* 重构：折叠面板样式 */
.function-accordion {
  background-color: var(--card-background);
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.accordion-panel {
  border-bottom: 1px solid var(--border-color);
}

.accordion-panel:last-child {
  border-bottom: none;
}

.accordion-header {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  cursor: pointer;
  transition: background-color 0.2s;
  justify-content: space-between;
}

.accordion-header:hover {
  background-color: var(--hover-background);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.panel-icon {
  font-size: 20px;
  color: var(--link-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
}

.panel-arrow {
  font-size: 16px;
  color: var(--text-secondary);
  transition: transform 0.2s ease;
}

.accordion-content {
  background-color: var(--hover-background);
  border-top: 1px solid var(--border-color);
}

/* 功能列表项样式（复用原有样式，调整间距） */
.list-item {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color);
  transition: background-color 0.2s, var(--theme-transition);
}

.list-item:last-child {
  border-bottom: none;
}

.list-item:hover {
  background-color: var(--card-background);
}

.item-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  background-color: var(--hover-background);
  transition: var(--theme-transition);
}

/* 不同功能图标配色 */
.love-icon {
  background-color: var(--markdown-heading-pink-bg);
}
.love-icon .item-icon {
  color: var(--markdown-heading-pink-border);
}

.about-icon {
  background-color: var(--markdown-heading-green-bg);
}
.about-icon .item-icon {
  color: var(--markdown-heading-green-border);
}

.system-icon {
  background-color: var(--markdown-heading-blue-bg);
}
.system-icon .item-icon {
  color: var(--markdown-heading-blue-border);
}

.logout-icon {
  background-color: var(--hover-background);
}
.logout-icon .item-icon {
  color: var(--text-secondary);
}

.destroy-icon {
  background-color: rgba(255, 77, 79, 0.1);
}
.destroy-icon .item-icon {
  color: var(--comment-delete-hover-color);
}

.sign-icon {
  background-color: var(--markdown-heading-yellow-bg);
}
.sign-icon .item-icon {
  color: var(--markdown-heading-yellow-border);
}

.privacy-icon {
  background-color: var(--markdown-heading-purple-bg);
}

.privacy-icon .item-icon {
  color: var(--markdown-heading-purple-border);
}

.item-icon {
  font-size: 20px;
  color: var(--link-color);
  transition: var(--theme-transition);
}

.item-content {
  flex: 1;
}

.item-text {
  font-size: 16px;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 2px;
}

.item-desc {
  font-size: 12px;
  color: var(--text-secondary);
}

.danger-text {
  color: var(--comment-delete-hover-color);
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: var(--comment-drawer-backdrop);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--theme-transition);
}

.modal-content {
  background-color: var(--card-background);
  border-radius: 16px;
  width: 100%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  z-index: 1001;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

/* 日历弹窗 - 重构样式 */
.calendar-content {
  max-width: 600px;
}

.calendar-body {
  padding: 20px;
}

.calendar-wrapper {
  width: 100%;
  overflow-y: auto;
  max-height: 500px;
}

/* 月份网格：每行显示4个月（PC）/2个月（移动端） */
.month-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.month-item {
  background-color: var(--card-background);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}

.month-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  text-align: center;
}

/* 星期头部 */
.week-header {
  display: flex;
  margin-bottom: 6px;
}

.week-day {
  flex: 1;
  font-size: 10px;
  color: var(--text-secondary);
  text-align: center;
}

/* 日期网格 */
.date-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.date-item {
  width: 100%;
  height: 20px;
  line-height: 20px;
  font-size: 11px;
  text-align: center;
  border-radius: 4px;
  color: var(--text-secondary);
  transition: all 0.2s ease, var(--theme-transition);
}

/* 已签到样式 */
.date-item.signed {
  background-color: var(--markdown-heading-pink-bg);
  color: var(--markdown-heading-pink-border);
  font-weight: 500;
}

/* 空白占位 */
.date-item.empty {
  visibility: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  background-color: var(--card-background);
  z-index: 1;
  transition: var(--theme-transition);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.close-btn {
  font-size: 20px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s, var(--theme-transition);
}

.close-btn:hover {
  color: var(--text-primary);
}

.modal-body {
  padding: 20px;
}

.permission-setting-body {
  padding: 0;
}

.permission-setting-modal {
  max-width: 500px;
}

/* 设置项样式 */
.setting-section {
  margin-bottom: 24px;
}

.setting-section .section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.setting-item {
  margin-bottom: 16px;
}

.setting-label {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 8px;
  font-weight: 500;
}

.layout-preview-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.layout-option {
  border: 2px solid var(--border-color);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: var(--hover-background);
}

.layout-option:hover {
  border-color: var(--link-color);
  transform: translateY(-2px);
}

.layout-option.active {
  border-color: var(--link-color);
  background-color: rgba(59, 130, 246, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  text-align: center;
}

.preview-content {
  display: flex;
  gap: 8px;
  min-height: 80px;
  margin-bottom: 12px;
}

.waterfall-preview {
  column-count: 3;
  column-gap: 8px;
  break-inside: avoid;
}

.grid-preview {
  flex-wrap: wrap;
  align-items: flex-start;
}

.single-preview {
  flex-direction: column;
  align-items: stretch;
}

.preview-item {
  background: linear-gradient(135deg, #667eea 0%, #3b82f6 100%);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.waterfall-preview .preview-item {
  break-inside: avoid;
  margin-bottom: 8px;
  width: 100%;
  display: block;
}

.waterfall-preview .preview-item.tall {
  height: 80px;
}

.waterfall-preview .preview-item.medium {
  height: 60px;
}

.waterfall-preview .preview-item.short {
  height: 40px;
}

.grid-preview .preview-item {
  flex: 1 1 calc(33.333% - 8px);
  height: 50px;
  min-width: 0;
  margin-bottom: 8px;
}

.grid-preview .preview-item.uniform {
  height: 50px;
}

.single-preview .preview-item {
  height: 40px;
  width: 100%;
  margin-bottom: 6px;
}

.single-preview .preview-item.wide {
  height: 60px;
}

.single-preview .preview-item:last-child {
  margin-bottom: 0;
}

.layout-description {
  text-align: center;
  font-size: 12px;
  color: var(--text-secondary);
  font-style: italic;
}

/* 等高比例选择器 */
.aspect-ratio-selector {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}

.ratio-options {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.ratio-option {
  padding: 6px 12px;
  border-radius: 20px;
  background-color: var(--hover-background);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
}

.ratio-option:hover {
  background-color: var(--link-color);
  color: var(--text-other);
  border-color: var(--link-color);
}

.ratio-option.active {
  background-color: var(--link-color);
  color: var(--text-other);
  border-color: var(--link-color);
  font-weight: 500;
}

/* ================= X-Style 个人资料编辑弹窗样式 ================= */
.glass-mask {
  backdrop-filter: blur(10px);
  background-color: rgba(0, 0, 0, 0.4);
}

.profile-edit-modal {
  max-width: 440px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.compact-header {
  padding: 16px 20px;
  border-bottom: none;
  background: transparent;
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 10;
}
.compact-header .modal-title {
  color: #fff;
  text-shadow: 0 1px 3px rgba(0,0,0,0.6);
  font-weight: 700;
}
.compact-header .close-btn {
  color: #fff;
  background: rgba(0,0,0,0.3);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.compact-header .close-btn:hover {
  background: rgba(0,0,0,0.5);
  transform: scale(1.05);
}

/* 沉浸式顶部 (Cover & Avatar) */
.profile-hero-section {
  position: relative;
  width: 100%;
  height: 160px; /* 适中高度 */
  background-color: var(--hover-background);
  margin-bottom: 48px; /* 增加留白 */
}

.hero-cover {
  width: 100%;
  height: 100%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);
}

.hero-cover:hover .cover-image {
  transform: scale(1.02);
}

.cover-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 24px;
}
.hero-cover:hover .cover-overlay {
  opacity: 1;
}

.hero-avatar-wrapper {
  position: absolute;
  bottom: -40px; /* 增加偏移量，更显悬浮感 */
  left: 24px;
  z-index: 5;
}

.avatar-upload-circle {
  width: 80px; /* 从 88px 减小 */
  height: 80px;
  border-radius: 50%;
  border: 4px solid var(--card-background);
  background-color: var(--card-background);
  position: relative;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.hero-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-upload-circle:hover .avatar-overlay {
  opacity: 1;
}

/* 紧凑表单区域 */
.scrollable-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px ; /* 增加左右内边距，更高级 */
}
/* 隐藏滚动条 */
.scrollable-body::-webkit-scrollbar {
  width: 6px;
}
.scrollable-body::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 10px;
}

.form-compact .form-item {
  margin-bottom: 24px; /* 增加 Item 间距，减少压迫感 */
}

.form-compact .form-label {
  font-size: 14px; /* 稍微放大一点点 */
  font-weight: 500; /* 减小字重，更清爽 */
  color: var(--text-secondary);
  margin-bottom: 8px; /* 增加标签间距 */
  padding-left: 4px;
}

.soft-input {
  background-color: var(--card-background);
  border: 1px solid var(--border-color) !important;
  border-radius: 10px;
  height: 48px;
  padding: 0 16px;
  color: var(--text-primary);
  transition: border-color 0.2s, box-shadow 0.2s;
  width: 100%;
  box-shadow: none !important;
}

.soft-input:focus, .soft-input:focus-within {
  background-color: var(--card-background);
  border-color: var(--link-color) !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1) !important;
}

.textarea-input {
  height: auto;
  padding: 12px 16px;
  resize: none;
  line-height: 1.5;
}

.date-picker-custom {
  padding: 0 16px !important;
  display: flex;
  align-items: center;
  box-shadow: none !important;
}
.date-picker-custom :deep(.ant-picker-input > input) {
  color: var(--text-primary);
  font-size: 15px;
}

/* 一行两列布局 */
.row-group {
  display: flex;
  gap: 16px;
}
.flex-1 {
  flex: 1;
  min-width: 0;
}
.w-full {
  width: 100%;
}

/* 定位按钮结合体内嵌 */
.location-group {
  position: relative;
  display: flex;
  align-items: center;
}
.inset-btn {
  position: absolute;
  right: 6px;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: var(--card-background);
  color: var(--link-color);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  transition: all 0.2s;
}
.inset-btn:hover {
  background: var(--link-color);
  color: #fff;
}
.inset-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 性别胶囊分段选择器 */
.gender-segmented-control {
  display: flex;
  background-color: var(--hover-background);
  border-radius: 10px;
  padding: 4px;
  gap: 4px;
  border: 1px solid var(--border-color);
}
.segment-pill {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.segment-pill.active {
  background-color: var(--card-background);
  color: var(--link-color);
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}
.segment-pill:hover:not(.active) {
  color: var(--text-primary);
}

/* 提交按钮 */
.modal-footer-actions {
  margin-top: 20px;
}
.pill-submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  background: var(--link-color);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: none;
}
.pill-submit-btn:hover {
  opacity: 0.9;
  background: #2563eb;
}

/* 表单样式 - 美化字数统计 */
.form-compact .char-count {
  text-align: right;
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
  height: 14px;
}

.form-input::placeholder {
  color: var(--comment-input-placeholder);
}

.verify-group {
  display: flex;
  gap: 12px;
}

.verify-input {
  flex: 1;
}

.send-code-btn {
  width: 120px;
  height: 50px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background-color: var(--hover-background);
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.2s ease, var(--theme-transition);
  font-size: 14px;
}

.send-code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: var(--border-color);
}

.send-code-btn:hover:not(:disabled) {
  background-color: var(--link-color);
  color: var(--text-other);
  border-color: var(--link-color);
}

/* 按钮样式 */
.submit-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  background-color: var(--link-color);
  color: var(--text-other);
  border: none;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease, var(--theme-transition);
  margin-top: 16px;
}

.submit-btn:hover {
  background-color: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}

.link-btn {
  color: var(--link-color);
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  transition: color 0.2s;
}

.link-btn:hover {
  color: #2563eb;
  text-decoration: underline;
}

/* --- 新增：通用通知卡片样式 --- */
.notice-card {
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
  background: var(--hover-background);
}

.notice-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  margin-bottom: 6px;
  font-size: 14px;
}

.notice-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.info-notice {
  background: rgba(59, 130, 246, 0.03);
  border-color: rgba(59, 130, 246, 0.1);
}
.info-notice .notice-header {
  color: var(--link-color);
}

.warning-notice {
  background: rgba(255, 77, 79, 0.03);
  border-color: rgba(255, 77, 79, 0.1);
}
.warning-notice .notice-header {
  color: #ff4d4f;
}

.highlight-text {
  color: var(--link-color);
  font-weight: 700;
  text-decoration: underline;
}

/* --- 新增：操作栏与按钮优化 --- */
.footer-link-wrapper {
  margin-top: 12px;
  text-align: center;
}

.vertical-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.danger-pill-btn {
  background: #ff4d4f !important;
}
.danger-pill-btn:hover {
  background: #f5222d !important;
}

.pill-cancel-btn {
  width: 100%;
  height: 48px;
  border-radius: 10px;
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.pill-cancel-btn:hover {
  background: var(--hover-background);
  color: var(--text-primary);
}

.pill-send-btn {
  border: none !important;
  background: var(--link-color) !important;
  color: #fff !important;
  font-weight: 600;
  width: 90px !important;
  height: 48px !important; /* 与 soft-input 高度一致 */
  border-radius: 12px !important;
}
.pill-send-btn:disabled {
  background: var(--border-color) !important;
  color: var(--text-tertiary) !important;
}

/* 头像上传样式 */
.avatar-upload {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  position: relative;
  cursor: pointer;
}

.edit-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--card-background);
  box-shadow: 0 2px 8px var(--shadow-color);
}

.camera-icon {
  font-size: 20px;
  color: var(--text-secondary);
}

/* 关于我们弹窗样式 */
.about-content {
  text-align: center;
}

.app-logo {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: var(--markdown-heading-pink-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.logo-icon {
  font-size: 40px;
  color: var(--markdown-heading-pink-border);
}

.version {
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.desc {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 20px 0;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item a {
  color: var(--link-color);
  text-decoration: none;
}

.info-item a:hover {
  text-decoration: underline;
}

.copyright {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 20px;
  margin-bottom: 16px;
}

.about-btn {
  padding: 8px 24px;
  border-radius: 20px;
  background-color: var(--hover-background);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 8px;
}

.about-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
}

.about-btn:hover {
  background-color: var(--link-color);
  color: var(--text-other);
  border-color: var(--link-color);
}

/* 确认弹窗样式 */
.confirm-content {
  background-color: var(--card-background);
  border-radius: 16px;
  width: 100%;
  max-width: 350px;
  padding: 24px;
  box-shadow: 0 10px 30px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.confirm-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
  text-align: center;
}

.confirm-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  text-align: center;
}

.confirm-btns {
  display: flex;
  gap: 12px;
}

.cancel-btn, .confirm-btn {
  flex: 1;
  height: 44px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.cancel-btn {
  background-color: var(--hover-background);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.cancel-btn:hover {
  background-color: var(--card-background);
  border-color: var(--link-color);
}

.confirm-btn {
  background-color: var(--link-color);
  color: var(--text-other);
  border: none;
}

.confirm-btn:hover {
  background-color: #2563eb;
}

/* 危险操作确认弹窗 */
.danger-confirm .confirm-title {
  color: var(--comment-delete-hover-color);
}

.danger-btn {
  background-color: var(--comment-delete-hover-color);
}

.danger-btn:hover {
  background-color: #dc2626;
}

/* 年份选择器 */
.year-selector {
  width: 100px;
  margin-right: 16px;
}

.bind-status {
  font-size: 13px;
  font-weight: 500;
  padding: 4px 12px;
  border-radius: 20px;
}

.bind-status.success {
  background-color: rgba(7, 193, 96, 0.1);
  color: #07c160;
}

.wx-bind-modal {
  max-width: 450px;
}

.wx-bind-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.qr-code-wrapper {
  width: 180px;
  height: 180px;
  padding: 10px;
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.qr-code {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.bind-steps {
  width: 100%;
  padding: 16px;
  background-color: var(--hover-background);
  border-radius: 12px;
}

.step {
  font-size: 14px;
  color: var(--text-primary);
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.step:last-child {
  margin-bottom: 0;
}

.highlight {
  color: var(--link-color);
  font-weight: 700;
  margin: 0 4px;
  background: rgba(59, 130, 246, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.verify-code-container {
  margin-bottom: 24px;
}

.verify-grid {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.verify-cell {
  width: 45px;
  height: 55px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  text-align: center;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  background-color: var(--hover-background);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.verify-cell:focus {
  border-color: var(--link-color);
  background-color: var(--card-background);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
  outline: none;
  transform: translateY(-2px);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .month-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .user-card {
    padding: 16px;
    margin-top: 12px;
  }

  .modal-content {
    max-width: 90%;
  }

  .calendar-content {
    max-width: 90%;
  }
}

@media (max-width: 480px) {
  .month-grid {
    grid-template-columns: 1fr;
  }

  .preview-content {
    min-height: 60px;
  }

  .waterfall-preview {
    column-count: 2;
  }

  .grid-preview .preview-item {
    flex: 1 1 calc(50% - 8px);
  }
}
</style>
