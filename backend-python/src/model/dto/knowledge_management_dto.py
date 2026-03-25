from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime


class KnowledgeFileDTO(BaseModel):
    """
    知识库文件数据传输对象
    """
    filename: str
    filepath: str
    size: int
    extension: str
    md5: str
    created_at: datetime
    updated_at: Optional[datetime] = None


class GetAllKnowledgeFilesResponseDTO(BaseModel):
    """
    获取所有知识库文件响应数据传输对象
    """
    code: int
    msg: str
    data: List[KnowledgeFileDTO]


class DeleteKnowledgeFileRequestDTO(BaseModel):
    """
    删除知识库文件请求数据传输对象
    """
    filename: str


class DeleteKnowledgeFileResponseDTO(BaseModel):
    """
    删除知识库文件响应数据传输对象
    """
    code: int
    msg: str
    data: Optional[dict] = {}


class SetMaxKnowledgeCountRequestDTO(BaseModel):
    """
    设置最大知识库数量请求数据传输对象
    """
    max_count: int


class SetMaxKnowledgeCountResponseDTO(BaseModel):
    """
    设置最大知识库数量响应数据传输对象
    """
    code: int
    msg: str
    data: Optional[dict] = {}