<template>
	<BilibiliBannerBaseComponent
		:baseSrc="baseSrc"
		:moveRate="moveRate"
		:maxLeftPosition="maxLeftPosition" :maxRightPosition="maxRightPosition"
		:imgData="imgData">
	</BilibiliBannerBaseComponent>
</template>
<script setup lang="ts">
import BilibiliBannerBaseComponent from '@/banner/base/bilibili-banner-base.component.vue';
import type { OriDataType } from '../base/bilibili-banner-base.type';

const baseSrc: string = '/bilibili/spring-5/';
const moveRate: number = 300;
const maxLeftPosition = { index: 14, cut: 0 };
const maxRightPosition = { index: 22, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '17a1bcfacfc51f000cad0611dca32020b18db6f5.png@1c.webp', width: 2000, height: 187, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newRotate: 0, bench: 0 },
	{ type: 'image', file: 'f0ba9b7f8d4ec1745a08cabdf3951ceb4aa266d1.png@1c.webp', width: 2000, height: 187, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -0.424065, newY: 2.12032, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'be5632d3f86228a81979b8a85ce9cf0d5df541e0.png@1c.webp', width: 2400, height: 224.4, x: 300, y: 24, r: 0, s: 1, o: 1, newX: 298.728, newY: 24, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'f63cf9726b73c78ececc43ff16f3336fdfc76748.png@1c.webp', width: 2200, height: 205.7, x: 330, y: 33, r: 0, s: 1, o: 1, newX: 323.003, newY: 33, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '2c32dd208cb8f317ac78e03c78da5cd04b9a1410.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -2.54439, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'e04d0fbaaf67bdb0de7c40dca04180a9c1defa0a.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -3.81658, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'd5113ee4aaca71d7e7570a45b1e541efc0deca71.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -6.36097, newY: 15, newRotate: 0, bench: -0.424065 }, // 6
	{ type: 'image', file: 'ae0543610dd9b3d0903cec7a6982200691dce1b5.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -6.36097, newY: 15, newRotate: 0, bench: -0.424065 }, // 7
	{ type: 'image', file: '6d9f792b3458e0b6f8647f7ad3958b1c1438d50e.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -6.36097, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'f3945f0ed928e45fb58c8a1edb07358ec719c8df.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -8.48129, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'ba22577d90164e589f87f9073d56953ff3e30088.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -11.6618, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'd6ef89533fed72dfde53009ac2aab66009ab545a.png@1c.webp', width: 2000, height: 187, x: 100, y: 15, r: 0, s: 1, o: 1, newX: 93.639, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '8cacfce7b1f44f707836fa4fb68444a9c7202d63.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -12.7219, newY: 14.788, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'ff63f10698f5f0a9e9083d90c6f32de5269801d4.png@1c.webp', width: 2000, height: 187, x: 0, y: 10, r: 0, s: 1, o: 1, newX: -16.9626, newY: 9.78797, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '5630bebd1068fa6c46eaea4ffa9b004f882d7d33.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -16.9626, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'a7ead379e7a6653cd70f24e8556ce336702abb3a.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -16.9626, newY: 14.1519, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '06336184ba84f89890f50e867012c05914219947.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -25.4439, newY: 13.9398, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: 'ddc58cd92df71ada0be9bad00b2139a34872ef94.png@1c.webp', width: 2000, height: 187, x: -100, y: 15, r: 0, s: 1, o: 1, newX: -138.166, newY: 15, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '996a5656d3a31738f79b67d94ea0b654fba169ac.png@1c.webp', width: 1800, height: 168.3, x: -90, y: 0, r: 0, s: 1, o: 1, newX: -147.249, newY: 0, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '905fbc85df462f16c3fe0b83f0e5814696818a56.png@1c.webp', width: 2000, height: 187, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -25.4439, newY: 0, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '4d37acea06841ace858f27cab3a5596d9fdd8354.png@1c.webp', width: 2000, height: 187, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -31.8048, newY: 0, newRotate: 0, bench: -0.424065 },
	{ type: 'video', file: 'a0de269c-fb3b-42b6-8ea5-8be8fe8d95a1.webm', width: 180, height: 100, x: -245, y: 15, r: 0, s: 1, o: 1, newX: -301.852, newY: 15, newRotate: -0.371803, bench: -0.424065 },
	{ type: 'image', file: 'd4d4569741806055f36c4d9140bdd7069a50ec09.png@1c.webp', width: 2200, height: 205.7, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -46.6471, newY: 0, newRotate: 0, bench: -0.424065 },
	{ type: 'image', file: '2dd4a5f8aee1829bcdae687052853b8947a5d2f6.png@1c.webp', width: 2000, height: 187, x: 0, y: 15, r: 0, s: 1, o: 1, newX: -42.4065, newY: 15, newRotate: 0, bench: -0.424065 },
];
</script>
