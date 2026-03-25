import os
from langchain_core.tools import tool
from utils.log_utils import knowledge_logger

# 延迟导入各种工具以避免循环导入和其他依赖问题
def get_rag_service():
    from rag.rag_summarize import RagSummarizeService
    return RagSummarizeService()

@tool(description="从向量存储中检索参考资料并总结")
def rag_summarize(query: str) -> str:
    """使用本地RAG服务检索和总结相关信息"""
    try:
        rag_service = get_rag_service()
        result = rag_service.rag_summarize(query)
        knowledge_logger.info(f'[RAG_TOOL] RAG查询成功 | query: {query}')
        return f'{{"type": "rag_result", "query": "{query}", "result": "{result}"}}'
    except Exception as e:
        error_msg = f"RAG查询失败: {str(e)}"
        knowledge_logger.error(f'[RAG_TOOL_ERROR] {error_msg}')
        return f'{{"error": "{error_msg}", "query": "{query}"}}'

# 从各个工具模块导入
try:
    from .tavily_tool import tavily_search_tool
    TAVILY_TOOLS_AVAILABLE = True
except ImportError:
    TAVILY_TOOLS_AVAILABLE = False
    print("警告: Tavily工具不可用，请检查tavily_tool.py文件。")
    # 定义占位符工具
    @tool
    def tavily_search_tool(query: str, max_results: int = 5) -> str:
        """Tavily搜索工具（不可用）"""
        return "Tavily搜索工具不可用"

try:
    from .datetime_tool import datetime_tool
    DATETIME_TOOLS_AVAILABLE = True
except ImportError:
    DATETIME_TOOLS_AVAILABLE = False
    print("警告: 时间工具不可用，请检查datetime_tool.py文件。")
    # 定义占位符工具
    @tool
    def datetime_tool() -> str:
        """日期时间工具（不可用）"""
        return "日期时间工具不可用"

try:
    from .baidu_search_tool import baidu_search_tool
    BAIDU_TOOLS_AVAILABLE = True
except ImportError:
    BAIDU_TOOLS_AVAILABLE = False
    print("警告: 百度搜索工具不可用，请检查baidu_search_tool.py文件。")
    # 定义占位符工具
    @tool
    def baidu_search_tool(query: str, max_results: int = 5, model: str = None) -> str:
        """百度搜索工具（不可用）"""
        return "百度搜索工具不可用"

# 可以根据需要添加更多工具
@tool(description="使用YOLOv8识别图片中的物体")
def yolo_object_detection(image_url: str) -> str:
    """对在线图片进行目标检测，识别图片中的物体内容"""
    try:
        from main import yolo_service
        if yolo_service is None:
            return "YOLO服务未初始化"
        result = yolo_service.detect_from_url(image_url)
        labels = [d['label'] for d in result['detections']]
        return f'{{"type": "yolo_result", "url": "{image_url}", "objects": {labels}}}'
    except Exception as e:
        error_msg = f"YOLO检测失败: {str(e)}"
        knowledge_logger.error(f'[YOLO_TOOL_ERROR] {error_msg}')
        return f'{{"error": "{error_msg}", "url": "{image_url}"}}'

available_tools = [
    rag_summarize,
    tavily_search_tool,
    datetime_tool,
    baidu_search_tool,
    yolo_object_detection
]