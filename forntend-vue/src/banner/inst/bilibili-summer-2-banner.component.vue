<template>
	<BilibiliBannerBaseComponent
		:baseSrc="baseSrc"
		:marginLeft="marginLeft" :moveRate="moveRate"
		:maxLeftPosition="maxLeftPosition" :maxRightPosition="maxRightPosition"
		:imgData="imgData">
	</BilibiliBannerBaseComponent>
</template>
<script setup lang="ts">
import BilibiliBannerBaseComponent from '@/banner/base/bilibili-banner-base.component.vue';
import type { OriDataType } from '../base/bilibili-banner-base.type';

const baseSrc: string = '/bilibili/summer-2/';
const marginLeft: number = 0;
const moveRate: number = -2;
const maxLeftPosition = { index: 0, cut: 0 };
const maxRightPosition = { index: 0, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: 'background.png', width: 960 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
	{ type: 'image', file: 'cover.png', width: 960 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
