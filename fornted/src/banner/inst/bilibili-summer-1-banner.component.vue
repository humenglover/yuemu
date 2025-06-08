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
import { CommonUtil } from "@/banner/common-util.ts";

const baseSrc: string = '/bilibili/summer-1/';
const marginLeft: number = -20;
const moveRate: number = -2;

function getLightImg(): string {
	let arr = [
		'1c713de1-3bbf-42b4-9148-0427b567b741',
		'9f75c700-2e13-45c7-b021-006928c80667',
		'11dd110b-5b4b-4912-af6d-1c4906295a1d',
	];
	return arr[CommonUtil.randomNum(0, 2)];
}

function getDarkImg(): string {
	let arr = [
		'b180aa43-9b33-476d-bca3-7db19583beff',
		'b180aa43-9b33-476d-bca3-7db19583beff',
		'4c0b2c71-0990-47a5-b19f-40e478ff6923',
		'6ef8db87-259b-48fb-b930-0fd201780d9b',
	];
	return arr[CommonUtil.randomNum(0, 3)];
}

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

let type: 'image' | 'video' = 'video';

let img = '';
let hours = new Date().getHours();
if (hours >= 6 && hours < 20) {
	img = getLightImg();
} else {
	img = getDarkImg();
}
const imgData: Array<OriDataType> = [
	{ type: type, file: `${img}.${getFileType(type)}`, width: 1920 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
