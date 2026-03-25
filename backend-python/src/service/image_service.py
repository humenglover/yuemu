import os
from utils.log_utils import app_logger

class ImageService:
    def __init__(self, models_dir: str):
        """
        初始化图片处理服务
        :param models_dir: 存放所有 AI 模型的本地目录
        """
        app_logger.info("Initializing ImageService...")
        # 预设环境变量使得 U2NET 查找目录定向到内部 models（不下载）
        self.models_dir = os.path.abspath(models_dir)
        os.environ["U2NET_HOME"] = self.models_dir
        
        # 注意：rembg session 不在这里创建！
        # 采用「懒加载 + 用完即摧」策略，调用时临时创建，用完后立刻销毁归还内存。
        app_logger.info("Rembg session will be created on-demand (lazy load to save memory).")

        # 人脸检测 YOLO 模型体积较小(12MB)，常驻无压力
        self.face_model_path = os.path.abspath(os.path.join(models_dir, "yolov8n-face-lindevs.onnx"))
        app_logger.info(f"[DIAGNOSTIC] Face model expected at: {self.face_model_path}")
        
        self.face_session = None
        self.face_input_name = None
        
        self._init_face_session()

    def _init_face_session(self):
        """内部初始化方法，支持重复尝试"""
        if self.face_session is not None:
            return True
            
        if not os.path.exists(self.face_model_path):
            app_logger.error(f"[DIAGNOSTIC] Face model file MISSING: {self.face_model_path}")
            return False
            
        try:
            import onnxruntime as ort
            app_logger.info(f"Attempting to load YOLO Face model into ONNX session...")
            
            # 服务器环境可能需要显式设置 providers 为 CPU
            self.face_session = ort.InferenceSession(
                self.face_model_path, 
                providers=['CPUExecutionProvider']
            )
            self.face_input_name = self.face_session.get_inputs()[0].name
            app_logger.info("YOLO Face ONNX session initialized successfully.")
            return True
        except Exception as e:
            app_logger.error(f"[DIAGNOSTIC] ONNX Load ERROR: {str(e)}")
            self.face_session = None
            return False

    def remove_background(self, input_image_bytes: bytes) -> bytes:
        import numpy as np
        import cv2
        import gc
        
        # --- 1. 防止内存爆炸：大幅降低内部计算分辨率 ---
        MAX_SIZE = 512
        nparr = np.frombuffer(input_image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            raise ValueError("无法解析图像数据进行背景去除")
            
        h, w = img.shape[:2]
        processed_input_bytes = input_image_bytes
        
        if max(h, w) > MAX_SIZE:
            scale = MAX_SIZE / max(h, w)
            new_w, new_h = int(w * scale), int(h * scale)
            app_logger.info(f"图片尺寸过大 ({w}x{h})，自动缩放至 ({new_w}x{new_h}) 以防 OOM 内存崩溃")
            resized_img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)
            success, buffer = cv2.imencode('.png', resized_img)
            if success:
                processed_input_bytes = buffer.tobytes()
            del resized_img
                
        del img
        del nparr
        gc.collect()
            
        # --- 2. 懒加载 rembg session，用完立即销毁 ---
        try:
            app_logger.info("临时创建 rembg session (u2netp) 进行抠图...")
            from rembg import remove, new_session
            tmp_session = new_session("u2netp")
            
            output_image_bytes = remove(
                processed_input_bytes,
                session=tmp_session,
                alpha_matting=False
            )
            app_logger.info("抠图完成")
        finally:
            try:
                del tmp_session
            except:
                pass
            gc.collect()
            if os.name != 'nt':
                try:
                    import ctypes
                    libc = ctypes.CDLL("libc.so.6")
                    libc.malloc_trim(0)
                except Exception:
                    pass
        
        return output_image_bytes

    def change_background(self, input_image_bytes: bytes, background_color: str = None, background_image_bytes: bytes = None) -> bytes:
        """
        使用 U2NetP (via rembg) 进行背景提取并合成新背景
        :param input_image_bytes: 原始图片
        :param background_color: 背景颜色 (例如 '#FF0000')
        :param background_image_bytes: 背景图片
        :return: 合成后的图片 (PNG)
        """
        import cv2
        import numpy as np
        import gc
        from rembg import remove, new_session

        # 1. 解码图片
        nparr = np.frombuffer(input_image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Failed to decode input image")
        orig_h, orig_w = img.shape[:2]

        # 2. 获取人像 Alpha Mask (Matte)
        # 同样采用懒加载策略
        try:
            app_logger.info("创建临时 U2NetP session 提取 Mask...")
            tmp_session = new_session("u2netp")
            
            # 使用 only_mask=True 直接获取 Alpha 通道
            # 内部会自动进行预处理，我们直接传原始字节 (rembg 会处理缩放)
            mask_bytes = remove(input_image_bytes, session=tmp_session, only_mask=True)
            
            mask_nparr = np.frombuffer(mask_bytes, np.uint8)
            mask = cv2.imdecode(mask_nparr, cv2.IMREAD_GRAYSCALE)
            
            if mask is None:
                raise ValueError("Failed to extract mask from image")
            
            # 确保 Mask 尺寸与原图一致
            if mask.shape[:2] != (orig_h, orig_w):
                mask = cv2.resize(mask, (orig_w, orig_h), interpolation=cv2.INTER_LINEAR)
            
            # 归一化为 0-1 的浮点数
            alpha = mask.astype(float) / 255.0
            alpha = np.expand_dims(alpha, axis=-1)  # [H, W, 1]
            
        finally:
            try:
                del tmp_session
            except:
                pass
            gc.collect()

        # 3. 准备背景
        if background_image_bytes:
            bg_nparr = np.frombuffer(background_image_bytes, np.uint8)
            bg_img_raw = cv2.imdecode(bg_nparr, cv2.IMREAD_COLOR)
            if bg_img_raw is not None:
                # 适配背景：Cover 模式
                bg_h, bg_w = bg_img_raw.shape[:2]
                bg_scale = max(orig_w / bg_w, orig_h / bg_h)
                bg_new_w, bg_new_h = int(bg_w * bg_scale), int(bg_h * bg_scale)
                bg_resized = cv2.resize(bg_img_raw, (bg_new_w, bg_new_h), interpolation=cv2.INTER_AREA)
                
                start_x = (bg_new_w - orig_w) // 2
                start_y = (bg_new_h - orig_h) // 2
                bg_img = bg_resized[start_y:start_y+orig_h, start_x:start_x+orig_w]
            else:
                bg_img = np.zeros_like(img)
        elif background_color:
            try:
                color = background_color.lstrip('#')
                r, g, b = int(color[0:2], 16), int(color[2:4], 16), int(color[4:6], 16)
                bg_img = np.full((orig_h, orig_w, 3), (b, g, r), dtype=np.uint8)
            except:
                bg_img = np.full((orig_h, orig_w, 3), (255, 255, 255), dtype=np.uint8)
        else:
            bg_img = np.full((orig_h, orig_w, 3), (255, 255, 255), dtype=np.uint8)

        # 4. Alpha 混合合成
        img_float = img.astype(float)
        bg_float = bg_img.astype(float)
        
        # 合成公式：前景 * alpha + 背景 * (1 - alpha)
        combined = img_float * alpha + bg_float * (1.0 - alpha)
        combined = np.clip(combined, 0, 255).astype(np.uint8)

        # 5. 编码返回
        success, buffer = cv2.imencode('.png', combined)
        
        # 内存释放
        del nparr, img, mask, alpha, bg_img, img_float, bg_float, combined
        gc.collect()
        if os.name != 'nt':
            try:
                import ctypes
                libc = ctypes.CDLL("libc.so.6")
                libc.malloc_trim(0)
            except:
                pass

        if not success:
            raise ValueError("Failed to encode combined image")
        return buffer.tobytes()


    def blur_faces(self, input_image_bytes: bytes) -> bytes:
        """
        处理图片，检测人脸并进行高斯打码模糊处理，带最大分辨率限制以防内存爆满
        :param input_image_bytes: 原始图片二进制数据
        :return: 模糊人脸后的图片二进制数据 (PNG)
        """
        import cv2
        import numpy as np
        import gc

        # --- 核心修复：如果 session 未初始化，在此尝试最后一次加载并捕获详尽日志 ---
        if self.face_session is None:
            app_logger.info("Face session is None, attempting on-demand diagnostic initialization...")
            if not self._init_face_session():
                raise RuntimeError("YOLO Face ONNX session is not initialized properly. Please check logs for [DIAGNOSTIC] details.")

        # --- 1. 防止内存爆炸：大幅降低内部计算分辨率 ---
        # 降为 YOLOv8 训练的原生尺寸 640，防止非必要的放缩计算占用任何多余的几十M内存
        MAX_SIZE = 640
        nparr = np.frombuffer(input_image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        if img is None:
            raise ValueError("无法解析图像数据")

        h, w = img.shape[:2]
        
        # 如果尺寸过大，则进行等比例缩放
        if max(h, w) > MAX_SIZE:
            scale = MAX_SIZE / max(h, w)
            new_w, new_h = int(w * scale), int(h * scale)
            app_logger.info(f"[Face Blur] 图片尺寸过大 ({w}x{h})，自动缩放至 ({new_w}x{new_h}) 以防 OOM 内存崩溃")
            img = cv2.resize(img, (new_w, new_h), interpolation=cv2.INTER_AREA)

        # 更新当前处理的宽高
        orig_h, orig_w = img.shape[:2]
        orig_img = img.copy()

        # 释放不需要的原始字节流
        del nparr
        gc.collect()

        # 预处理：缩放为模型输入尺寸
        input_size = 640
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        input_img = cv2.resize(img_rgb, (input_size, input_size))
        input_img = input_img.transpose(2, 0, 1)  # HWC to CHW
        input_img = input_img.reshape(1, 3, input_size, input_size).astype(np.float32)
        input_img /= 255.0

        # 推理
        outputs = self.face_session.run(None, {self.face_input_name: input_img})
        output = outputs[0][0]  # [box_cnt, anchor_cnt]
        output = output.transpose()  # [8400, x]

        boxes = []
        confidences = []

        conf_threshold = 0.3
        nms_threshold = 0.45

        for row in output:
            # 取第一类的置信度为类别置信度，这需要看实际模型结构
            classes_scores = row[4:] 
            max_score = np.amax(classes_scores)
            
            if max_score >= conf_threshold:
                # 提取坐标
                cx, cy, _, _ = row[0:4]
                box_w, box_h = row[2], row[3]

                x_scale = orig_w / input_size
                y_scale = orig_h / input_size

                left = int((cx - box_w / 2) * x_scale)
                top = int((cy - box_h / 2) * y_scale)
                width = int(box_w * x_scale)
                height = int(box_h * y_scale)

                boxes.append([left, top, width, height])
                confidences.append(float(max_score))

        indices = cv2.dnn.NMSBoxes(boxes, confidences, conf_threshold, nms_threshold)
        
        if len(indices) > 0:
            indices = indices.flatten() if isinstance(indices, np.ndarray) else indices
            for i in indices:
                box = boxes[i]
                orig_x, orig_y, orig_w, orig_h = box
                
                # --- 核心优化：扩大包抄面积 (Padding) 30% 防止漏脸颊边缘 ---
                pad_w = int(orig_w * 0.3)
                pad_h = int(orig_h * 0.3)
                
                # 计算扩展后左上角的安全坐标
                new_x = max(0, orig_x - pad_w // 2)
                new_y = max(0, int(orig_y - pad_h * 0.55)) # 额头向上多扩一点点防止漏头发线
                
                # 计算扩展后右下角的安全坐标
                max_x = min(orig_img.shape[1], orig_x + orig_w + pad_w // 2)
                max_y = min(orig_img.shape[0], orig_y + orig_h + pad_h // 2)
                
                # 算出这块新画板的物理宽高
                face_w = max_x - new_x
                face_h = max_y - new_y
                
                if face_w <= 0 or face_h <= 0:
                    continue
                    
                # 重新提取更大面积的图片切片 (包含更多脸外围的头发和下巴空隙)
                face_roi = orig_img[new_y:max_y, new_x:max_x]
                
                blur_ksize = int(max(face_h, face_w) * 0.25)
                if blur_ksize % 2 == 0:
                    blur_ksize += 1
                blur_ksize = min(blur_ksize, 99) 
                
                if blur_ksize >= 3:
                    # 全局性模糊这个大画布
                    blurred_face = cv2.GaussianBlur(face_roi, (blur_ksize, blur_ksize), 30)
                    
                    # 在这个超级大画布中间，画满实心的胶囊/卵圆形面具。
                    # 因为画布被放大了，椭圆画满也没事，必定完全遮住处于中心的脸庞
                    mask = np.zeros((face_h, face_w), dtype=np.uint8)
                    center_x = face_w // 2
                    center_y = int(face_h * 0.55) # 仍稍微靠下一点躲开上方大额头
                    
                    # 椭圆半径撑满放大后画布的八成到九成
                    axes_w = int((face_w // 2) * 0.85) 
                    axes_h = int((face_h // 2) * 0.95)
                    
                    cv2.ellipse(mask, (center_x, center_y), (axes_w, axes_h), 0, 0, 360, 255, -1)
                    
                    # 大核极端羽化：把多出来的这 30% 的周围画布空间全用来做渐滑过渡！
                    feather_k = int(max(face_w, face_h) * 0.35)
                    if feather_k % 2 == 0: 
                        feather_k += 1
                    if feather_k >= 3:
                        mask = cv2.GaussianBlur(mask, (feather_k, feather_k), 0)
                    
                    # 融合操作
                    mask_3ch = cv2.merge([mask, mask, mask])
                    mask_float = mask_3ch.astype(float) / 255.0
                    
                    face_roi_float = face_roi.astype(float)
                    blurred_face_float = blurred_face.astype(float)
                    blended_face = (blurred_face_float * mask_float + face_roi_float * (1.0 - mask_float)).astype(np.uint8)
                    
                    # 将带有完美过渡晕染的画板放回放大的原图区域内
                    orig_img[new_y:max_y, new_x:max_x] = blended_face
        
        # 将结果编码为PNG
        success, buffer = cv2.imencode('.png', orig_img)
        
        # 强制释放推理用的大内存变量
        del input_img
        del img_rgb
        del orig_img
        gc.collect()
        
        if not success:
            raise ValueError("模糊图像编码失败")
            
        # 针对 Linux 环境的特殊优化：强制 C 运行时归还内存给操作系统
        if os.name != 'nt':
            try:
                import ctypes
                libc = ctypes.CDLL("libc.so.6")
                libc.malloc_trim(0)
            except Exception:
                pass

        return buffer.tobytes()
