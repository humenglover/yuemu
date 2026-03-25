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
import { CommonUtil } from "@/banner/common-util.ts";

const baseSrc: string = '/bilibili/autumn-5/';
const moveRate: number = CommonUtil.isPhonePointerEvent() ? 50 : 150;
const maxLeftPosition = { index: 12, cut: 0 };
const maxRightPosition = { index: 12, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '18e14b236c8c0478b9f126b53dc149718c8277d6.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newOpacity: 1, bench: 0 },
	{ type: 'image', file: '2c048730ce3449400cd657650f5f2e68e2fa5ef5.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -1.7317, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '582b9609daf669327c460af63776f5f0c14b5c26.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -3.4634, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: 'f5fcc108b811577571e3f54d6d46df58a9f553ed.png@1c.webp', width: 1696, height: 159, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -7.06, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '4367bada261c826302101afa18fe41e86f545099.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -6.92679, newY: -0.34634, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '0ebcccd16a9fe9bb551e213de4eaf3cd5a24c8ff.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -7.79264, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '21a613a7457891ed7daa559b03191df7bf551362.png@1c.webp', width: 1664, height: 156, x: 104, y: 0, r: 0, s: 1, o: 1, newX: 17.4151, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '83b58af34fc683e529962449078a3ded7f1d2e80.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -8.65849, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '6683112849dadee65749c4f7f72b83c9a9f8f537.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -10.3902, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '93ab310ca3e2a24489aa1873865501adb3f81393.png@1c.webp', width: 1664, height: 156, x: 10.4, y: 0, r: 0, s: 1, o: 1, newX: 2.60736, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '064a75156c568aa6a9982e2c983d2a30cb86ea94.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -12.1219, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '7bb79a88a5c61724c7fd2ab009f462b744ba5b29.png@1c.webp', width: 1696, height: 159, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -8.825, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '1b6718fe3996574889da7ef1b9c29a566fe963f4.png@1c.webp', width: 1728, height: 162, x: 0, y: 1.08, r: 0, s: 1, o: 1, newX: -13.4873, newY: 1.08, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: 'c31995a7a80e459e165ae4ef60e323e27f497420.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -17.317, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '4a66f79212d4c6182a8e10a1eb0424f3d7c79120.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -13.8536, newY: -0.865849, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '30ccbdfe561bcd18a7ee457bd151b55da7a3dd3c.png@1c.webp', width: 1664, height: 156, x: 0, y: -10.4, r: 0, s: 1, o: 1, newX: -69.2679, newY: -10.4, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '20ccfde3e5606d223ab230a78234e4f9ac140371.png@1c.webp', width: 104, height: 67.08, x: 260, y: 10.4, r: 0, s: 1, o: 1, newX: 190.732, newY: 10.4, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: 'd562864113a6e6cce54b41745132a974b4576c3c.png@1c.webp', width: 133.12, height: 85.28, x: 104, y: 10.4, r: 0, s: 1, o: 1, newX: 52.0491, newY: 11.2658, newScale: 1.0621, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: 'ab4fbaed481d880a618e52f97e7e1f6e5c8b7f7c.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -103.902, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'video', file: 'b66a4ac5-5c0b-487e-86d2-f787b70a4ace.webm', width: 1664, height: 156, x: 0, y: 10.4, r: 0, s: 1, o: 1, newX: -34.634, newY: 10.4, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '57f20e919517fc784f1e03900140274cfea2cb85.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -51.9509, newY: 0, newOpacity: 1, bench: -1.7317 },
	{ type: 'image', file: '670d70b0351582e5b53855ddbf50600c03425062.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -86.5849, newY: 0, newOpacity: 1, bench: -1.7317 },
];
</script>
