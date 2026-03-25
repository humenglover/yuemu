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

const baseSrc: string = '/bilibili/spring-1/';
const marginLeft: number = -220;
const moveRate: number = -10;
const maxLeftPosition = { index: 3, cut: 10 };
const maxRightPosition = { index: 2, cut: 200 };
const imgData: Array<OriDataType> = [
    { type: 'image', file: 'c12c6ceaa5f78de17cd94bb0ad325339b2386ee8.png', width: 4833, height: 180, x: 0, y: -15, o: 1, newX: 10.01, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '4d5ddb371f6d710cf0222a5d0cef46ea3ed5901c.png', width: 4833, height: 180, x: 1100, y: 0, o: 1, newX: 1110.01, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: 'c7a1dbf9bbc09c1f867b14a141c4e2f07c4b228d.png', width: 1585, height: 162, x: 675, y: 0, o: 1, newX: 688.514, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: 'd605a7076d5fe89d0d7e3d6219bffe4000033a09.png', width: 1439, height: 176, x: -637, y: 0, o: 1, newX: -592.856, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '8d947f22c6a67b8a5d2fbe1424ca2a946802e729.png', width: 250, height: 62, x: 607.5, y: 45, o: 1, newX: 618.761, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '65a860abda015ed247d48b1bcc5cbb9ce725d050.png', width: 205, height: 84, x: 252, y: 36.4, o: 0, newX: 257.606, newOpacity: -0.24374, bench: 10.01 }, //
    { type: 'image', file: '963685e7d82d27ba5e8187680814a2d548b5adbe.png', width: 1229, height: 125, x: 112, y: 14, o: 1, newX: 182.07, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '6a6e6e52a18a7731645aa1d93b1d74f2f628f77a.png', width: 1229, height: 81, x: -350, y: 49, o: 1, newX: -265.916, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '3969636231e7668ec91985bb147c89a6b69021c7.png', width: 198, height: 138, x: -240, y: 16, o: 0, newX: -199.96, newOpacity: 0.2002, bench: 10.01 }, //
    { type: 'image', file: '9af4cdc3c81e01522fc4df82a5b7cfa8b03b6b5b.png', width: 58, height: 102, x: -340, y: 32, o: 0, newX: -281.942, newOpacity: 0.2002, bench: 10.01 }, //
    { type: 'image', file: '4d8514308bd6673b215cdb1aadbf12891d784672.png', width: 270, height: 114, x: -90, y: 13.5, o: 1, newX: 22.6126, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '9b4f18f9f18c8dca17ba6c0b46a1dcedb649c82b.png', width: 2138, height: 180, x: 100, y: 0, o: 1, newX: 250.15, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: 'b24c872ae1ef34ebe794d1631a50b57fbdb0a482.png', width: 419, height: 147, x: 216, y: 13.5, o: 1, newX: 364.649, newOpacity: 1, bench: 10.01 },
    { type: 'image', file: '45031fa60ea6084886e80ca9e906803f9a3221b1.png', width: 444, height: 211, x: 2100, y: 0, o: 1, newX: 2310.21, newOpacity: 1, bench: 10.01, filter: 'blur(2px)' },
    { type: 'image', file: '17a00335345ecb9f5439069ee2d069772a9cede3.png', width: 958, height: 180, x: -1000, y: 0, o: 1, newX: -699.7, newOpacity: 1, bench: 10.01, filter: 'blur(1px)' },
];
</script>
