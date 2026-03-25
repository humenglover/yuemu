from langchain_core.callbacks import BaseCallbackHandler
from utils.log_utils import knowledge_logger
from typing import Any, Dict

class ToolMonitorCallbackHandler(BaseCallbackHandler):
    """工具监控回调处理器，用于记录工具调用和结果"""
    
    def on_tool_start(self, serialized: Dict[str, Any], input_str: str, *, run_id,
                      parent_run_id=None, tags=None, metadata=None, **kwargs: Any) -> None:
        """工具开始执行时的回调"""
        tool_name = serialized.get("name", "Unknown Tool")
        tool_args = serialized.get("args", {})
        knowledge_logger.info(f'[AGENT_TOOL_START] 工具调用开始 | tool: {tool_name} | args: {tool_args}')
    
    def on_tool_end(self, output: Any, *, run_id, parent_run_id=None, tags=None, **kwargs: Any) -> None:
        """工具执行结束时的回调"""
        # 获取工具名称
        serialized = kwargs.get('serialized', {})
        tool_name = serialized.get("name", "Unknown Tool")
        knowledge_logger.info(f'[AGENT_TOOL_END] 工具调用结束 | tool: {tool_name} | output_length: {len(str(output))} | output_preview: {str(output)[:200]}')

# 可用的监控回调处理器
available_monitor_callbacks = [
    ToolMonitorCallbackHandler
]