
export class CommonUtil {

	public static isPhonePointerEvent(): boolean {
		return 'ontouchmove' in window;
	}

	public static randomNum(minNum: number, maxNum: number, decimalNum?: number): number {
		let max = 0, min = 0;
		if (minNum <= maxNum) {
			min = minNum;
			max = maxNum;
		} else {
			min = maxNum;
			max = minNum;
		}
		switch (arguments.length) {
			case 1:
				return Math.floor(Math.random() * (max + 1));
			case 2:
				return Math.floor(Math.random() * (max - min + 1) + min);
			case 3:
				return parseFloat((Math.random() * (max - min) + min).toFixed(decimalNum));
			default:
				return Math.random();
		}
	}
	
}
