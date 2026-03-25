import cv2
import numpy as np
import base64
import onnxruntime as ort
from typing import List, Dict, Any, Tuple
import os
from utils.log_utils import app_logger

class YoloService:
    def __init__(self, model_path: str):
        """
        初始化 YOLO 服务 (ONNX 模式)
        :param model_path: .onnx 模型文件的绝对路径
        """
        # 如果传入的是 .pt 路径，尝试自动转为 .onnx
        if model_path.endswith('.pt'):
            model_path = model_path.replace('.pt', '.onnx')
            
        self.model_path = model_path
        app_logger.info(f"正在加载 YOLO ONNX 模型: {model_path}")
        
        try:
            # 优先使用 CPU 运行以确保通用性
            self.session = ort.InferenceSession(model_path, providers=['CPUExecutionProvider'])
            self.input_name = self.session.get_inputs()[0].name
            
            # YOLOv8 默认类别名称 (COCO 数据集)
            self.classes = [
                'person', 'bicycle', 'car', 'motorcycle', 'airplane', 'bus', 'train', 'truck', 'boat', 
                'traffic light', 'fire hydrant', 'stop sign', 'parking meter', 'bench', 'bird', 'cat', 
                'dog', 'horse', 'sheep', 'cow', 'elephant', 'bear', 'zebra', 'giraffe', 'backpack', 
                'umbrella', 'handbag', 'tie', 'suitcase', 'frisbee', 'skis', 'snowboard', 'sports ball', 
                'Kite', 'baseball bat', 'baseball glove', 'skateboard', 'surfboard', 'tennis racket', 
                'bottle', 'wine glass', 'cup', 'fork', 'knife', 'spoon', 'bowl', 'banana', 'apple', 
                'sandwich', 'orange', 'broccoli', 'carrot', 'hot dog', 'pizza', 'donut', 'cake', 
                'chair', 'couch', 'potted plant', 'bed', 'dining table', 'toilet', 'tv', 'laptop', 
                'mouse', 'remote', 'keyboard', 'cell phone', 'microwave', 'oven', 'toaster', 'sink', 
                'refrigerator', 'book', 'clock', 'vase', 'scissors', 'teddy bear', 'hair drier', 'toothbrush'
            ]
            app_logger.info("YOLO ONNX 模型加载成功")
        except Exception as e:
            app_logger.error(f"YOLO ONNX 模型加载失败 (请确保模型已转换为 .onnx 格式): {str(e)}")
            raise e

    def preprocess(self, img):
        """预处理图像"""
        # 宽高等比缩放并填充 (Letterbox)
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        input_size = 640
        input_img = cv2.resize(img_rgb, (input_size, input_size))
        input_img = input_img.transpose(2, 0, 1) # HWC to CHW
        input_img = input_img.reshape(1, 3, input_size, input_size).astype(np.float32)
        input_img /= 255.0
        return input_img

    def detect(self, image_bytes: bytes) -> Dict[str, Any]:
        """
        进行目标检测 (ONNX 推理)
        """
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("无法解析图像数据")

        orig_h, orig_w = img.shape[:2]
        input_tensor = self.preprocess(img)
        
        # 运行推理
        outputs = self.session.run(None, {self.input_name: input_tensor})
        output = outputs[0][0] # [84, 8400] -> YOLOv8 输出格式 (box_cnt, anchor_cnt)
        output = output.transpose() # [8400, 84]

        boxes = []
        confidences = []
        class_ids = []

        # 阈值
        conf_threshold = 0.25
        nms_threshold = 0.45

        for row in output:
            classes_scores = row[4:]
            max_score = np.amax(classes_scores)
            if max_score >= conf_threshold:
                class_id = np.argmax(classes_scores)
                # YOLOv8 坐标是中心点格式 [cx, cy, w, h]
                cx, cy, w, h = row[0:4]
                
                # 缩放回原始尺寸
                x_scale = orig_w / 640
                y_scale = orig_h / 640
                
                left = int((cx - w / 2) * x_scale)
                top = int((cy - h / 2) * y_scale)
                width = int(w * x_scale)
                height = int(h * y_scale)
                
                boxes.append([left, top, width, height])
                confidences.append(float(max_score))
                class_ids.append(class_id)

        # NMS 非极大值抑制
        indices = cv2.dnn.NMSBoxes(boxes, confidences, conf_threshold, nms_threshold)
        
        detections = []
        if len(indices) > 0:
            indices = indices.flatten() if isinstance(indices, np.ndarray) else indices
            for i in indices:
                box = boxes[i]
                label = self.classes[class_ids[i]] if class_ids[i] < len(self.classes) else f"class_{class_ids[i]}"
                
                detections.append({
                    "label": label,
                    "confidence": confidences[i],
                    "x": box[0],
                    "y": box[1],
                    "width": box[2],
                    "height": box[3]
                })

                # 在原图上绘制检测结果用于返回
                color = (0, 255, 0)
                # 确保坐标不越界
                x, y, w, h = box
                cv2.rectangle(img, (x, y), (x + w, y + h), color, 2)
                text = f"{label} {confidences[i]:.2f}"
                cv2.putText(img, text, (max(0, x), max(10, y - 10)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)

        # 将标注后的图像转换为 Base64
        _, buffer = cv2.imencode('.jpg', img)
        annotated_base64 = base64.b64encode(buffer).decode('utf-8')

        return {
            "detections": detections,
            "annotatedImageBase64": annotated_base64
        }
