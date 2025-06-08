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

const baseSrc: string = '/bilibili/winter-3/';
const marginLeft: number = 0;
const moveRate: number = 300;
const maxLeftPosition = { index: 12, cut: 0 };
const maxRightPosition = { index: 4, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: 'cb5b4ca963e3a1a6d3917182561844b24a05fc20.png@1c.webp', width: 2304, height: 216, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, bench: 0 },
	{ type: 'image', file: 'bad95b1085650b58108b3491c9c13e1ef4e6b41f.png@1c.webp', width: 1282.8, height: 211.2, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 2.2818, bench: 2.2818 },
	{ type: 'image', file: 'b28b35a71a658e8bc449cd570337d7d9662d07d7.png@1c.webp', width: 1687, height: 173, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 11.409, bench: 2.2818 },
	{ type: 'image', file: '596c782fe6636269c65a50a9188fd4d16d12a83e.png@1c.webp', width: 1920, height: 160, x: 0, y: 10, r: 0, s: 1, o: 1, newX: 0, bench: 0 },
	{ type: 'image', file: '4cbfd5b2c00e1abb157e9ca3e79380bfa40d2b26.png@1c.webp', width: 1920, height: 83, x: 0, y: 55, r: 0, s: 1, o: 1, newX: 95.0749, bench: 2.2818 },
	{ type: 'image', file: '6b8bf544dec0088423136771f01696c68112a145.png@1c.webp', width: 1920, height: 111, x: 0, y: 40, r: 0, s: 1, o: 1, newX: 19.015, bench: 2.2818 },
	{ type: 'image', file: 'cd6dadba219330bf44d3f8bcb584aede77d37f79.png@1c.webp', width: 713, height: 106, x: -250, y: -10, r: 0, s: 1, o: 1, newX: -192.955, bench: 2.2818 },
	{ type: 'image', file: '31ff321fe1c2b003ab5c42beebb07f6c1652f66e.png@1c.webp', width: 53, height: 46.5, x: 300, y: 25, r: 0, s: 1, o: 1, newX: 228.694, bench: 2.2818 },
	{ type: 'image', file: 'cfb54458ad3e2101d245f3434222e33453825f4e.png@1c.webp', width: 260, height: 80, x: 130, y: 20, r: 0, s: 1, o: 1, newX: 196.552, bench: 2.2818 },
	{ type: 'image', file: '5103762686dc9627619b028354c15ba2a72b0886.png@1c.webp', width: 70, height: 61, x: -320, y: 20, r: 0, s: 1, o: 1, newX: -224.925, bench: 2.2818 },
	{ type: 'image', file: '09fa68c0c716596cebf6b1f81ed69a2a7a0a8c89.png@1c.webp', width: 82.8, height: 93.6, x: -180, y: 18, r: 0, s: 1, o: 1, newX: -77.3191, bench: 2.2818 },
	{ type: 'image', file: 'c2914ea86a54203e59b9b9fb1b8ab3461a5976ed.png@1c.webp', width: 341, height: 127, x: 75, y: 19, r: 0, s: 1, o: 1, newX: 217.612, bench: 2.2818 },
	{ type: 'image', file: '0cea6143cb8fce3ac55c0e1888a889c906a976aa.png@1c.webp', width: 832.8, height: 117.6, x: -720, y: 48, r: 0, s: 1, o: 1, newX: -491.82, bench: 2.2818 },
	{ type: 'image', file: 'c7cb3d67462676f79b3e702db60f4d4b038f3f28.png@1c.webp', width: 340, height: 174, x: -600, y: -5, r: 0, s: 1, o: 1, newX: -219.7, bench: 2.2818 },
	{ type: 'image', file: '459fbeaaff1fbeced05ac36f1cb0c0c70df3ff01.png@1c.webp', width: 454, height: 180, x: -850, y: 0, r: 0, s: 1, o: 1, newX: -374.625, bench: 2.2818 },
	{ type: 'image', file: 'bba2f3520bedef31ef33a816f11a9d22487b80aa.png@1c.webp', width: 422, height: 74, x: -1200, y: 55, r: 0, s: 1, o: 1, newX: -534.475, bench: 2.2818 },
	{ type: 'image', file: '7c2d36bba37320de03c417ed7d86111f7b05347e.png@1c.webp', width: 214, height: 118, x: 450, y: 20, r: 0, s: 1, o: 1, newX: 925.375, bench: 2.2818 },
	{ type: 'image', file: 'aa01bb2357ea84d7cc6cf3030769f2581a3ac98f.png@1c.webp', width: 126, height: 75, x: 350, y: 50, r: 0, s: 1, o: 1, newX: 920.45, bench: 2.2818 },
	{ type: 'image', file: '89d1c8fb55797a064a4d4aca51be2ebb8d86643a.png@1c.webp', width: 367, height: 180, x: 700, y: 0, r: 0, s: 1, o: 1, newX: 1460.6, bench: 2.2818 },
	{ type: 'image', file: 'bba2f3520bedef31ef33a816f11a9d22487b80aa.png@1c.webp', width: 422, height: 74, x: 1200, y: 55, r: 0, s: 1, o: 1, newX: 2008.14, bench: 2.2818 },
	{ type: 'video', file: '0eb9f058-7ec4-4539-8a2c-083bd91146d5.webm', width: 2112, height: 154, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 52.2912, bench: 2.2818 },
];
</script>
