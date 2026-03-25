from pydantic import BaseModel
from typing import Generic, TypeVar, Optional, Dict, Any
from enum import Enum


T = TypeVar('T')


class ResponseCode(Enum):
    """
    响应状态码枚举
    """
    SUCCESS = 200
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    INTERNAL_ERROR = 500


class ResponseWrapper(BaseModel, Generic[T]):
    """
    统一响应包装器
    """
    code: int
    msg: str
    data: Optional[T] = None

    @classmethod
    def success(cls, data: T = None, msg: str = "请求成功"):
        return cls(code=ResponseCode.SUCCESS.value, msg=msg, data=data)

    @classmethod
    def error(cls, code: int = ResponseCode.INTERNAL_ERROR.value, msg: str = "服务异常", data: T = None):
        return cls(code=code, msg=msg, data=data)

    @classmethod
    def bad_request(cls, msg: str = "请求参数错误", data: T = None):
        return cls(code=ResponseCode.BAD_REQUEST.value, msg=msg, data=data)


class SSEEventType(Enum):
    """
    SSE事件类型枚举
    """
    START = "start"
    CHUNK = "chunk"
    END = "end"
    ERROR = "error"


class SSEData(BaseModel):
    """
    SSE数据结构
    """
    event: str
    data: Dict[str, Any]