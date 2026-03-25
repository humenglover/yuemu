import os
from datetime import datetime
from typing import List, Dict, Any
from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import PromptTemplate
import sys
import json

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from model.factory import create_chat_model
from utils.log_utils import rag_logger
from utils.file_utils import log_event

class RAGSummarizer:
    def __init__(self, vector_store):
        self.chat_model = create_chat_model()
        self.vector_store = vector_store
        self.retriever = vector_store.get_retriever()
        self.prompt_template = self.load_prompt_template()
        self.prompt = PromptTemplate.from_template(self.prompt_template)
        self.chain = self._init_chain()
    
    def _init_chain(self):
        chain = self.prompt | self.chat_model | StrOutputParser()
        return chain
    
    def load_prompt_template(self):
        """加载提示词模板"""
        prompt_path = os.path.join(os.path.dirname(__file__), '..', 'prompts', 'rag_summarize_prompt.txt')
        with open(prompt_path, 'r', encoding='utf-8') as f:
            return f.read().strip()
    
    def retrieve_docs(self, query: str) -> List[Document]:
        """检索文档"""
        return self.retriever.invoke(query)
    
    def summarize_with_rag(self, 
                          question: str, 
                          history: List[Dict[str, str]] = None,
                          long_term_memory: str = "") -> str:
        """使用RAG进行摘要"""
        try:
            # 记录AI回答开始事件
            safe_question = question[:50]
            history_len = len(history or [])
            rag_logger.info('[AI_ANSWER_START] 开始AI回答 | question: ' + safe_question + '... | history_length: ' + str(history_len))
            
            # 检索相关文档
            context_docs = self.retrieve_docs(question)
            
            # 构建上下文字符串
            context = self.build_context_str(context_docs, long_term_memory)
            
            # 构建历史对话字符串
            history_str = self.build_history_str(history or [])
            
            # 调用链
            result = self.chain.invoke({
                "input": question,
                "context": context,
                "history": history_str
            })
            
            # 记录AI回答完成事件
            rag_logger.info('[AI_ANSWER_COMPLETE] AI回答完成 | question: ' + safe_question + '... | answer_length: ' + str(len(result)))
            
            return result
                
        except Exception as e:
            err_info = str(e)
            print("RAG摘要过程中出现错误: " + err_info)
            # 记录AI回答错误事件
            safe_question = question[:50]
            rag_logger.error('[AI_ANSWER_ERROR] AI回答过程中出现错误 | question: ' + safe_question + '... | error: ' + err_info)
            return "抱歉，AI服务暂时不可用，请稍后再试。错误详情: " + err_info
    
    def direct_summarize(self, text: str, prompt_template: str = None) -> str:
        """直接对文本进行摘要，不使用 RAG 检索"""
        try:
            if not text:
                rag_logger.warning('[DIRECT_SUMMARIZE] 输入文本为空')
                return ""
            
            # 使用简单的摘要模板，避免知识库干扰
            if not prompt_template:
                prompt_template = "请简要总结以下对话内容（100字以内）：\n\n{text}\n\n回答："
            
            summary_prompt = PromptTemplate.from_template(prompt_template)
            
            # 使用较调皮的日志记录输入前 50 个字符
            rag_logger.info('[DIRECT_SUMMARIZE_START] 开始摘要 | 文本预览: ' + text[:50].replace('\n', ' ') + '...')
            
            # 尝试直接调用模型获取原始输出
            raw_response = self.chat_model.invoke(summary_prompt.format(text=text))
            
            # 获取解析后的文本
            result = ""
            if hasattr(raw_response, 'content'):
                result = raw_response.content
            else:
                result = str(raw_response)
            
            result = result.strip()
            
            if not result:
                rag_logger.warning('[DIRECT_SUMMARIZE_EMPTY] 模型返回了空字符串 | 原始响应: ' + str(raw_response))
            else:
                rag_logger.info('[DIRECT_SUMMARIZE_COMPLETE] 摘要完成 | 摘要长度: ' + str(len(result)) + ' | 摘要预览: ' + result[:30] + '...')
            
            return result
        except Exception as e:
            rag_logger.error('[DIRECT_SUMMARIZE_ERROR] 摘要失败: ' + str(e))
            import traceback
            rag_logger.error(traceback.format_exc())
            return ""

    def build_context_str(self, context_docs: List[Document], long_term_memory: str = "") -> str:
        """构建上下文字符串"""
        context_parts = []
        if long_term_memory:
            context_parts.append("【历史相关记忆】\n" + long_term_memory)
        
        if context_docs:
            context_parts.append("【参考知识库】")
        for i, doc in enumerate(context_docs, 1):
            content = doc.page_content if hasattr(doc, 'page_content') else str(doc)
            source = doc.metadata.get('source', '未知来源') if hasattr(doc, 'metadata') else '未知来源'
            # 纯字符串拼接，删除f-string
            context_parts.append(str(i) + ". 来源: " + source + " | 内容: " + content)
        
        return "\n".join(context_parts)
    
    def build_history_str(self, history: List[Dict[str, str]]) -> str:
        """构建历史对话字符串"""
        if not history:
            return ""
        
        history_parts = ["【会话历史】"]
        for item in history:
            role = "用户" if item.get("role") == "user" else "客服"
            content = item.get("content", "")
            # 纯字符串拼接，删除f-string
            history_parts.append(role + "：" + content)
        
        return "\n".join(history_parts)
    
    def stream_summarize_with_rag(self, 
                                 question: str, 
                                 history: List[Dict[str, str]] = None,
                                 long_term_memory: str = ""):
        """流式使用RAG进行摘要"""
        try:
            # 记录流式AI回答开始事件
            safe_question = question[:50]
            history_len = len(history or [])
            rag_logger.info('[STREAM_AI_ANSWER_START] 开始流式AI回答 | question: ' + safe_question + '... | history_length: ' + str(history_len))
            
            # 检索相关文档
            context_docs = self.retrieve_docs(question)
            
            # 构建上下文字符串
            context = self.build_context_str(context_docs, long_term_memory)
            
            # 构建历史对话字符串
            history_str = self.build_history_str(history or [])
            
            # 流式调用链
            for chunk in self.chain.stream({
                "input": question,
                "context": context,
                "history": history_str
            }):
                yield chunk
                    
        except Exception as e:
            err_info = str(e)
            print("RAG流式摘要过程中出现错误: " + err_info)
            # 记录流式AI回答错误事件
            safe_question = question[:50]
            rag_logger.error('[STREAM_AI_ANSWER_ERROR] 流式AI回答过程中出现错误 | question: ' + safe_question + '... | error: ' + err_info)
            yield "抱歉，AI流式服务暂时不可用，请稍后再试。错误详情: " + err_info
    
    async def astream_summarize_with_rag(self, 
                                        question: str, 
                                        history: List[Dict[str, str]] = None):
        """异步流式使用RAG进行摘要"""
        try:
            # 检索相关文档
            context_docs = self.retrieve_docs(question)
            
            # 构建上下文字符串
            context = self.build_context_str(context_docs)
            
            # 构建历史对话字符串
            history_str = self.build_history_str(history or [])
            
            # 异步流式调用链
            async for chunk in self.chain.astream({
                "input": question,
                "context": context,
                "history": history_str
            }):
                yield chunk
                    
        except Exception as e:
            err_info = str(e)
            print("RAG异步流式摘要过程中出现错误: " + err_info)
            yield "抱歉，AI异步流式服务暂时不可用，请稍后再试。错误详情: " + err_info