<template>
	<BilibiliBannerBaseComponent
		:baseSrc="baseSrc"
		:marginLeft="marginLeft" :moveRate="moveRate"
		:imgData="imgData">
	</BilibiliBannerBaseComponent>
</template>
<script setup lang="ts">
import BilibiliBannerBaseComponent from '@/banner/base/bilibili-banner-base.component.vue';
import type { OriDataType } from '../base/bilibili-banner-base.type';

const baseSrc: string = '/bilibili/winter-5/';
const marginLeft: number = 0;
const moveRate: number = 300;
const maxMove = { left: 2000, right: 2000 }; // 这两个是同时向外的，所以不用index定位
const imgData: Array<OriDataType> = [
	{ type: 'image', file: '477fb7a2f5e2cc1b2aa00c679c8ab168b47ff1b9.png@1c.webp', width: 1728, height: 162, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -1.17573, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'dd4aa5af4898a15dde074fe6b833bdfbc045dd47.png@1c.webp', width: 1632, height: 153, x: -42.5, y: 0, r: 0, s: 1, o: 1, newX: -46.2014, newY: 1.48055, newRotate: -2.17727, bench: -1.17573 },
	{ type: 'image', file: 'ae64c474ba2748b4fea1a1433b9b373598d3e686.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 0, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'd41b0292d9d1b0fce37a35f6efacac579093cdb3.png@1c.webp', width: 1728, height: 162, x: -63, y: -18, r: 0, s: 1, o: 1, newX: -68.8786, newY: -18, newRotate: -2.17727, bench: -1.17573 },
	{ type: 'image', file: 'a12cbab877db2b6d3b96ec012735cfe3998de399.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 3.70136, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '6f27fe80f495e83546921c2c58b2107c2ee89706.png@1c.webp', width: 1824, height: 171, x: 9.5, y: 0, r: 0, s: 1, o: 1, newX: -0.842045, newY: -2.06841, newRotate: 0, newScale: 0.978227, bench: -1.17573 },
	{ type: 'image', file: 'ac580ac636595e27b5d5c8b1f9d802751823f02f.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: 14.8055, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '987a407da2e02a12be549ad7f011dc11f73216e9.png@1c.webp', width: 1632, height: 153, x: 0, y: 8.5, r: 0, s: 1, o: 1, newX: 5.55205, newY: 7.38959, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '843ee84b16bab6274b98b317b3adaf1351b4be76.png@1c.webp', width: 1632, height: 153, x: 8.5, y: 0, r: 0, s: 1, o: 1, newX: -2.60409, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'a5607001ac06b4780106e2a10aa835657e553f07.png@1c.webp', width: 1632, height: 153, x: 8.5, y: 0, r: 0, s: 1, o: 1, newX: 15.9027, newY: -12.9548, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'b14cad3c76e957705810c26f4c6acc236f280721.png@1c.webp', width: 1632, height: 153, x: 17, y: 0, r: 0, s: 1, o: 1, newX: 11.448, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'f569be6834a5968c038bcc0fb0403f8f77e24c18.png@1c.webp', width: 1632, height: 153, x: 8.5, y: 0, r: 0, s: 1, o: 1, newX: 1.09727, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'eb98a42561de7d45f8856407c4531d2724ac6fc6.png@1c.webp', width: 1344, height: 126, x: 21, y: 21, r: 0, s: 1, o: 1, newX: -9.48182, newY: 17.9518, newRotate: 2.17727, newScale: 1.13064, bench: -1.17573 },
	{ type: 'image', file: '480a5c02dbcd3afd5f790cf621efbd1b2d39efcb.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -18.5068, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'cc1405d4d805baf9c459f67c03f095f70ec864c6.png@1c.webp', width: 1632, height: 153, x: 0, y: 17, r: 0, s: 1, o: 1, newX: -5.55205, newY: 18.8507, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '1696ffeacb26bcce7ad50037952620ee43614a4a.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -7.40273, newY: -2.96109, newRotate: 0, newScale: 1.08709, bench: -1.17573 },
	{ type: 'image', file: 'b0747b7f64e0e07e379a4bd416a0891b3e52c7d9.png@1c.webp', width: 1651.2, height: 154.8, x: 34.4, y: 0, r: 0, s: 1, o: 1, newX: 11.9305, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'ec9f99bf71be9408a359d7cd48dfb47e48ebce2c.png@1c.webp', width: 1632, height: 153, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -9.25341, newY: 6.29232, newRotate: -8.70909, newScale: 1.13064, bench: -1.17573 },
	{ type: 'image', file: 'c4cef2b063a853521c7f413d8e7782ff653e4126.png@1c.webp', width: 1632, height: 153, x: 17, y: 0, r: 0, s: 1, o: 1, newX: -12.6109, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '567e0221b3bd89e34d85c604549d915c95ca17d1.png@1c.webp', width: 1920, height: 180, x: 30, y: 0, r: 0, s: 1, o: 1, newX: 73.5455, newY: 0, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: '23c05e4d9e0ffbc16671e14c406fbe785b06ff9f.png@1c.webp', width: 1632, height: 153, x: 15.3, y: 17, r: 0, s: 1, o: 1, newX: -18.0123, newY: 17, newRotate: 0, bench: -1.17573 },
	{ type: 'image', file: 'fbfeb5de7e41fd1e888fca7ac76cbf2f8a42f3d3.png@1c.webp', width: 1920, height: 180, x: 0, y: 0, r: 0, s: 1, o: 1, newX: -43.5455, newY: 0, newRotate: 0, bench: -1.17573 },
];
</script>
