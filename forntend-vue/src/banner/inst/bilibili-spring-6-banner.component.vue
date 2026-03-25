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

const baseSrc: string = '/bilibili/spring-6/';
const marginLeft: number = 200;
const moveRate: number = 150;
const maxLeftPosition = { index: 12, cut: 25 };
const maxRightPosition = { index: 15, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '5598c26a3d090ba7cdf1d99de1e7d76561d56e78.png@1c.webp', width: 1670.4, height: 156.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newRotate: 0, newScale: 1, bench: 0 },
	{ type: 'image', file: '556c868c5b003f3f167cf1e63e6e3aa27b74bdc7.png@1c.webp', width: 1670.4, height: 156.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -1.57786, newY: 0, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: 'b1b6ebe5d3def015132105fe7a8253eced9b563c.png@1c.webp', width: 1920, height: 180, x: 0, y: 5, r: 0, s: 1, o: 1, newX: -12.0909, newY: -1.04545, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '59a63bc51e049fd01d7e567b9759727888d0de43.png@1c.webp', width: 1670.4, height: 156.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -2.62977, newY: 0, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '2e5a5407987125451f4401e1f80da8aa6c9ec74e.png@1c.webp', width: 1670.4, height: 156.6, x: -234.9, y: -17.4, r: 0, s: 1, o: 1, newX: -245.419, newY: -17.4, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '96151dfbace0750a4a590e9fac92211ce7c28b82.png@1c.webp', width: 1824, height: 171, x: -47.5, y: 0, r: 0, s: 1, o: 1, newX: -56.1148, newY: 2.87159, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: 'f272c9c3125afec158fe4c4e0540a271cd75dfb4.png@1c.webp', width: 75, height: 60, x: -250, y: -20, r: 0, s: 1, o: 1, newX: -268.136, newY: -20, newRotate: -6.04545, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '598389b103a4c89592706b9a426c1dd5c9fbdb96.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -18.1364, newY: -3.02273, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '3dd93e33d7e9bb6b74b7e2c577cb4a61cd7876bf.png@1c.webp', width: 1670.4, height: 156.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -26.2977, newY: -5.25955, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '2ec5703e20135d578a4d31a9d40dd9deb23c326e.png@1c.webp', width: 1824, height: 171, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -11.4864, newY: -2.87159, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '48d675bee7f2b20af495ddcfa2051d1e42eeb526.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -24.1818, newY: -6.04545, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: 'e35035fb17abb07d80c1749988bc938c9a5919dc.png@1c.webp', width: 2112, height: 198, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -46.55, newY: -9.975, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '5cbcbeb21c079264f78e63c411f09334126a11d3.png@1c.webp', width: 2112, height: 198, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -53.2, newY: -9.975, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: 'c9ef0b13e5ec27cc6e9fa1516e79a8ab8f5c5b38.png@1c.webp', width: 2112, height: 198, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -59.85, newY: -9.975, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '5a97b3d3f84e4ce0e77988a8a0d5744a2a2fd8b9.png@1c.webp', width: 1670.4, height: 156.6, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: '679c3bb077504de3379100c688770d94d97778cf.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -42.3182, newY: -4.83636, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'image', file: 'add3317b33f371b4ce4391b5169dbd6455065ed4.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -36.2727, newY: -6.04545, newRotate: 0, newScale: 1, bench: -1.57786 },
	{ type: 'video', file: 'b94241f2-320a-47ac-9f14-e9b0c46385ce.webm', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 0.5, newX: -36.2727, newY: 0, newRotate: 0, newScale: 1, bench: -1.57786, filter: 'blur(1px)' },
];
</script>
