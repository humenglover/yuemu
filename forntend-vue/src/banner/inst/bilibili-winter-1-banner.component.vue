<template>
	<BilibiliBannerBaseComponent
		:baseSrc="baseSrc"
		:moveRate="moveRate"
		:imgData="imgData">
	</BilibiliBannerBaseComponent>
</template>
<script setup lang="ts">
import BilibiliBannerBaseComponent from '@/banner/base/bilibili-banner-base.component.vue';
import type { OriDataType } from '../base/bilibili-banner-base.type';

const baseSrc: string = '/bilibili/winter-1/';
const marginLeft: number = -40;
const moveRate: number = -2;

function getFileType(type: string): string {
	switch (type) {
		case 'image':
			return 'gif';
		case 'video':
			return 'webm';
		default:
			return 'gif';
	}
}

function getLightImg(): string {
	return '392f3829-0285-4589-8fba-ba5c3a236048';
}

function getTwilightImg(): string {
	return 'c1f4248b-fe5b-413e-890a-ebdbfbb3543f';
}

function getDarkImg(): string {
	return '93d05910-f94a-4246-aa23-60234dc90e31';
}

let type: 'image' | 'video' = 'video';

let img = '';
let hours = new Date().getHours();
if (hours >= 7 && hours < 15) {
	img = getLightImg();
} else if (hours >= 15 && hours < 20) {
	img = getTwilightImg();
} else {
	img = getDarkImg();
}

const imgData: Array<OriDataType> = [
	{ type: type, file: `${img}.${getFileType(type)}`, width: 1920 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
