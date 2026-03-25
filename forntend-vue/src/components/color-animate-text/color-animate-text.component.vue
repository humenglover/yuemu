<template>
	<div ref="container"></div>
</template>
<script lang="ts">
import { defineComponent } from 'vue';
import { ColorAnimateTextInstance } from "@/components/color-animate-text/color-animate-text.instance.ts";

export default defineComponent({
	name: 'app-color-animate-text',
	props: {
		prefixString: { type: String, default: '' }, // 前缀文字
		texts: { type: [Array<string>], default: [''] }, // 需要做动效的文字
		defaultRun: { type: Boolean, default: true }, // 在初始化组件时是否默认运行
		infinite: { type: Boolean, default: true }, // 是否无限循环运行
		frameTime: { type: Number, default: 75 }, // 每一帧所用时间
		textWaitStep: { type: Number, default: 1 }, // 每个文字停留多久
		paragraphWaitStep: { type: Number, default: 2 }, // 每段文字停留多久
	},
	setup() {
		let instance!: ColorAnimateTextInstance;
		return {
			instance,
		}
	},
	created() {
		this.instance = new ColorAnimateTextInstance(this);
		this.instance.ngOnInit();
	},
	mounted() {
		this.instance.ngAfterViewInit();
	},
	beforeUnmount() {
		this.instance.ngOnDestroy();
	},
});
</script>
