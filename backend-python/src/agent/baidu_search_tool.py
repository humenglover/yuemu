import os
import requests
import json
from langchain_core.tools import tool
from utils.log_utils import knowledge_logger
from model.factory import load_config

# 加载百度搜索配置
config = load_config()
baidu_config = config.get('baidu_search', {})
baidu_api_key = baidu_config.get('api_key', '')
baidu_base_url = baidu_config.get('base_url', 'https://qianfan.baidubce.com/v2/ai_search/chat/completions')
baidu_model = baidu_config.get('model', 'ernie-3.5-lite')
max_results_default = baidu_config.get('max_results_default', 5)

@tool
def baidu_search_tool(query: str, max_results: int = 5, model: str = None) -> str:
    """
    使用百度AI搜索获取实时网络信息，返回结构化结果供Agent使用
    Args:
        query: 搜索查询字符串
        max_results: 最大返回结果数，默认为5
        model: 使用的模型，默认为ernie-3.5-lite
    """
    if not baidu_api_key:
        return f"{{\"error\": \"百度搜索工具未配置API密钥\", \"query\": \"{query}\"}}"
    
    try:
        headers = {
            "Authorization": f"Bearer {baidu_api_key}",
            "Content-Type": "application/json"
        }
        
        payload = {
            "messages": [
                {
                    "content": query,
                    "role": "user"
                }
            ],
            "stream": False,
            "model": model or baidu_model,
            "instruction": "请用简洁的语言总结查询结果，仅返回核心答案",
            "enable_corner_markers": False,
            "enable_deep_search": False,
            "temperature": 0.1,
            "max_tokens": 500
        }
        
        response = requests.post(
            baidu_base_url,
            headers=headers,
            json=payload,
            timeout=15
        )
        
        if response.status_code != 200:
            return f"{{\"error\": \"百度搜索请求失败: {response.status_code}\", \"query\": \"{query}\"}}"
        
        result = response.json()
        
        if 'choices' in result and len(result['choices']) > 0:
            answer = result['choices'][0]['message']['content'].strip()
            search_result = answer if answer else f"未找到关于 '{query}' 的相关信息"
        else:
            search_result = f"未找到关于 '{query}' 的相关信息"
        
        knowledge_logger.info(f'[BAIDU_SEARCH_TOOL] 搜索成功 | query: {query[:20]} | model: {model or baidu_model}')
        return f"{{\"type\": \"baidu_search_result\", \"query\": \"{query}\", \"result\": \"{search_result}\"}}"
        
    except requests.exceptions.Timeout:
        error_msg = "百度搜索请求超时"
        knowledge_logger.error(f'[BAIDU_SEARCH_TOOL_ERROR] {error_msg} | query: {query[:20]}')
        return f"{{\"error\": \"{error_msg}\", \"query\": \"{query}\"}}"
    except Exception as e:
        error_msg = f"百度AI搜索失败: {str(e)[:100]}"
        knowledge_logger.error(f'[BAIDU_SEARCH_TOOL_ERROR] {error_msg} | query: {query[:20]}')
        return f"{{\"error\": \"{error_msg}\", \"query\": \"{query}\"}}"

available_baidu_tools = [
    baidu_search_tool
]