<template>
	<video aria-hidden="true" data-recon-click="covers,onVideoBtnClick,covers,covers"
		data-recon-globalfocus="covers,playOnBrowserActive,covers,covers"
		data-recon-globalblur="covers,onBrowserBlur,covers,covers" preload="metadata" :muted="true" :loop="true"
		:autoplay="true" :playsinline="true" :src="props.src" :style="props.style" @canplay="onCanplay"
		@playing="onPlaying" ref="videoRef"></video>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps<{
	src: string;
	style?: Record<string, string | number>;
}>();

const eventName = 'ontouchmove' in window ? 'click' : 'click';
const videoRef = ref<HTMLVideoElement | null>(null);
const isPlayVideoCalled = ref(false);
const videoCanPlay = ref(false);
const videoIsPlaying = ref(false);

onMounted(() => {
	window.addEventListener(eventName, eventFunc);
})

onUnmounted(() => {
	window.removeEventListener(eventName, eventFunc);
})

const eventFunc = () => {
	if (!isPlayVideoCalled.value) {
		window.removeEventListener(eventName, eventFunc);
		setTimeout(() => {
			playVideo();
		}, 25);
	}
};

const playVideo = () => {
	isPlayVideoCalled.value = true;
	if (!videoCanPlay.value) {
		setTimeout(() => {
			playVideo();
		}, 25);
	} else {
		if (!videoIsPlaying.value) {
			videoRef.value?.play().then(() => {
			});
		}
	}
};

const onCanplay = () => {
	videoCanPlay.value = true;
}

const onPlaying = () => {
	videoIsPlaying.value = true;
}
</script>
