<style lang="scss" scoped src="./bilibili-banner-base.component.scss"></style>
<template>
	<div ref="containerRef" class="container" @mouseenter="onMouseEnter" @mousemove="onMouseMove"
		 @mouseleave="onMouseLeave" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
		<div class="animated-banner">
			<template v-if="imgNewData.length == props.imgData.length">
				<div v-for="(each, i) in props.imgData" :key="i" class="layer">
					<template v-if="each.type === 'image'">
						<img :src="baseSrc + each.file" :style="{
												 width: each.width + 'px',
												 height: each.height + 'px',
												 transform: '' +
													 'translate(' +
													 (each.x == undefined ? 0 : imgNewData[i].currentX) + 'px, ' +
													 (each.y == undefined ? 0 : imgNewData[i].currentY) + 'px' +
													 ')' + ' ' +
													 'rotate(' + (each.r == undefined ? 0 : imgNewData[i].currentRotate) + 'deg)' + ' ' +
													 'scale(' + (each.s == undefined ? 1 : imgNewData[i].currentScale) + ')',
												 opacity: each.o == undefined ? 1 : imgNewData[i].currentOpacity,
												 filter: each.filter == undefined ? 'none' : each.filter,
												 transition: transition + 's'
											 }"/>
					</template>
					<template v-else>
						<AutoPlayVideoComponent :src="props.baseSrc + each.file" :style="{
												 width: each.width + 'px',
												 height: each.height + 'px',
												 transform: '' +
													 'translate(' +
													 (each.x == undefined ? 0 : imgNewData[i].currentX) + 'px, ' +
													 (each.y == undefined ? 0 : imgNewData[i].currentY) + 'px' +
													 ')' + ' ' +
													 'rotate(' + (each.r == undefined ? 0 : imgNewData[i].currentRotate) + 'deg)' + ' ' +
													 'scale(' + (each.s == undefined ? 1 : imgNewData[i].currentScale) + ')',
												 opacity: each.o == undefined ? 1 : imgNewData[i].currentOpacity,
												 filter: each.filter == undefined ? 'none' : each.filter,
												 transition: transition + 's'
											 }"></AutoPlayVideoComponent>
					</template>
				</div>
			</template>
		</div>
		<div v-if="hasTouchEvent" class="logo" :style="{ opacity: logoShow ? '1' : '0' }" @click="reset">
			<img :src="props.baseSrc + 'logo.png'"/>
		</div>
		<div v-else class="logo" :style="{ opacity: logoShow ? '1' : '0' }">
			<img :src="props.baseSrc + 'logo.png'"/>
		</div>
	</div>
</template>
<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { BILIBILI_BANNER_LOGO_SHOW } from "@/banner/config.ts";
import { CommonUtil } from "@/banner/common-util.ts";
import type { InstDataType, OriDataType } from "@/banner/base/bilibili-banner-base.type.ts";
import AutoPlayVideoComponent from '@/auto-play-video/auto-play-video.component.vue';
import { ResizeObserverWrap } from "@/banner/resize-observer.wrap.ts";

const props = defineProps<{
	baseSrc: string,
	imgData: OriDataType[],
	marginLeft?: number,
	moveRate?: number,
	maxMove?: { left: number, right: number },
	maxLeftPosition?: { index: number, cut: number },
	maxRightPosition?: { index: number, cut: number },
}>()

const marginLeft = props.marginLeft ?? 0
const moveRate = props.moveRate ?? 1

const logoShow: boolean = BILIBILI_BANNER_LOGO_SHOW;
const hasTouchEvent: boolean = CommonUtil.isPhonePointerEvent();

const containerRef = ref<HTMLDivElement | null>(null);
const resizeObserver = ref<ResizeObserverWrap | null>(null);
const startPoint = ref({ x: 0, y: 0 });
const imgNewData = ref<InstDataType[]>([]);
const transition = ref(0.75);
const isReseting = ref(false);
const isInResetingEnter = ref(false);
const moveX = ref(0);
const containerWidth = ref(0);

onMounted(() => {
	resizeObserver.value = new ResizeObserverWrap(containerRef.value!, (e) => {
		containerWidth.value = e.clientWidth;
	});
	props.imgData.forEach((each, i) => {
		each.x += marginLeft;
		each.newX += marginLeft;
		imgNewData.value.push({
			currentX: each.x,
			currentY: each.y == undefined ? 0 : each.y,
			currentRotate: each.r == undefined ? 0 : each.r,
			currentScale: each.s == undefined ? 0 : each.s,
			currentOpacity: each.o == undefined ? 1 : each.o,
		});
	});
})

onUnmounted(() => {
	resizeObserver.value?.unmount();
})

/** 鼠标移动事件 */
const onMouseEnter = (e: any): void => {
	if (hasTouchEvent) {
		return;
	}
	if (isReseting.value) {
		isInResetingEnter.value = true;
	} else {
		bilibiliStart(e.clientX, e.clientY);
	}
}

const onMouseMove = (e: any): void => {
	if (hasTouchEvent) {
		return;
	}
	if (!isReseting.value) {
		if (isInResetingEnter.value) {
			bilibiliStart(e.clientX, e.clientY);
			isInResetingEnter.value = false;
		} else {
			bilibiliMove(e.clientX, e.clientY);
		}
	}
}

const onMouseLeave = (e: any): void => {
	if (hasTouchEvent) {
		return;
	}
	bilibiliEnd();
	reset();
}

/** 触摸事件 */
const onTouchStart = (e: any): void => {
	if (hasTouchEvent) {
		bilibiliStart(e.touches[0].clientX, e.touches[0].clientY);
	}
}

const onTouchMove = (e: any): void => {
	if (hasTouchEvent) {
		bilibiliMove(e.touches[0].clientX, e.touches[0].clientY);
	}
}

const onTouchEnd = (e: any): void => {
	if (hasTouchEvent) {
		bilibiliEnd();
	}
}

const bilibiliStart = (x: number, y: number): void => {
	startPoint.value.x = x;
	startPoint.value.y = y;
	transition.value = 0;
}

const bilibiliMove = (x: number, y: number): void => {
	let __moveX = (x - startPoint.value.x) / moveRate;
	startPoint.value.x = x;
	moveX.value += __moveX;
	if (props.maxMove) {
		let v = moveX.value * moveRate;
		if (__moveX < 0 && v * -1 > props.maxMove.left) {
			return;
		} else if (__moveX > 0 && v > props.maxMove.right) {
			return;
		}
	}
	if (moveX.value > 0) {
		if (Math.abs(moveX.value) > getLeftWidth()) {
			moveX.value -= __moveX;
			return;
		}
	} else {
		if (Math.abs(moveX.value) > getRightWidth()) {
			moveX.value -= __moveX;
			return;
		}
	}
	for (let i = 0; i < props.imgData.length; i++) {
		moveFunction(i);
	}
}

const bilibiliEnd = (): void => {
	startPoint.value.x = 0;
	startPoint.value.y = 0;
}

const getLeftWidth = (): number => {
	let leftWidth;
	if (props.maxLeftPosition) {
		let i = props.maxLeftPosition.index;
		let r = (props.imgData[i].newX - props.imgData[i].x) / props.imgData[i].bench;
		leftWidth = (props.imgData[i].width - containerWidth.value) / 2 - props.imgData[i].x;
		leftWidth = leftWidth - props.maxLeftPosition.cut;
		leftWidth = leftWidth / r;
	} else {
		leftWidth = containerWidth.value / 2 - marginLeft;
	}
	return leftWidth;
}

const getRightWidth = (): number => {
	let rightWidth;
	if (props.maxRightPosition) {
		let i = props.maxRightPosition.index;
		let r = (props.imgData[i].newX - props.imgData[i].x) / props.imgData[i].bench;
		rightWidth = (props.imgData[i].width - containerWidth.value) / 2 + props.imgData[i].x;
		rightWidth = rightWidth - props.maxRightPosition.cut;
		rightWidth = rightWidth / r;
	} else {
		rightWidth = containerWidth.value / 2 + marginLeft;
	}
	return rightWidth;
}

const moveFunction = (i: number): void => {
	let bench = props.imgData[i].bench;
	// 移动量 - X
	let x = props.imgData[i].x;
	let newX = props.imgData[i].newX;
	if (x != null && newX != null && x != newX) {
		let x1 = (newX - x) / bench;
		imgNewData.value[i].currentX = x1 * moveX.value + x;
	}
	// 移动量 - Y
	let y = props.imgData[i].y;
	let newY = props.imgData[i].newY;
	if (y != null && newY != null && y != newY) {
		let y1 = (newY - y) / bench;
		imgNewData.value[i].currentY = y1 * moveX.value + y;
	}
	// 移动量 - Rotate
	let r = props.imgData[i].r;
	let newRotate = props.imgData[i].newRotate;
	if (r != null && newRotate != null && r != newRotate) {
		let r1 = (newRotate - r) / bench;
		imgNewData.value[i].currentRotate = r1 * moveX.value + r;
	}
	// 移动量 - Scale
	let s = props.imgData[i].s;
	let newScale = props.imgData[i].newScale;
	if (s != null && newScale != null && s != newScale) {
		let s1 = (newScale - s) / bench;
		imgNewData.value[i].currentScale = s1 * moveX.value + s;
	}
	// 移动量 - Opacity
	let o = props.imgData[i].o;
	let newOpacity = props.imgData[i].newOpacity;
	if (o != null && newOpacity != null && o != newOpacity) {
		let o1 = (newOpacity - o) / bench;
		imgNewData.value[i].currentOpacity = o1 * moveX.value + o;
	}
	if (imgNewData.value[i].currentOpacity < 0) { // 透明度检测，在0~1范围内
		imgNewData.value[i].currentOpacity = 0;
	} else if (imgNewData.value[i].currentOpacity > 1) {
		imgNewData.value[i].currentOpacity = 1;
	}
}

const reset = (): void => {
	transition.value = 0.75
	moveX.value = 0;
	for (let i = 0; i < props.imgData.length; i++) {
		let data = props.imgData[i];
		imgNewData.value[i].currentX = data.x;
		imgNewData.value[i].currentY = data.y == undefined ? 0 : data.y;
		imgNewData.value[i].currentRotate = data.r == undefined ? 0 : data.r;
		imgNewData.value[i].currentScale = data.s == undefined ? 0 : data.s;
		imgNewData.value[i].currentOpacity = data.o == undefined ? 0 : data.o;
	}
	isReseting.value = true;
	setTimeout(() => {
		isReseting.value = false;
	}, 750);
}
</script>
