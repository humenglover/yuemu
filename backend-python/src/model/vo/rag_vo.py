from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class RAGResultVO(BaseModel):
    """
    RAG结果值对象
    """
    answer: str
    session_id: Optional[str] = None
    top_k: Optional[int] = 8
    temperature: Optional[float] = 0.7


class KnowledgeUploadResultVO(BaseModel):
    """
    知识库上传结果值对象
    """
    filename: str
    file_path: str
    documents_count: Optional[int] = 0


class StreamChunkVO(BaseModel):
    """
    流式响应块值对象
    """
    chunk: str
    full_answer: str
    session_id: Optional[str] = None


class StreamEndVO(BaseModel):
    """
    流式响应结束值对象
    """
    answer: str
    session_id: Optional[str] = None


class HistoryItemVO(BaseModel):
    """
    历史记录项值对象
    """
    role: str
    content: str