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

const baseSrc: string = '/bilibili/autumn-4/';
const marginLeft: number = 0;
const moveRate: number = 5;
const maxLeftPosition = { index: 6, cut: 25 };
const maxRightPosition = { index: 6, cut: 25 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: 'b51d0d75b1b27b669e456aa36bdf3b46f658a531.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newRotate: 0, bench: 0 },
	{ type: 'image', file: '8f976964bf99b64dcf844b698642caa01449623b.png@1c.webp', width: 464, height: 48, x: 0, y: -20, r: 0, s: 1, o: 1, newX: 188.698, newY: -20, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'a1d3587a7230b68e78b724558697ae6a1739e1cc.png@1c.webp', width: 1410.4, height: 48.8, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 1.50959, newY: 0, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '454a3f4bd1f8bd23d89d8e3a676a826e144ab13a.png@1c.webp', width: 1920, height: 80, x: 0, y: -5, r: 0, s: 1, o: 1, newX: 9.43491, newY: -5, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'a89c6c61109d1f58b83bc76fa3cf939df82d2ff4.png@1c.webp', width: 56.4, height: 33.6, x: 360, y: -36, r: 0, s: 1, o: 1, newX: 1492.19, newY: -58.6438, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '1c4b40f96bac2479f7a67541171170f79429158c.png@1c.webp', width: 1611, height: 118, x: 0, y: 30, r: 0, s: 1, o: 1, newX: 37.7396, newY: 30, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'a9a0eb9ce2e611290e86961a61c3b0942de3515b.png@1c.webp', width: 2039.4, height: 148.5, x: -55, y: 22, r: 0, s: 1, o: 1, newX: 17.6488, newY: 22, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '2422523efbc7d3486555c66a9a8f20c3e3f6fdb5.png@1c.webp', width: 95, height: 152, x: -300, y: 5, r: 0, s: 1, o: 1, newX: -215.086, newY: 5, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '16dc88de1e536a9b11dbe4acec40d44acbf30e4d.png@1c.webp', width: 184, height: 180, x: -165, y: 0, r: 0, s: 1, o: 1, newX: 23.6981, newY: 0, newRotate: 58.8698, bench: 188.698 },
	{ type: 'image', file: '85d3ee641e1af27233ce0571bfdc650bb6bac668.png@1c.webp', width: 140, height: 123, x: -200, y: 20, r: 0, s: 1, o: 1, newX: -11.3019, newY: 20, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'd86630ee943035f95c0e318bc386764196ab08b6.png@1c.webp', width: 200, height: 131, x: 200, y: 20, r: 0, s: 1, o: 1, newX: 341.524, newY: 20, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'e3a1d1ea8c4360518fa9bbef665795056ca1d50f.png@1c.webp', width: 798, height: 188.4, x: 660, y: 0, r: 0, s: 1, o: 1, newX: 886.438, newY: 0, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '459b9e641fc8be0466084ec0fff89e28ca2229ea.png@1c.webp', width: 45.5, height: 42.9, x: 521.3, y: -45.5, r: 0, s: 1, o: 1, newX: 753.116, newY: -45.5, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: '392e080a19e6618774a15c235ab0873180e4010d.png@1c.webp', width: 132, height: 97.5, x: 750, y: 45, r: 0, s: 1, o: 1, newX: 1089.66, newY: 228.981, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'd9ff91200e453584beb49626c2ddbd78e4dd60ed.png@1c.webp', width: 414, height: 216, x: -600, y: 0, r: 0, s: 1, o: 1, newX: -430.172, newY: 0, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'd29049442060427102881e002285ae50686052e2.png@1c.webp', width: 347, height: 80, x: -500, y: 50, r: 0, s: 1, o: 1, newX: -311.302, newY: 50, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'a2f14a60908565b6a4b85caee82bd0457127f9f3.png@1c.webp', width: 542.1, height: 143, x: -1170, y: 13, r: 0, s: 1, o: 1, newX: -679.385, newY: 13, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'f3bbed911be68b540c12fb7cbe602ae71ec9a8c7.png@1c.webp', width: 656.4, height: 116.4, x: -660, y: 60, r: 0, s: 1, o: 1, newX: -263.734, newY: 60, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'f3bbed911be68b540c12fb7cbe602ae71ec9a8c7.png@1c.webp', width: 601.7, height: 106.7, x: 990, y: 33, r: 0, s: 1, o: 1, newX: 1405.14, newY: 33, newRotate: 0, bench: 188.698 },
	{ type: 'image', file: 'a2f14a60908565b6a4b85caee82bd0457127f9f3.png@1c.webp', width: 417, height: 110, x: 600, y: 35, r: 0, s: 1, o: 1, newX: 977.396, newY: 35, newRotate: 0, bench: 188.698 },
];
</script>
