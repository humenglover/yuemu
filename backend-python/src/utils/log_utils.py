import os
import sys
import logging
from datetime import datetime
from pathlib import Path
from logging.handlers import TimedRotatingFileHandler


# 强制刷新输出缓冲区
def force_print(*args, **kwargs):
    """
    强制打印到控制台，绕过输出缓冲
    """
    print(*args, **kwargs)
    sys.stdout.flush()  # 强制刷新输出缓冲区
    sys.stderr.flush()  # 同时刷新错误输出缓冲区


def setup_logger(name: str, log_dir: str = "./logs"):
    """
    设置日志记录器，使用时间轮转文件处理器
    """
    # 创建日志目录
    Path(log_dir).mkdir(parents=True, exist_ok=True)

    # 创建logger
    logger = logging.getLogger(name)
    logger.setLevel(logging.DEBUG)  # 改为DEBUG级别，确保所有日志都能输出
    logger.propagate = False  # 防止日志重复输出

    # 清除现有的处理器，避免重复
    logger.handlers.clear()

    # 创建按天轮转的文件处理器
    log_file = os.path.join(log_dir, f"{name}.log")

    # 使用时间轮转处理器，每天创建新文件
    file_handler = TimedRotatingFileHandler(
        log_file,
        when="midnight",
        interval=1,
        backupCount=30,  # 保留30天的日志
        encoding='utf-8'
    )
    file_handler.setLevel(logging.DEBUG)

    # 创建格式器
    file_formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    file_handler.setFormatter(file_formatter)

    # 添加文件处理器
    logger.addHandler(file_handler)

    # 创建控制台处理器（关键修复：使用自定义流处理器）
    console_handler = logging.StreamHandler(sys.stdout)  # 明确指定输出到stdout
    console_handler.setLevel(logging.DEBUG)  # 控制台输出DEBUG级别
    console_formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    console_handler.setFormatter(console_formatter)

    # 添加控制台处理器
    logger.addHandler(console_handler)

    return logger


# 创建不同用途的日志记录器
app_logger = setup_logger("app", "./logs")
rag_logger = setup_logger("rag", "./logs")
upload_logger = setup_logger("upload", "./logs")
knowledge_logger = setup_logger("knowledge", "./logs")

# 导出强制打印函数，供其他模块使用
__all__ = ['app_logger', 'rag_logger', 'upload_logger', 'knowledge_logger', 'force_print']