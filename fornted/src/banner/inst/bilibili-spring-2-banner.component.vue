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

const baseSrc: string = '/bilibili/spring-2/';
const marginLeft: number = -20;
const moveRate: number = -2;
const maxLeftPosition = { index: 0, cut: 0 };
const maxRightPosition = { index: 0, cut: 0 };

function getFileType(type: string): string {
	switch (type) {
		case 'image':
			return 'gif';
		case 'video':
			return 'mp4';
		default:
			return 'gif';
	}
}

let type: 'image' | 'video' = 'video';

const imgData: Array<OriDataType> = [
	{ type: type, file: `kPbkWcX74M.${getFileType(type)}`, width: 960 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
