import os
from langchain_core.tools import tool
from tavily import TavilyClient
from utils.log_utils import knowledge_logger
from model.factory import load_config

# 从配置文件加载Tavily配置
config = load_config()
tavily_config = config.get('tavily', {})
tavily_api_key = tavily_config.get('api_key', '')

# 初始化Tavily客户端（只有在API密钥存在时才初始化）
if tavily_api_key:
    tavily_client = TavilyClient(api_key=tavily_api_key)
else:
    # 如果没有API密钥，创建一个模拟客户端，返回错误信息
    class MockTavilyClient:
        def search(self, **kwargs):
            raise Exception("Tavily API密钥未配置")
    
    tavily_client = MockTavilyClient()

@tool(description="使用Tavily搜索引擎获取实时网络信息")
def tavily_search_tool(query: str, max_results: int = 5) -> str:
    """使用Tavily搜索引擎获取实时网络信息"""
    try:
        response = tavily_client.search(
            query=query,
            max_results=max_results,
            search_depth="advanced",  # 使用高级搜索深度
            include_answer=True,  # 包含AI生成的答案
            include_images=False,  # 不包含图片
            include_raw_content=False  # 不包含原始内容
        )
        
        # 返回结构化结果供Agent使用
        result_str = response.get('answer', f'未找到关于 "{query}" 的相关信息')
        knowledge_logger.info(f'[TAVILY_SEARCH_TOOL] 网络搜索成功 | query: {query}')
        return f'{{"type": "tavily_search_result", "query": "{query}", "result": "{result_str}"}}'
    except Exception as e:
        error_msg = f"网络搜索失败: {str(e)}"
        knowledge_logger.error(f'[TAVILY_SEARCH_TOOL_ERROR] {error_msg}')
        return f'{{"error": "{error_msg}", "query": "{query}"}}'

# 可用的Tavily工具列表
available_tavily_tools = [
    tavily_search_tool
]