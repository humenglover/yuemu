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

const baseSrc: string = '/bilibili/autumn-1/';
const moveRate: number = -2;
const imgData: Array<OriDataType> = [];

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

function getLightImgList(type: 'image' | 'video'): Array<{ type: 'image' | 'video', file: string }> {
	return [
		{ type: type, file: `62a1c4b6-da1f-437a-90f4-1427f058d2e9.${getFileType(type)}` },
		{ type: 'image', file: '839af78322450ea0eb6dbb1d19889d831862804f.png' },
	];
}

function getDarkImgList(type: 'image' | 'video'): Array<{ type: 'image' | 'video', file: string }> {
	return [
		{ type: type, file: `8bcf476e-6c97-472b-b1e8-d19dd7170edf.${getFileType(type)}` },
		{ type: 'image', file: '5f18bd9e0b4fff738eaf6ebdc3c5c1477b5ea592.png' },
	];
}

let type: 'image' | 'video' = 'video';

let imgList;
let hours = new Date().getHours();
if (hours >= 6 && hours < 18) {
	imgList = getLightImgList(type);
} else {
	imgList = getDarkImgList(type);
}
for (let i = 0; i < imgList.length; i++) {
	imgData.push({ type: imgList[i].type, file: imgList[i].file, width: 1850 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 })
}
</script>
