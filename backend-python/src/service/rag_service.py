from typing import List, Dict, Any, Optional, Generator
from datetime import datetime
import time
import sys
import os
import re

# 添加src目录到路径，以便导入模块
sys.path.append(os.path.join(os.path.dirname(os.path.dirname(__file__))))

from model.dto.rag_request_dto import RAGRequestDTO, StreamRAGRequestDTO
from model.vo.rag_vo import RAGResultVO, StreamChunkVO, StreamEndVO
from model.common.response_wrapper import ResponseWrapper
from model.common.constants import RAGConstants
from utils.log_utils import app_logger, knowledge_logger
from utils.file_utils import log_event, calculate_md5, check_md5_exists_only, save_md5_with_filename, find_filename_by_md5, get_file_documents
from rag.rag_summarize import RAGSummarizer
from agent.react_agent import ReactAgent


class RAGService:
    """
    RAG服务类 - 处理RAG相关的业务逻辑
    """
    
    def __init__(self, rag_summarizer: RAGSummarizer, agent: Optional[ReactAgent] = None):
        """
        初始化RAG服务
        
        Args:
            rag_summarizer: RAG总结器实例
            agent: Agent实例（可选）
        """
        self.rag_summarizer = rag_summarizer
        self.agent = agent
    
    def should_use_agent(self, question: str, rag_response: Optional[str] = None) -> bool:
        """
        智能判断是否启用Agent处理请求
        判定维度：
        1. RAG响应质量（长度、有效性）
        2. 问题是否包含实时/联网特征
        3. 问题是否需要复杂推理
        4. 问题是否包含中文字符（仅在实时信息场景下优先使用Agent）
        """
        # 空问题直接返回False
        if not question or not question.strip():
            return False

        # 检查是否包含中文字符
        has_chinese = bool(re.search(r'[\u4e00-\u9fff]', question))
        
        # 维度1：RAG响应质量判断
        if rag_response:
            rag_response_clean = rag_response.strip()
            # 响应过短（无有效信息）
            if len(rag_response_clean) < 30:
                return True
            # 响应包含无结果提示
            empty_responses = [
                "无法找到相关信息", "未找到相关内容", "知识库中没有", 
                "不知道", "无相关信息", "暂无答案", "抱歉"
            ]
            if any(marker in rag_response_clean for marker in empty_responses):
                return True

        # 维度2：实时/联网特征判断（这才是需要使用Agent的关键场景）
        question_lower = question.lower()
        if any(keyword in question_lower for keyword in RAGConstants.REAL_TIME_INDICATORS):
            # 对于实时信息查询，如果是中文则优先使用Agent（百度搜索）
            if has_chinese:
                return True
            # 对于非中文的实时信息查询也使用Agent
            else:
                return True

        # 维度3：URL/网络资源特征
        if any(pattern in question for pattern in RAGConstants.URL_PATTERNS):
            return True

        # 维度4：复杂推理特征
        if any(keyword in question_lower for keyword in RAGConstants.REASONING_KEYWORDS):
            return True

        # 注意：这里不把所有中文查询都导向Agent
        # 只有在需要实时信息或网络搜索的情况下才使用Agent
        # 对于普通知识查询，优先使用RAG

        return False
    
    def clean_agent_response(self, raw_response: str, question: str) -> str:
        """
        清理Agent响应内容，移除冗余信息，特别是处理多工具结果的整合
        """
        if not raw_response or not question:
            return raw_response or ""
        
        clean_response = raw_response.strip()
        
        # 如果响应包含多个工具的结果（例如JSON格式或其他结构化数据），尝试提取主要内容
        # 移除常见的工具结果标记
        import re
        # 移除JSON结构标记
        clean_response = re.sub(r'\{[^{}]*\}', '', clean_response)
        # 移除工具类型标记
        clean_response = re.sub(r'【[^】]*】:', '', clean_response)
        clean_response = re.sub(r'工具结果[:：]', '', clean_response)
        clean_response = re.sub(r'搜索结果[:：]', '', clean_response)
        clean_response = re.sub(r'时间结果[:：]', '', clean_response)
        clean_response = re.sub(r'知识库信息[:：]', '', clean_response)
        
        # 移除原始问题重复内容
        question_clean = question.strip()
        if question_clean in clean_response and clean_response != question_clean:
            clean_response = clean_response.replace(question_clean, "").strip()
        
        # 移除多余分隔符
        clean_response = clean_response.lstrip('：:！？。,，').strip()
        
        # 如果清理后内容过多，可能是包含了工具的原始输出，尝试提取主要回答部分
        lines = clean_response.split('\n')
        filtered_lines = []
        for line in lines:
            line = line.strip()
            # 过滤掉明显是工具输出标识的行
            if not any(skip_marker in line.lower() for skip_marker in ['type', 'query', 'result', 'error']):
                if line:  # 只保留非空行
                    filtered_lines.append(line)
        
        clean_response = '\n'.join(filtered_lines).strip()
        
        # 检查是否clean_response包含有意义的内容（不是默认提示），如果是，则不需要兜底
        if clean_response and "已为您分析相关信息，但暂未获取到有效结果" not in clean_response:
            # 如果clean_response中有实际内容，直接返回
            return clean_response
        elif not clean_response or clean_response == "已为您分析相关信息，但暂未获取到有效结果":
            # 如果确实是空的或者只有默认提示，才返回默认提示
            return "已为您分析相关信息，但暂未获取到有效结果"
        else:
            # 如果clean_response有内容但不是默认提示，返回该内容
            return clean_response
    
    def process_rag_sync(self, request: RAGRequestDTO) -> ResponseWrapper:
        """
        处理同步RAG请求
        
        Args:
            request: RAG请求DTO
            
        Returns:
            ResponseWrapper: 响应包装器
        """
        # 基础参数初始化
        session_id = request.session_id or f"session_{int(time.time())}"
        safe_question = (request.question[:50] + "...") if request.question else ""
        
        # 参数验证
        if not request.question or not request.question.strip():
            app_logger.warning(f"无效请求：问题为空 | session_id: {session_id}")
            return ResponseWrapper.bad_request(
                msg="问题不能为空",
                data={"answer": "", "session_id": session_id, "error_detail": "问题不能为空"}
            )

        try:
            # 记录请求开始
            app_logger.info(
                f"开始处理同步RAG请求 | session_id: {session_id} | question: {safe_question} "
                f"| history_length: {len(request.history)} | top_k: {request.top_k}"
            )

            # 第一步：RAG基础回答
            rag_answer = self.rag_summarizer.summarize_with_rag(
                question=request.question,
                history=request.history,
                long_term_memory=request.long_term_memory
            )

            # 第二步：智能判断是否启用Agent
            final_answer = rag_answer
            if self.agent and self.should_use_agent(request.question, rag_answer):
                # Agent处理并清理响应
                raw_agent_answer = self.agent.execute(request.question)
                final_answer = self.clean_agent_response(raw_agent_answer, request.question)
                # 如果清理后的答案是默认提示，则忽略它
                if final_answer == "已为您分析相关信息，但暂未获取到有效结果" and raw_agent_answer and raw_agent_answer != final_answer:
                    # 如果原始答案不为空，但清理后变成了默认提示，说明清理过程中丢失了有用信息
                    # 尝试使用原始答案的清理版本，但去除默认提示
                    import re
                    # 从原始答案中移除默认提示
                    final_answer = raw_agent_answer.replace("已为您分析相关信息，但暂未获取到有效结果", "").strip()
                    if not final_answer:
                        final_answer = "已为您分析相关信息，但暂未获取到有效结果"
                # 确保最终答案不是用户原始问题
                if final_answer.strip() == request.question.strip():
                    final_answer = "已为您分析相关信息，但暂未获取到有效结果"
                app_logger.info(f"Agent已介入处理 | session_id: {session_id} | question: {safe_question}")
            else:
                app_logger.info(f"仅使用RAG处理 | session_id: {session_id} | question: {safe_question}")

            # 构建响应结果
            result_vo = RAGResultVO(
                answer=final_answer,
                session_id=session_id,
                top_k=request.top_k,
                temperature=request.temperature
            )

            # 记录请求完成
            app_logger.info(
                f"同步RAG请求处理完成 | session_id: {session_id} | question: {safe_question} "
                f"| answer_length: {len(final_answer)}"
            )

            return ResponseWrapper.success(data=result_vo.model_dump())

        except Exception as e:
            err_info = str(e)
            app_logger.error(
                f"同步RAG请求处理失败 | session_id: {session_id} | question: {safe_question} "
                f"| error: {err_info}"
            )
            error_data = {
                "answer": "",
                "session_id": session_id,
                "error_detail": f"服务处理异常：{err_info}"
            }
            return ResponseWrapper.bad_request(msg="服务异常，请稍后再试", data=error_data)
    
    def process_summarize(self, request: RAGRequestDTO) -> ResponseWrapper:
        """
        专门处理摘要生成请求（非 RAG 问答）
        """
        session_id = request.session_id or f"sum_{int(time.time())}"
        try:
            # question 字段在此接口中用作待总结的原始文本
            summary = self.rag_summarizer.direct_summarize(request.question)
            
            result_vo = RAGResultVO(
                answer=summary,
                session_id=session_id
            )
            return ResponseWrapper.success(data=result_vo.model_dump())
        except Exception as e:
            app_logger.error(f"摘要业务处理失败: {str(e)}")
            return ResponseWrapper.bad_request(msg="摘要处理异常")

    def process_rag_stream(self, request: StreamRAGRequestDTO) -> Generator[str, None, None]:
        """
        处理流式RAG请求
        
        Args:
            request: 流式RAG请求DTO
            
        Yields:
            str: SSE格式的流式响应数据
        """
        import json
        
        # 基础参数初始化
        session_id = request.session_id or f"session_{int(time.time())}"
        safe_question = (request.question[:50] + "...") if request.question else ""

        # 参数验证
        if not request.question or not request.question.strip():
            log_event('STREAM_QUESTION_INVALID', '无效流式请求：问题为空', {
                'session_id': session_id,
                'timestamp': datetime.now().isoformat()
            })
            error_data = {
                "answer": "",
                "session_id": session_id,
                "error_detail": "问题不能为空"
            }
            return ResponseWrapper.bad_request(msg="问题不能为空", data=error_data).model_dump()

        # 记录流式请求开始
        log_event('STREAM_REQUEST_START', '开始处理流式RAG请求', {
            'question': request.question,
            'session_id': session_id,
            'history_length': len(request.history),
            'top_k': request.top_k,
            'timestamp': datetime.now().isoformat()
        })

        full_answer = ""
        try:
            # 发送开始事件
            start_data = {
                "event": "start",
                "data": {
                    "code": 200,
                    "msg": "开始流式传输",
                    "data": {
                        "session_id": session_id,
                        "top_k": request.top_k,
                        "temperature": request.temperature
                    }
                }
            }
            yield f"data: {json.dumps(start_data)}\n\n"

            # 判断是否启用Agent流式处理
            use_agent_flag = self.agent and self.should_use_agent(request.question)
            if use_agent_flag:
                # Agent流式处理
                full_answer = ""  # 重置full_answer
                for chunk in self.agent.execute_stream(request.question):
                    if chunk:
                        # 清理每个chunk，防止工具原始输出泄露
                        cleaned_chunk = self.clean_agent_response(chunk, request.question)
                        # 检查是否是用户原始问题的重复（避免返回用户自己的问题）
                        if (cleaned_chunk and 
                            cleaned_chunk != "已为您分析相关信息，但暂未获取到有效结果" and
                            cleaned_chunk.strip() != request.question.strip()):  # 避免返回用户原始问题
                            full_answer += cleaned_chunk
                            chunk_data = {
                                "event": "chunk",
                                "data": {
                                    "code": 200,
                                    "msg": "流式数据块",
                                    "data": {
                                        "chunk": cleaned_chunk,
                                        "full_answer": full_answer,
                                        "session_id": session_id
                                    }
                                }
                            }
                            yield f"data: {json.dumps(chunk_data)}\n\n"
                app_logger.info(f"Agent流式处理完成 | session_id: {session_id} | question: {safe_question}")
            else:
                # RAG流式处理
                for chunk in self.rag_summarizer.stream_summarize_with_rag(
                    question=request.question,
                    history=request.history,
                    long_term_memory=request.long_term_memory
                ):
                    if chunk:
                        full_answer += chunk
                        chunk_data = {
                            "event": "chunk",
                            "data": {
                                "code": 200,
                                "msg": "流式数据块",
                                "data": {
                                    "chunk": chunk,
                                    "full_answer": full_answer,
                                    "session_id": session_id
                                }
                            }
                        }
                        yield f"data: {json.dumps(chunk_data)}\n\n"
                app_logger.info(f"RAG流式处理完成 | session_id: {session_id} | question: {safe_question}")

            # 发送结束事件
            end_data = {
                "event": "end",
                "data": {
                    "code": 200,
                    "msg": "流式传输完成",
                    "data": {
                        "answer": full_answer,
                        "session_id": session_id,
                        "answer_length": len(full_answer)
                    }
                }
            }
            yield f"data: {json.dumps(end_data)}\n\n"

        except Exception as e:
            err_info = str(e)
            app_logger.error(
                f"流式RAG请求处理失败 | session_id: {session_id} | question: {safe_question} "
                f"| error: {err_info}"
            )
            error_data = {
                "event": "error",
                "data": {
                    "code": 500,
                    "msg": "服务异常，请稍后再试",
                    "data": {
                        "answer": "",
                        "session_id": session_id,
                        "error_detail": f"流式处理异常：{err_info}"
                    }
                }
            }
            yield f"data: {json.dumps(error_data)}\n\n"