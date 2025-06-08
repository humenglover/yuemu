<template>
  <div class="banner-wrapper">
    <component :is="selectedBanner" class="seasonal-banner" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
// Import all banner components
import BilibiliSpring1Banner from './inst/bilibili-spring-1-banner.component.vue'
import BilibiliSpring2Banner from './inst/bilibili-spring-2-banner.component.vue'
import BilibiliSpring3Banner from './inst/bilibili-spring-3-banner.component.vue'
import BilibiliSpring4Banner from './inst/bilibili-spring-4-banner.component.vue'
import BilibiliSpring5Banner from './inst/bilibili-spring-5-banner.component.vue'
import BilibiliSpring6Banner from './inst/bilibili-spring-6-banner.component.vue'

import BilibiliSummer1Banner from './inst/bilibili-summer-1-banner.component.vue'
import BilibiliSummer3Banner from './inst/bilibili-summer-3-banner.component.vue'
import BilibiliSummer4Banner from './inst/bilibili-summer-4-banner.component.vue'
import BilibiliSummer5Banner from './inst/bilibili-summer-5-banner.component.vue'
import BilibiliSummer6Banner from './inst/bilibili-summer-6-banner.component.vue'
import BilibiliSummer7Banner from './inst/bilibili-summer-7-banner.component.vue'

import BilibiliAutumn1Banner from './inst/bilibili-autumn-1-banner.component.vue'
import BilibiliAutumn2Banner from './inst/bilibili-autumn-2-banner.component.vue'
import BilibiliAutumn3Banner from './inst/bilibili-autumn-3-banner.component.vue'
import BilibiliAutumn4Banner from './inst/bilibili-autumn-4-banner.component.vue'
import BilibiliAutumn5Banner from './inst/bilibili-autumn-5-banner.component.vue'

import BilibiliWinter1Banner from './inst/bilibili-winter-1-banner.component.vue'
import BilibiliWinter2Banner from './inst/bilibili-winter-2-banner.component.vue'
import BilibiliWinter3Banner from './inst/bilibili-winter-3-banner.component.vue'
import BilibiliWinter4Banner from './inst/bilibili-winter-4-banner.component.vue'
import BilibiliWinter5Banner from './inst/bilibili-winter-5-banner.component.vue'

// Banner selection logic
const banners = {
  spring: [
    BilibiliSpring1Banner,
    BilibiliSpring2Banner,
    BilibiliSpring3Banner,
    BilibiliSpring4Banner,
    BilibiliSpring5Banner,
    BilibiliSpring6Banner
  ],
  summer: [
    BilibiliSummer1Banner,
    BilibiliSummer3Banner,
    BilibiliSummer4Banner,
    BilibiliSummer5Banner,
    BilibiliSummer6Banner,
    BilibiliSummer7Banner
  ],
  autumn: [
    BilibiliAutumn1Banner,
    BilibiliAutumn2Banner,
    BilibiliAutumn3Banner,
    BilibiliAutumn4Banner,
    BilibiliAutumn5Banner
  ],
  winter: [
    BilibiliWinter1Banner,
    BilibiliWinter2Banner,
    BilibiliWinter3Banner,
    BilibiliWinter4Banner,
    BilibiliWinter5Banner
  ]
} as const

// Get current season
const getCurrentSeason = () => {
  const now = new Date()
  const month = now.getMonth() + 1
  const day = now.getDate()

  // Spring: March 1 - May 31
  if ((month === 3 && day >= 1) || month === 4 || (month === 5 && day <= 31)) {
    return 'spring'
  }
  // Summer: June 1 - August 31
  if ((month === 6 && day >= 1) || month === 7 || (month === 8 && day <= 31)) {
    return 'summer'
  }
  // Autumn: September 1 - November 30
  if ((month === 9 && day >= 1) || month === 10 || (month === 11 && day <= 30)) {
    return 'autumn'
  }
  // Winter: December 1 - February 28/29
  return 'winter'
}

// Randomly select a banner for current season
const getRandomBanner = () => {
  const season = getCurrentSeason()
  const seasonalBanners = banners[season as keyof typeof banners]
  const randomIndex = Math.floor(Math.random() * seasonalBanners.length)
  return seasonalBanners[randomIndex]
}

const selectedBanner = ref(getRandomBanner())

// Initialize banner on mount
onMounted(() => {
  selectedBanner.value = getRandomBanner()
})
</script>

<style scoped>
.banner-wrapper {
  width: 100%;
  height: 180px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.seasonal-banner {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Mobile optimization */
@media screen and (max-width: 768px) {
  .banner-wrapper {
    height: 140px;
  }
}
</style>
