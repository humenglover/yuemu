from typing import List, Optional
from pydantic import BaseModel
import hashlib
import os


class KnowledgeFileDTO(BaseModel):
    """知识库文件数据传输对象"""
    id: Optional[int] = None
    original_name: str  # 原始文件名
    stored_name: str    # 存储文件名
    file_url: str       # 文件访问URL
    file_size: int      # 文件大小
    file_type: str      # 文件类型
    upload_time: Optional[str] = None  # 上传时间
    user_id: Optional[int] = None      # 上传用户ID
    status: int = 1     # 状态：1-正常，0-已删除
    md5_hash: Optional[str] = None    # MD5哈希值
    vector_count: int = 0  # 向量数量

    @staticmethod
    def calculate_md5(file_path: str) -> str:
        """计算文件MD5"""
        hash_md5 = hashlib.md5()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()

    @classmethod
    def from_file(cls, file_path: str, original_name: str = "", user_id: int = None):
        """从文件路径创建DTO"""
        file_size = os.path.getsize(file_path)
        file_ext = os.path.splitext(file_path)[1][1:]  # 去掉点号的扩展名
        md5_hash = cls.calculate_md5(file_path)
        
        return cls(
            original_name=original_name or os.path.basename(file_path),
            stored_name=os.path.basename(file_path),
            file_url=f"/api/knowledge/files/{os.path.basename(file_path)}",
            file_size=file_size,
            file_type=file_ext,
            user_id=user_id,
            md5_hash=md5_hash
        )


class UploadKnowledgeFileRequest(BaseModel):
    """上传知识库文件请求"""
    user_id: int
    file_type: str


class DeleteKnowledgeFileRequest(BaseModel):
    """删除知识库文件请求"""
    filename: str


class BatchDeleteKnowledgeFilesRequest(BaseModel):
    """批量删除知识库文件请求"""
    filenames: List[str]


class KnowledgeFileListRequest(BaseModel):
    """知识库文件列表请求"""
    file_type: Optional[str] = None
    page: int = 1
    page_size: int = 10