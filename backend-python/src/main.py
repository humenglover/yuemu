from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi import File, UploadFile, Response
from typing import List, Optional, Dict, Any
import sys
import os

from datetime import datetime
import json
import time
import hashlib
import asyncio
import shutil
from concurrent.futures import ThreadPoolExecutor

# 添加项目根路径
sys.path.append(os.path.join(os.path.dirname(__file__)))

# 配置强制输出（保留配置但不再使用）
os.environ["FORCE_CONSOLE_OUTPUT"] = "True"

from model.factory import load_config

# 加载配置
config = load_config()
concurrency_config = config.get('concurrency', {})
thread_pool_config = concurrency_config.get('thread_pool', {})
max_workers = thread_pool_config.get('max_workers', 20)
thread_pool = ThreadPoolExecutor(max_workers=max_workers)

# 导入工具类和模型
from utils.log_utils import app_logger, upload_logger, knowledge_logger
from utils.file_utils import log_event, calculate_md5, check_md5_exists_only, check_md5_exists_permanently, \
    save_md5_with_filename, save_md5_permanently, find_filename_by_md5, get_file_documents

from model.dto.rag_request_dto import RAGRequestDTO, StreamRAGRequestDTO
from model.dto.rag_response_dto import RAGResponseDTO
from model.common.response_wrapper import ResponseWrapper
from model.common.constants import RAGConstants, HttpStatusCodes

from rag.vector_store import VectorStoreManager
from rag.rag_summarize import RAGSummarizer
from service.knowledge_management_service import knowledge_management_service
from service.rag_service import RAGService
from agent.react_agent import ReactAgent
from service.yolo_service import YoloService
from service.image_service import ImageService

# 全局组件
vector_store_manager = None
rag_summarizer = None
agent = None
rag_service = None
yolo_service = None
image_service = None

def init_core_components():
    """初始化核心组件"""
    global vector_store_manager, rag_summarizer, agent, rag_service, yolo_service, image_service

    app_logger.info("\n" + "=" * 80)
    app_logger.info(f"初始化RAG服务 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    app_logger.info("=" * 80)

    try:
        # 初始化向量存储
        app_logger.info("初始化向量存储管理器...")
        vector_store_manager = VectorStoreManager()

        # 初始化RAG摘要器
        app_logger.info("初始化RAG摘要器...")
        rag_summarizer = RAGSummarizer(vector_store_manager)

        # 初始化Agent
        app_logger.info("初始化ReactAgent...")
        agent = ReactAgent()
        rag_service = RAGService(rag_summarizer, agent)

        app_logger.info("RAG组件和Agent初始化成功！")
        app_logger.info("=" * 80 + "\n")

    except Exception as agent_err:
        app_logger.warning(f"Agent初始化失败: {str(agent_err)}，将仅启用RAG功能")
        try:
            vector_store_manager = VectorStoreManager()
            rag_summarizer = RAGSummarizer(vector_store_manager)
            rag_service = RAGService(rag_summarizer)

            app_logger.info("RAG组件初始化成功！")
            app_logger.info("=" * 80 + "\n")
        except Exception as rag_err:
            app_logger.error(f"RAG组件初始化失败: {str(rag_err)}")
            sys.exit(1)

    # ============================================================
    # 图像服务(YOLO + 抠图 + 人脸打码)独立初始化
    # 必须与上面的 RAG/Agent 链路完全解耦！
    # 避免 Agent 启动失败时连带导致 image_service=None 的致命 Bug。
    # ============================================================
    try:
        models_dir = os.path.join(os.path.dirname(__file__), "models")
        app_logger.info(f"初始化图像服务 | 模型目录: {models_dir}")

        yolo_model_path = os.path.join(models_dir, "yolov8n.onnx")
        app_logger.info(f"初始化YOLO服务 | 模型路径: {yolo_model_path}")
        yolo_service = YoloService(yolo_model_path)

        app_logger.info("初始化 ImageService (rembg + 人脸打码)...")
        image_service = ImageService(models_dir)

        app_logger.info("图像处理服务初始化成功！")
    except Exception as img_err:
        app_logger.error(f"[CRITICAL] 图像处理服务初始化失败: {str(img_err)}")
        # 图像服务失败不阻断整体启动，只是相关接口会报 500
        yolo_service = None
        image_service = None


# 执行初始化
init_core_components()

# 创建FastAPI应用
app = FastAPI(title="Python RAG Service", version="1.0.0")

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ========== API接口 ==========
@app.post("/api/rag/sync", response_model=RAGResponseDTO)
async def rag_sync(request: RAGRequestDTO):
    try:
        app_logger.info(f"\n处理RAG同步请求 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(thread_pool, rag_service.process_rag_sync, request)
        return result
    except Exception as e:
        error_msg = f"RAG同步处理失败：{str(e)}"
        app_logger.error(f"{error_msg}")
        app_logger.error(f"[RAG_SYNC_ERROR] {error_msg}")
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/rag/stream")
async def rag_stream(request: StreamRAGRequestDTO):
    try:
        app_logger.info(f"\n处理RAG流式请求 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

        from fastapi.responses import StreamingResponse
        return StreamingResponse(rag_service.process_rag_stream(request), media_type="text/event-stream")
    except Exception as e:
        error_msg = f"RAG流式处理失败：{str(e)}"
        app_logger.error(f"{error_msg}")
        app_logger.error(f"[RAG_STREAM_ERROR] {error_msg}")
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/rag/summarize", response_model=RAGResponseDTO)
async def rag_summarize(request: RAGRequestDTO):
    try:
        app_logger.info(f"\n处理RAG摘要请求 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(thread_pool, rag_service.process_summarize, request)
        return result
    except Exception as e:
        error_msg = f"RAG摘要处理失败：{str(e)}"
        app_logger.error(f"{error_msg}")
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/knowledge/upload")
async def upload_knowledge_file(file: UploadFile = File(...)):
    """文件上传接口"""
    response = await knowledge_management_service.process_upload_file(file, vector_store_manager)
    # 根据响应code设置HTTP状态码
    from starlette.responses import JSONResponse
    return JSONResponse(
        content=response.model_dump(),
        status_code=response.code
    )

@app.get("/api/knowledge/list")
async def get_all_knowledge_files(file_type: str = None):
    """获取文件列表接口"""
    return await knowledge_management_service.get_all_knowledge_files_api(file_type)

@app.post("/api/knowledge/clear-all")
async def clear_all_knowledge():
    """清空所有知识库"""
    return await knowledge_management_service.clear_all_knowledge_api(vector_store_manager)

@app.post("/api/knowledge/delete")
async def delete_knowledge_files_by_md5(request: dict):
    """删除文件接口"""
    return await knowledge_management_service.delete_multiple_knowledge_files_by_md5_api(request, vector_store_manager)

@app.get("/api/vector/verify/{file_md5}")
async def verify_vector_metadata(file_md5: str):
    """验证向量元数据"""
    return await knowledge_management_service.verify_vector_metadata_api(file_md5, vector_store_manager)

# ========== YOLO 目标检测接口 ==========
@app.post("/api/yolo/detect")
async def yolo_detect(file: UploadFile = File(...)):
    """图片上传检测接口"""
    try:
        content = await file.read()
        result = yolo_service.detect(content)
        return ResponseWrapper.success(data=result)
    except Exception as e:
        app_logger.error(f"YOLO检测失败: {str(e)}")
        return ResponseWrapper.error(msg=f"检测失败: {str(e)}")

@app.get("/api/yolo/detect-url")
async def yolo_detect_url(url: str):
    """URL图片检测接口"""
    try:
        result = yolo_service.detect_from_url(url)
        return ResponseWrapper.success(data=result)
    except Exception as e:
        app_logger.error(f"YOLO URL检测失败: {str(e)}")
        return ResponseWrapper.error(msg=f"检测失败: {str(e)}")

# ========== 一键去除背景 (智能抠图) 接口 ==========
@app.post("/api/ai/remove_bg")
async def remove_background(file: UploadFile = File(...)):
    """智能去除图片背景接口"""
    try:
        app_logger.info(f"开始去除图片背景: {file.filename}")
        
        # 读取上传图片的二进制数据
        input_image = await file.read()
        
        # 调用 image_service 执行业务逻辑
        output_image = image_service.remove_background(input_image)
        
        app_logger.info(f"背景去除完成: {file.filename}")
        # 直接返回抠图后的 PNG 二进制流 (透明背景)
        return Response(content=output_image, media_type="image/png")
    except Exception as e:
        error_msg = f"去除背景失败: {str(e)}"
        app_logger.error(error_msg)
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/ai/face_blur")
async def face_blur(file: UploadFile = File(...)):
    """人脸打马赛克接口"""
    try:
        app_logger.info(f"开始进行人脸打码: {file.filename}")
        
        # 读取上传图片的二进制数据
        input_image = await file.read()
        
        # 调用 image_service 执行业务逻辑
        blurred_image = image_service.blur_faces(input_image)
        
        app_logger.info(f"人脸打码完成: {file.filename}")
        # 直接返回打码后的 PNG 二进制流
        return Response(content=blurred_image, media_type="image/png")
    except Exception as e:
        error_msg = f"人脸打码失败: {str(e)}"
        app_logger.error(error_msg)
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/ai/face_blur")
async def face_blur(file: UploadFile = File(...)):
    """人脸打马赛克接口"""
    try:
        app_logger.info(f"开始进行人脸打码: {file.filename}")
        
        # 读取上传图片的二进制数据
        input_image = await file.read()
        
        # 调用 image_service 执行业务逻辑
        blurred_image = image_service.blur_faces(input_image)
        
        app_logger.info(f"人脸打码完成: {file.filename}")
        # 直接返回打码后的 PNG 二进制流
        return Response(content=blurred_image, media_type="image/png")
    except Exception as e:
        error_msg = f"人脸打码失败: {str(e)}"
        app_logger.error(error_msg)
        return ResponseWrapper.error(msg=error_msg)

@app.post("/api/ai/change_background")
async def change_background(
    file: UploadFile = File(...), 
    background_image: Optional[UploadFile] = File(None),
    color: Optional[str] = None
):
    """智能更换图片背景接口 (MODNet)"""
    try:
        app_logger.info(f"开始更换图片背景: {file.filename}, 颜色: {color}, 背景图: {background_image.filename if background_image else 'None'}")
        
        # 读取原图
        input_image = await file.read()
        
        # 读取背景图 (如果有)
        bg_image = None
        if background_image:
            bg_image = await background_image.read()
            
        # 调用 image_service 执行业务逻辑
        output_image = image_service.change_background(
            input_image_bytes=input_image,
            background_color=color,
            background_image_bytes=bg_image
        )
        
        app_logger.info(f"背景更换完成: {file.filename}")
        return Response(content=output_image, media_type="image/png")
    except Exception as e:
        error_msg = f"更换背景失败: {str(e)}"
        app_logger.error(error_msg)
        return ResponseWrapper.error(msg=error_msg)

@app.get("/")
async def root():
    """根路径"""
    return {
        "message": "Python RAG Service is running!",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat(),
        "components": {
            "rag": "available" if rag_summarizer else "unavailable",
            "agent": "available" if agent else "unavailable",
            "vector_store": "available" if vector_store_manager else "unavailable"
        }
    }

# 主函数
if __name__ == "__main__":
    import uvicorn

    try:
        config = load_config()
        host = config.get('fastapi_host', '127.0.0.1')
        port = config.get('fastapi_port', 8001)

        app_logger.info(f"\n启动Python RAG Service")
        app_logger.info(f"地址: http://{host}:{port}")
        app_logger.info(f"Python版本: {sys.version[:5]}")
        app_logger.info(f"启动时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        app_logger.info("=" * 80)

        # 启动服务（添加参数禁用输出缓冲）
        uvicorn.run(
            app,
            host=host,
            port=port,
            log_level="debug",  # 设置日志级别为debug
            access_log=True  # 启用访问日志
        )
    except Exception as e:
        error_msg = f"启动服务失败: {str(e)}"
        app_logger.error(f"\n{error_msg}")
        app_logger.error(f"[SERVICE_START_FAILED] {error_msg}")
        sys.exit(1)