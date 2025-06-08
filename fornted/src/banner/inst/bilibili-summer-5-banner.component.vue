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

const baseSrc: string = '/bilibili/summer-5/';
const marginLeft: number = 0;
const moveRate: number = 25;
const maxLeftPosition = { index: 14, cut: 0 };
const maxRightPosition = { index: 14, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '4e7edb416c34eccc40f34d2b297524652685a9bb.png@1c.webp', width: 2172, height: 203, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newRotate: 0, newScale: 1, newOpacity: 1, bench: 0 },
	{ type: 'image', file: '78e8b2abe1bc5715719e847a4e3654e3d11bcbf7.png@1c.webp', width: 783, height: 64, x: 108.581, y: -21.7161, r: 0, s: 1, o: 1, newX: 121.8, newY: -21.7161, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: 'a679239d0399412dd75e4e0eb30702b0818ef26d.png@1c.webp', width: 1542, height: 203, x: 253.355, y: 10.8581, r: 0, s: 1, o: 1, newX: 266.574, newY: 10.8581, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '9f86739c3b0ff5ddc99122aa506dee041aab4ec4.png@1c.webp', width: 2019, height: 203, x: -115.819, y: 7.23871, r: 0, s: 1, o: 1, newX: -82.7715, newY: 7.23871, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: 'ce1377aa701c7704b5f0984959512120ac57c586.png@1c.webp', width: 1033, height: 160, x: -144.774, y: 28.9548, r: 0, s: 1, o: 1, newX: -78.6784, newY: 28.9548, newRotate: 0, newScale: 1, newOpacity: 0.00233275, bench: 13.219 },
	{ type: 'image', file: '149e5beb3ef989ee01a28c0412ece9842c026e74.png@1c.webp', width: 1033, height: 160, x: -144.774, y: 28.9548, r: 0, s: 1, o: 0, newX: -78.6784, newY: 28.9548, newRotate: 0, newScale: 1, newOpacity: 0.998176, bench: 13.219 },
	{ type: 'image', file: '8198227e8abb7c75a94ddbc1209a9bf5ba067a68.png@1c.webp', width: 95, height: 51, x: -633.387, y: 42.2258, r: 0, s: 1, o: 1, newX: -594.831, newY: 42.2258, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: 'af9d07a3e29858d742cf2f0dd22c70d5553641f3.png@1c.webp', width: 228, height: 117, x: -687.677, y: 50.671, r: 0, s: 1, o: 1, newX: -489.39, newY: 50.671, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '0725acc8fac5c8164726240de477fbd0ce18e6dd.png@1c.webp', width: 107, height: 38, x: 431.306, y: 86.2613, r: 0, s: 1, o: 1, newX: 552.482, newY: 86.2613, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '7b96b0044345b5c2e3ddb8ea487daed8761c57f7.png@1c.webp', width: 77, height: 46, x: 72.3871, y: 0, r: 0, s: 1, o: 1, newX: 210.087, newY: -11.016, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	// { type: 'image', file: '24c57a58af481117640928798419341bc6fd32d3.png@1c.webp', width: 345, height: 131, x: -144.774, y: 14.4774, r: 0, s: 1, o: 1, newX: -85.288, newY: 14.4774, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: 'a89d95306fab17a35b50a5f7451e2b27ba4d2a6e.png@1c.webp', width: 345, height: 131, x: -144.774, y: 14.4774, r: 0, s: 1, o: 1, newX: -85.288, newY: 14.4774, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '1a77fc745eb5b65b976506bc3a8ce1c8fe876991.png@1c.webp', width: 293, height: 72, x: -219.574, y: 47.0516, r: 0, s: 1, o: 1, newX: -155.131, newY: 47.0516, newRotate: 4.56544, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: 'dc29b8093f5d97a41d48ca24b4d7d00f78920d54.png@1c.webp', width: 2244, height: 251, x: -27.1452, y: -4.52419, r: 0, s: 1, o: 1, newX: 80.2604, newY: -2.8718, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '9af5dad13028d419618e86399eb8fc009700d74c.png@1c.webp', width: 222, height: 100, x: -304.026, y: -57.9097, r: 0, s: 1, o: 1, newX: -39.6428, newY: -51.3001, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219 },
	{ type: 'image', file: '339abbc0d089a1ddbe3265b238e750173ce56de4.png@1c.webp', width: 2534, height: 237, x: 0, y: -16.8903, r: 0, s: 1, o: 1, newX: 231.335, newY: -16.8903, newRotate: 0, newScale: 1, newOpacity: 1, bench: 13.219, filter: 'blur(3px)' },
];
</script>
