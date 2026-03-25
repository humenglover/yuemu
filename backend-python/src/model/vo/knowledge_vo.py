from pydantic import BaseModel
from typing import Optional


class KnowledgeUploadVO(BaseModel):
    """
    知识库上传结果值对象
    """
    filename: str
    file_path: str
    documents_count: Optional[int] = 0
    md5: Optional[str] = ""


class KnowledgeFileInfoVO(BaseModel):
    """
    知识库文件信息值对象
    """
    filename: str
    file_path: str
    size: Optional[int] = 0
    content_type: Optional[str] = ""
    md5: Optional[str] = ""