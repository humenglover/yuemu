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

const baseSrc: string = '/bilibili/autumn-3/';
const marginLeft: number = 0;
const moveRate: number = 50;
const maxLeftPosition = { index: 15, cut: 0 };
const maxRightPosition = { index: 16, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '0876da4f3ec73ff180fc87b46b6d1bcf4d52030a.png@1c.webp', width: 2100, height: 196.7, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, bench: 13.329 },
	{ type: 'image', file: '4adc24834b64143296d77bc81ccd952a59301b08.png@1c.webp', width: 1710.8, height: 196.7, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 13.329, newY: 0, bench: 13.329 },
	{ type: 'image', file: 'e33992d2d75647af38c50792c199fb9db006786c.png@1c.webp', width: 2100, height: 196.7, x: 21, y: 0, r: 0, s: 1, o: 1, newX: 54.3226, newY: 0, bench: 13.329 },
	{ type: 'image', file: '57102cf6e2e2845f2773581928c1d0f0c1985a3e.png@1c.webp', width: 1649.4, height: 168.6, x: -30, y: 0, r: 0, s: 1, o: 1, newX: 27.1245, newY: 0, bench: 13.329 },
	{ type: 'image', file: '0f79aa287163a3271c6cb22a73a6dc0fc210ab7c.png@1c.webp', width: 90, height: 81, x: 360, y: 0, r: 0, s: 1, o: 1, newX: 417.124, newY: 0, bench: 13.329 },
	{ type: 'image', file: '639de634f90688e86b7bd32924cfb029a316cde4.png@1c.webp', width: 84, height: 28.8, x: -240, y: -36, r: 0, s: 1, o: 1, newX: 331.245, newY: -36, bench: 13.329 },
	{ type: 'image', file: '852c3bd9d9608ceccb7265de8cea7cf3a4b039eb.png@1c.webp', width: 543.9, height: 196.7, x: 140, y: 0, r: 0, s: 1, o: 1, newX: -59.9356, newY: 0, bench: 13.329 },
	{ type: 'image', file: '9556f65510f73c44aa6756ee9b8cf7fc2562b906.png@1c.webp', width: 226.8, height: 196.7, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 333.226, newY: 0, bench: 13.329 },
	{ type: 'image', file: '530a4624fe408881101885c6f24c72e8c2a14fb6.png@1c.webp', width: 96.6, height: 108, x: -90, y: 12, r: 0, s: 1, o: 1, newX: 24.2489, newY: 12, bench: 13.329 },
	{ type: 'image', file: 'bf896616dec163402ec79a81f936af67932eca2f.png@1c.webp', width: 1713.6, height: 74.2, x: -350, y: 56, r: 0, s: 1, o: 1, newX: -183.387, newY: 56, bench: 13.329 },
	{ type: 'image', file: '32aa867cd3e8fba371a2be90d7d844a68c67f7a5.png@1c.webp', width: 180.4, height: 146.85, x: -165, y: 0, r: 0, s: 1, o: 1, newX: -7.90773, newY: 0, bench: 13.329 },
	{ type: 'image', file: 'c6263485a0218c887848c47a0fe4f515006e1567.png@1c.webp', width: 207, height: 160.8, x: -360, y: 12, r: 0, s: 1, o: 1, newX: -160.064, newY: 12, bench: 13.329 },
	{ type: 'image', file: 'cd4fda9478b06a8d81cd04f69d7a2ad475d1db40.png@1c.webp', width: 1650, height: 63, x: -132, y: 54, r: 0, s: 1, o: 1, newX: 96.4979, newY: 54, bench: 13.329 },
	{ type: 'image', file: '0479542df6a7c5deddf60fbbf63b6e1f1f770c08.png@1c.webp', width: 256.8, height: 161.4, x: 180, y: 12, r: 0, s: 1, o: 1, newX: 465.622, newY: 12, bench: 13.329 },
	{ type: 'image', file: 'b6f4359409b4550fb38594d8c227902db29c720a.png@1c.webp', width: 2550, height: 238.85, x: -85, y: -4.25, r: 0, s: 1, o: 1, newX: 400.558, newY: -4.25, bench: 13.329 },
	{ type: 'image', file: '8a6899088fc9dcd37a0039884fb77f69dc3b1c30.png@1c.webp', width: 500.8, height: 224.8, x: -1440, y: 0, r: 0, s: 1, o: 1, newX: -678.34, newY: 0, bench: 13.329 },
	{ type: 'image', file: '96704a6197153e1717a910e6c227937f34743cee.png@1c.webp', width: 408.6, height: 168.6, x: 1500, y: 0, r: 0, s: 1, o: 1, newX: 2356.87, newY: 0, bench: 13.329 },
];
</script>
