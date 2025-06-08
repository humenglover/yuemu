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

const baseSrc: string = '/bilibili/summer-6/';
const marginLeft: number = -100;
const moveRate: number = CommonUtil.isPhonePointerEvent() ? 50 : 100;
const maxLeftPosition = { index: 7, cut: 0 };
const maxRightPosition = { index: 5, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '39f5846ccf601e178afe37eef2e9759d38078d56.png@1c.webp', width: 2100, height: 196.7, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '0a3bf7a4c7c3b8b5b63aae6adf57c5fabeeb0cff.png@1c.webp', width: 201, height: 106.8, x: 78, y: 6, r: 0, s: 1, o: 1, newX: 78, newY: 6, newOpacity: 1, bench: 4.692 },
	{ type: 'video', file: '31c6df60-8e93-40bc-8f16-e144f90a3ff3.webm', width: 1800, height: 99.6, x: 36, y: 42, r: 0, s: 1, o: 1, newX: 36, newY: 42, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'e737e375a9bf412f802feaec18ee52ad517b4db9.png@1c.webp', width: 62.25, height: 23, x: -15, y: 32.5, r: 0, s: 1, o: 1, newX: -10.308, newY: 32.5, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '9539f58accbf88eb810a45ff2cbefbf1c29840c3.png@1c.webp', width: 755.2, height: 159.2, x: 800, y: -32, r: 0, s: 1, o: 1, newX: 815.014, newY: -32, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'b5514fc043485ce31d0c91a398aeb288261256dc.png@1c.webp', width: 1141, height: 170, x: 850, y: -10, r: 0, s: 1, o: 1, newX: 878.152, newY: -10, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '9cc0c1c3b87ca0ea32b80c3b08611e794108348b.png@1c.webp', width: 124.5, height: 46, x: 225, y: 30, r: 0, s: 1, o: 1, newX: 262.536, newY: 30, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '8512395c322494ef32b0fb40821e8e66fb291b53.png@1c.webp', width: 660.1, height: 162.4, x: -560, y: 0, r: 0, s: 1, o: 1, newX: -497.596, newY: 0, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'e737e375a9bf412f802feaec18ee52ad517b4db9.png@1c.webp', width: 149.4, height: 55.2, x: -660, y: 24, r: 0, s: 1, o: 1, newX: -575.544, newY: 24, newOpacity: 1, bench: 4.692 },
	{ type: 'video', file: 'ada15ae5-b072-434b-ab94-f1d8a683b74e.webm', width: 1255.8, height: 133.8, x: 0, y: 0, r: 0, s: 1, o: 0.6, newX: 0, newY: 0, newOpacity: 0.6, bench: 4.692 },
	{ type: 'image', file: '6e9dd4d7f26b117773661aeed3a357641d519f2a.png@1c.webp', width: 1465.1, height: 156.1, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 32.844, newY: -6.56881, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'f48b2d7535cae1ed97c7bb3c69d29e574cbe30c6.png@1c.webp', width: 225.5, height: 87.45, x: -110, y: 27.5, r: 0, s: 1, o: 1, newX: -58.3879, newY: 27.5, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '4d9b71139bc84c4ddd1b37a906115c4b0c54af87.png@1c.webp', width: 192.5, height: 102.5, x: 350, y: 35, r: 0, s: 1, o: 1, newX: 443.84, newY: 35, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'cd873292b7b3ae5603b546e1ecdf05fecaa955f3.png@1c.webp', width: 436.2, height: 168.6, x: -420, y: 0, r: 0, s: 1, o: 1, newX: -307.392, newY: 0, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'f3a02af183b808d236d0cc7997cbeb8958ef9bb0.png@1c.webp', width: 326.15, height: 108.9, x: -385, y: 22, r: 0, s: 1, o: 1, newX: -230.164, newY: 22, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: 'cd873292b7b3ae5603b546e1ecdf05fecaa955f3.png@1c.webp', width: 581.6, height: 224.8, x: 800, y: 0, r: 0, s: 1, o: 1, newX: 1025.22, newY: 0, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '3483ce9cad9fc3dd80f34cb164bcd5eeb1606332.png@1c.webp', width: 445.9, height: 140.7, x: -140, y: 0, r: 0, s: 1, o: 1, newX: 57.0642, newY: 0, newOpacity: 1, bench: 4.692 },
	{ type: 'image', file: '988a546df1cdf5eeefa0a5c319bc6b4bfca7d42d.png@1c.webp', width: 445.9, height: 140.7, x: 560, y: 0, r: 0, s: 1, o: 1, newX: 822.752, newY: 0, newOpacity: 1, bench: 4.692 },
];
</script>
