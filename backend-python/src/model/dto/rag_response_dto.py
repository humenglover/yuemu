from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class RAGResponseDTO(BaseModel):
    """
    RAG响应数据传输对象
    """
    code: int
    msg: str
    data: Dict[str, Any]


class KnowledgeUploadResponseDTO(BaseModel):
    """
    知识库上传响应数据传输对象
    """
    code: int
    msg: str
    data: Dict[str, Any]


class SSEEventDataDTO(BaseModel):
    """
    SSE事件数据传输对象
    """
    code: int
    msg: str
    data: Dict[str, Any]


class SSEDataDTO(BaseModel):
    """
    SSE数据传输对象
    """
    event: str
    data: SSEEventDataDTO