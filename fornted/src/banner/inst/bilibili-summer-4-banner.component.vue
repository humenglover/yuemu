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
import { CommonUtil } from "@/banner/common-util.ts";

const baseSrc: string = '/bilibili/summer-4/';
const marginLeft: number = 40;
const moveRate: number = CommonUtil.isPhonePointerEvent() ? 50 : 150;
const maxLeftPosition = { index: 14, cut: -200 };
const maxRightPosition = { index: 14, cut: -100 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '58a4287288d296ccb9235e49f330cc9f8ae3f6e8.png@1c.webp', width: 1950, height: 183, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 5.005, newY: 0, newRotate: 0.77, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '4d444aa8fafe9dac2db3b847657938a24a2fa83d.png@1c.webp', width: 158, height: 85, x: -280, y: 35, r: 0, s: 1, o: 1, newX: -296.22, newY: 24.22, newRotate: -7.7, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '93b789dfa303f9458d31690c2f13a4fc719da2db.png@1c.webp', width: 571, height: 153, x: -350, y: 0, r: 0, s: 1, o: 1, newX: -296.1, newY: 0, newRotate: 2.31, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: 'dc63a982b67922e999d743d3772091a8b412671c.png@1c.webp', width: 458, height: 86, x: -225, y: -15, r: 5, s: 1, o: 1, newX: -207.65, newY: -15, newRotate: 1.15, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '109794fd095a7328501365454faae6c91bc914cb.png@1c.webp', width: 1446, height: 211, x: 75, y: -7.5, r: 0, s: 1, o: 1, newX: 86.55, newY: -7.5, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: 'b83df7a50dd88a9f4558e1773b48fe523705d333.png@1c.webp', width: 314, height: 153, x: -175, y: 0, r: 0, s: 1, o: 1, newX: -207.34, newY: 0, newRotate: 3.85, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '640b9089660a21c30698edd35fd89146b28a25a2.png@1c.webp', width: 158, height: 109, x: 225, y: 0, r: 0, s: 1, o: 1, newX: 282.75, newY: 57.75, newRotate: 15.4, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '502ef5bcff5ae1588acf6abaf8ec57115183b6a5.png@1c.webp', width: 1721, height: 197, x: -245, y: 0, r: 0, s: 1, o: 1, newX: -201.88, newY: 0, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '5f2aa97bddf032a32420313465904560e96e791d.png@1c.webp', width: 457, height: 158, x: 10.5, y: 10.5, r: 0, s: 1, o: 1, newX: 64.4, newY: 21.28, newRotate: 6.16, newScale: 1.077, newOpacity: 1, bench: 5.005 },
	{ type: 'image', file: '4d58f4c657906f900a37eb6a9dcdaab5d7c31ef6.png@1c.webp', width: 419, height: 145, x: -120, y: 0, r: 0, s: 1, o: 0, newX: -138.48, newY: 0, newRotate: 0, newScale: 0.923, newOpacity: 0.945721, bench: 5.005 },
	{ type: 'image', file: '1f7d26c7582e7b320cb21923175613c9d8077f56.png@1c.webp', width: 818, height: 197, x: 0, y: 21, r: 0, s: 1, o: 1, newX: 107.8, newY: 21, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005, filter: 'blur(1px)' },
	{ type: 'image', file: 'b4fcb84965fadf036875b8189910280b3edb182c.png@1c.webp', width: 643, height: 164, x: 324, y: 0, r: 0, s: 1, o: 1, newX: 434.88, newY: -11.088, newRotate: -3.85, newOpacity: 1, newScale: 1, bench: 5.005 },
	{ type: 'image', file: '6ea563b54bfaf1830ed95edd295e2521c14ecf38.png@1c.webp', width: 300, height: 186, x: 381.6, y: 0, r: 0, s: 1, o: 1, newX: 547.92, newY: -27.72, newRotate: -1.54, newOpacity: 1, newScale: 1, bench: 5.005, filter: 'blur(2px)' },
	{ type: 'image', file: '79abfde6b4ac5d95fd4df0dcea96c3d14d55f64c.png@1c.webp', width: 2132, height: 211, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 202.125, newY: 0, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005, filter: 'blur(1px)' },
	{ type: 'image', file: '0b9e2b95931f266a853ba046b6c225e49511a428.png@1c.webp', width: 1806, height: 225, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 246.4, newY: 0, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005, filter: 'blur(3px)' },
	{ type: 'image', file: 'b772ae3b3ed3342b0cbf26b6f2e66a054b7af81b.png@1c.webp', width: 2400, height: 225, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 308, newY: 30.8, newRotate: 0, newScale: 1, newOpacity: 1, bench: 5.005 },
];
</script>
