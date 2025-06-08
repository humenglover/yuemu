export type OriDataType = {
	type: 'image' | 'video',
	file: string,
	width: number, height: number,
	x: number, y?: number, r?: number, s?: number, o?: number,
	newX: number, newY?: number, newRotate?: number, newScale?: number, newOpacity?: number,
	bench: number, filter?: string,
}

export type InstDataType = {
	currentX: number,
	currentY: number,
	currentRotate: number,
	currentScale: number,
	currentOpacity: number,
};
