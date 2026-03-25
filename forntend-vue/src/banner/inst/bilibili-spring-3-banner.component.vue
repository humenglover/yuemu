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

const baseSrc: string = '/bilibili/spring-3/';
const moveRate: number = -2;
const imgData: Array<OriDataType> = [
	{ type: 'image', file: 'c3c807eb2f5bcc333a3bf3189634e3f9867c6a85.webp', width: 1920 * 0.9, height: 180 * 0.9, x: 0, newX: 1, bench: 1 },
];
</script>
