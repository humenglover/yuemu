try:
    from langchain_community.chat_models.tongyi import ChatTongyi
except ImportError:
    from langchain_community.chat_models import ChatTongyi
from langchain_community.embeddings import DashScopeEmbeddings
import yaml
import os
from utils.log_utils import app_logger
from utils.file_utils import log_event

def load_config():
    """加载配置文件"""
    import os
    from utils.log_utils import app_logger
    
    # 加载主配置文件
    rag_config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'rag.yml')
    with open(rag_config_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)
    
    # 加载工具配置文件
    tool_config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'tool.yml')
    if os.path.exists(tool_config_path):
        with open(tool_config_path, 'r', encoding='utf-8') as f:
            tool_config = yaml.safe_load(f)
            # 将工具配置合并到主配置中
            config.update(tool_config)
    else:
        app_logger.warning('[CONFIG_LOAD] 工具配置文件不存在: ' + tool_config_path)
    
    # 加载并发配置文件
    concurrency_config_path = os.path.join(os.path.dirname(__file__), '..', 'config', 'concurrency.yml')
    if os.path.exists(concurrency_config_path):
        with open(concurrency_config_path, 'r', encoding='utf-8') as f:
            concurrency_config = yaml.safe_load(f)
            # 将并发配置合并到主配置中
            config.update(concurrency_config)
    else:
        app_logger.warning('[CONFIG_LOAD] 并发配置文件不存在: ' + concurrency_config_path)
        # 设置默认并发配置
        config['concurrency'] = {
            'thread_pool': {'max_workers': 20},
            'request_processing': {'max_concurrent_requests': 50, 'timeout_seconds': 300},
            'agent': {'max_concurrent_agents': 10, 'agent_timeout_seconds': 120}
        }
    
    return config

def create_chat_model():
    """创建千问聊天模型"""
    config = load_config()
    model_name = config['chat_model_name']
    temperature = config.get('qwen_temperature', 0.7)
    max_tokens = config.get('qwen_max_tokens', 2048)
    app_logger.info('[MODEL_CREATION] 创建聊天模型 | model_name: ' + model_name + ' | temperature: ' + str(temperature) + ' | max_tokens: ' + str(max_tokens))
    
    model = ChatTongyi(
        model=config['chat_model_name'],
        dashscope_api_key=config['qwen_api_key'],
        temperature=config.get('qwen_temperature', 0.7),
        max_tokens=config.get('qwen_max_tokens', 2048)
    )
    
    app_logger.info('[MODEL_CREATED] 聊天模型创建成功 | model_name: ' + model_name)
    return model

def create_embedding_model():
    """创建嵌入模型"""
    config = load_config()
    model_name = config['embedding_model_name']
    app_logger.info('[EMBEDDING_MODEL_CREATION] 创建嵌入模型 | model_name: ' + model_name)
    
    model = DashScopeEmbeddings(
        model=config['embedding_model_name'],
        dashscope_api_key=config['qwen_api_key']
    )
    
    app_logger.info('[EMBEDDING_MODEL_CREATED] 嵌入模型创建成功 | model_name: ' + model_name)
    return model