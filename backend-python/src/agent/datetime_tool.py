import os
from langchain_core.tools import tool
from utils.log_utils import knowledge_logger

@tool(description="获取当前日期和时间")
def datetime_tool() -> str:
    """获取当前日期和时间"""
    from datetime import datetime
    current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    knowledge_logger.info(f'[DATETIME_TOOL] 获取当前时间 | time: {current_time}')
    return f'{{"type": "datetime_result", "result": "当前日期和时间: {current_time}"}}'

# 可用的时间工具列表
available_datetime_tools = [
    datetime_tool
]