<template>
	<BilibiliBannerBaseComponent
		:baseSrc="baseSrc"
		:marginLeft="marginLeft" :moveRate="moveRate"
		:imgData="imgData">
	</BilibiliBannerBaseComponent>
</template>
<script setup lang="ts">
import BilibiliBannerBaseComponent from '@/banner/base/bilibili-banner-base.component.vue';
import type { OriDataType } from '../base/bilibili-banner-base.type';

const baseSrc: string = '/bilibili/summer-3/';
const marginLeft: number = 112;
const moveRate: number = -2;
const imgData: Array<OriDataType> = [
	{ type: 'image', file: 'fba0c9bac0fad743e9befa9305f197a919561580.png@3840w_360h_1c_90q.webp', width: 1920 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
