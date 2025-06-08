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

const baseSrc: string = '/bilibili/winter-4/';
const marginLeft: number = 35;
const moveRate: number = 300;
const maxLeftPosition = { index: 13, cut: 0 };
const maxRightPosition = { index: 8, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '01b79b5b5b38d7305a0ca1780c24662cc5a59f18.png@1c.webp', width: 1920, height: 162, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 4.11352, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: 'fe903820ca0f604ef24f16ff7ad51192ead3d0c8.png@1c.webp', width: 1728, height: 162, x: 0, y: -9, r: 0, s: 1, o: 1, newX: 16.4541, newY: -9, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '73fcb8a56776307147c140f68bf9c5793696ae88.png@1c.webp', width: 1728, height: 147.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 32.9081, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: 'b8a5bcf77a07793ce50294a8cb14a9fb8f2cea83.png@1c.webp', width: 826, height: 76, x: 0, y: -40, r: 0, s: 1, o: 1, newX: 27.4235, newY: -40, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '1fd3d44b8f890b88cf52f6bcc041326b30e7cb62.png@1c.webp', width: 1523, height: 180, x: 100, y: 0, r: 0, s: 1, o: 1, newX: 154.847, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '085bc5e7f70b625b62c1f5d374f55acbd828ab65.png@1c.webp', width: 381.6, height: 66.6, x: -126, y: -45, r: 0, s: 1, o: 1, newX: -84.8648, newY: -45, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '3e16264878dd88c1b8ead37aab07a9363186b18c.png@1c.webp', width: 1920, height: 156, x: 0, y: 10, r: 0, s: 1, o: 1, newX: 63.9881, newY: 10, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '8902051ab71c8093a85ce2a71882054ee91ea631.png@1c.webp', width: 124.8, height: 130.8, x: 276, y: -48, r: 0, s: 1, o: 1, newX: 363.755, newY: -48, newRotate: -36.5646, bench: 4.11352 },
	{ type: 'image', file: 'c9231e763120806f3e918d06ad3f415e920c2b73.png@1c.webp', width: 589, height: 180, x: 660, y: 0, r: 0, s: 1, o: 1, newX: 733.129, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '67a3c27832d63060d88fbd7bdb2fc543e3fb9ddf.png@1c.webp', width: 420, height: 157, x: 750, y: 0, r: 0, s: 1, o: 1, newX: 841.412, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: 'c651a0183b81857191c86b133c5a777059b13571.png@1c.webp', width: 369.55, height: 141.55, x: 503.5, y: 14.25, r: 0, s: 1, o: 1, newX: 607.709, newY: 14.25, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '6afc90f36610318b4785a52c1a51ef6f1de9d25b.png@1c.webp', width: 171.9, height: 133.2, x: 504, y: 13.5, r: 0, s: 1, o: 1, newX: 726.13, newY: 13.5, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '476679726643b3b6cc5599b882d85953ef34f3d9.png@1c.webp', width: 203.4, height: 149.4, x: -171, y: -4.5, r: 0, s: 1, o: 1, newX: -88.7296, newY: -4.5, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '121335f79c78bb6f47e4fc7dae4b0af0eac804bb.png@1c.webp', width: 731.7, height: 162, x: -540, y: 0, r: 0, s: 1, o: 1, newX: -391.913, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '590a6dd64ff00d5d0e63e230188939c405253452.png@1c.webp', width: 291.65, height: 112.1, x: -332.5, y: 23.75, r: 0, s: 1, o: 1, newX: -202.239, newY: 23.75, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: 'e00c60ae700774655af2e2516a13a5b1fa2fcfe7.png@1c.webp', width: 190.95, height: 115.9, x: -551, y: 23.75, r: 0, s: 1, o: 1, newX: -333.029, newY: 23.75, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: '357dd346995f678c43eb6bf51fe02f47bbe7db7d.png@1c.webp', width: 379.2, height: 144, x: -880, y: 16, r: 0, s: 1, o: 1, newX: -616.735, newY: 16, newRotate: 0, bench: 4.11352 },
	{ type: 'image', file: 'bae427c01d895b63e6aaf6b0c239b86027527e6a.png@1c.webp', width: 288, height: 145, x: 20, y: 10, r: 0, s: 1, o: 0.3, newX: 93.1292, newY: 10, newRotate: 0, bench: 4.11352 },
	{ type: 'video', file: 'dd79de12-0736-4d00-acba-c6b201bc4886.webm', width: 2112, height: 154, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 30.1658, newY: 0, newRotate: 0, bench: 4.11352 },
	{ type: 'video', file: '758c63f5-cd91-43d2-abdd-4a3d6ccd4562.webm', width: 420, height: 630, x: 42, y: -84, r: 0, s: 1, o: 0.3, newX: 111.107, newY: -84, newRotate: 0, bench: 4.11352, filter: 'blur(5px)' },
];
</script>
