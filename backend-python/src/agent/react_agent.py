from langchain.agents import create_agent
from model.factory import create_chat_model
from utils.log_utils import knowledge_logger
from .tools import available_tools
import json
import os

class ReactAgent:
    def __init__(self):
        # 创建聊天模型
        self.chat_model = create_chat_model()
            
        # 从外部文件加载系统提示词
        prompt_file_path = os.path.join(os.path.dirname(__file__), '..', 'prompts', 'agent_system_prompt.txt')
        with open(prompt_file_path, 'r', encoding='utf-8') as f:
            system_prompt = f.read().strip()
            
        # 创建Agent（保留工具列表+业务范围判断逻辑）
        self.agent = create_agent(
            model=self.chat_model,
            tools=available_tools,
            system_prompt=system_prompt,
        )
            
        knowledge_logger.info('[AGENT_INIT] ReactAgent初始化完成')

    def execute(self, query: str):
        """执行查询并返回结果"""
        try:
            knowledge_logger.info(f'[AGENT_EXECUTE] 开始执行查询 | query: {query}')
            
            input_dict = {
                "messages": [
                    {"role": "user", "content": query},
                ]
            }
            
            # 执行查询
            result = self.agent.invoke(input_dict)
            
            knowledge_logger.info(f'[AGENT_EXECUTE_SUCCESS] 查询执行成功 | query: {query}')
            
            # 增强型结果解析
            if isinstance(result, dict):
                output = result.get("output")
                if output is not None:
                    return str(output).strip()
                elif "messages" in result and result["messages"]:
                    last_message = result["messages"][-1]
                    if hasattr(last_message, 'content'):
                        return str(last_message.content).strip()
                    elif isinstance(last_message, dict) and "content" in last_message:
                        return str(last_message["content"]).strip()
                return str(result).strip()
            else:
                return str(result).strip()
        except Exception as e:
            error_msg = f"代理执行失败: {str(e)}"
            knowledge_logger.error(f'[AGENT_EXECUTE_ERROR] {error_msg}')
            return f"抱歉，处理您的请求时发生错误：{error_msg}"

    def execute_stream(self, query: str):
        """流式执行查询并返回结果"""
        try:
            knowledge_logger.info(f'[AGENT_EXECUTE_STREAM] 开始流式执行查询 | query: {query}')
            
            input_dict = {
                "messages": [
                    {"role": "user", "content": query},
                ]
            }
            
            # 使用LangChain的流式功能
            for chunk in self.agent.stream(input_dict, stream_mode="values"):
                latest_message = chunk["messages"][-1]
                # 提取消息内容
                content = None
                if hasattr(latest_message, 'content'):
                    content = latest_message.content
                elif isinstance(latest_message, dict) and "content" in latest_message:
                    content = latest_message["content"]
                
                if content:
                    # 过滤工具调用中间步骤
                    content_str = str(content).strip()
                    if content_str and not content_str.startswith(('{', 'Action:', 'Observation:', 'Thought:')):
                        yield content_str + "\n"
        except Exception as e:
            error_msg = f"流式代理执行失败: {str(e)}"
            knowledge_logger.error(f'[AGENT_EXECUTE_STREAM_ERROR] {error_msg}')
            yield f"抱歉，流式处理您的请求时发生错误：{error_msg}"