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

const baseSrc: string = '/bilibili/summer-7/';
const moveRate: number = CommonUtil.isPhonePointerEvent() ? 50 : 100;
const maxLeftPosition = { index: 7, cut: 0 };
const maxRightPosition = { index: 7, cut: 0 };
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '75ec2d45ce8c942a1f7379d4641171da4d90ab0d.png@1c.webp', width: 1728, height: 162, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newOpacity: 1, bench: 0 },
	{ type: 'image', file: '286eb259a60a0eabfcde96d7ea92d239fe68b3fe.png@1c.webp', width: 1696, height: 159, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -3.1362, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '782d55aeca6cc75f51d2d630005f514a61a0ddfa.png@1c.webp', width: 1760, height: 165, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -3.25455, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'd6c941cf2d5fc6c717173f7e3f166dbc444aa15b.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -9.23109, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '27e411d92729604aa594858beb5130ed60aad76d.png@1c.webp', width: 1760, height: 165, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -9.76366, newY: 0, newOpacity: 1, newScale: 0.881653, bench: -3.1362 },
	{ type: 'image', file: '09d0855b6b6d6965e8f02404777986237848c6c9.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -92.3109, newY: -3.07703, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '40878bbef514e2d4bf5d660fe1145c869567bec2.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -6.15406, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '42485baddbca05d2c4c7710a0b76b74d303e06d7.png@1c.webp', width: 1728, height: 162, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -25.563, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '8ea0e95a8e5fc85ae227810925dba1ace1e9fcba.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -36.9244, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'c13ca9c6405c71bf864ed2bc421680cb437f45ef.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -24.6162, newY: -12.3081, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'a43c6833d262301373234ffbd6934559d2ce7fb2.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -30.7703, newY: -15.3852, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'ce8c5e45230a6d3805baf60f5916f1cd441aac8e.png@1c.webp', width: 1728, height: 162, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -41.5399, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'cbf19f3682dfb02e62557d07fefaf241a80296a1.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -61.5406, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'video', file: 'dcf4044d-c928-4e4f-bd94-d5a34df80d0d.webm', width: 100, height: 250, x: 500, y: 0, r: 0, s: 1, o: 1, newX: 494.083, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '7998ca9f0bc267375fb7b45f75626d96806f94d7.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -92.3109, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '198efffbc58493300854c04ab0ea8d979a6f9223.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -86.1569, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: '142a486b8dd500a626a60b68ad993af8dabc8b55.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -92.3109, newY: -40.0014, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'bb6266e1525a51f7920fc8881e47cadeee271b0c.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -3.69608, newY: -6.15406, newOpacity: 1, bench: -3.1362 },
	{ type: 'image', file: 'cd68251cde11936871237ca94360acb451bf7ed2.png@1c.webp', width: 1664, height: 156, x: 0, y: 0, r: 0, s: 1, o: 0.5, newX: -257.852, newY: 0, newOpacity: 0.5, bench: -3.1362 },
	{ type: 'video', file: 'e305925c-56ec-44b5-b18e-1b8dc73b25f2.webm', width: 108, height: 270, x: 216, y: 0, r: 0, s: 1, o: 1, newX: 200.023, newY: 0, newOpacity: 1, bench: -3.1362 },
	{ type: 'video', file: '4596b25b-a9e6-47a2-b80c-d9c70c04d8eb.webm', width: 200, height: 500, x: -700, y: 0, r: 0, s: 1, o: 1, newX: -717.752, newY: 0, newOpacity: 1, bench: -3.1362 },
];
</script>
