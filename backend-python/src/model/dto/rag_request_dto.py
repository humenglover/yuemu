from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class RAGRequestDTO(BaseModel):
    """
    RAG请求数据传输对象
    """
    history: Optional[List[Dict[str, str]]] = []
    question: str
    session_id: Optional[str] = None
    top_k: Optional[int] = 8
    temperature: Optional[float] = 0.7
    long_term_memory: Optional[str] = ""


class StreamRAGRequestDTO(BaseModel):
    """
    RAG流式请求数据传输对象
    """
    history: Optional[List[Dict[str, str]]] = []
    question: str
    session_id: Optional[str] = None
    top_k: Optional[int] = 8
    temperature: Optional[float] = 0.7
    long_term_memory: Optional[str] = ""


class KnowledgeUploadRequestDTO(BaseModel):
    """
    知识库上传请求数据传输对象
    """
    filename: str
    content_type: str
    size: int