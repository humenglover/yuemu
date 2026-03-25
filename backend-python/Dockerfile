# 基础镜像：Python 3.10 轻量化版本
FROM python:3.10-slim

# 解决时区问题
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置工作目录
WORKDIR /app

# 复制依赖文件
COPY requirements.txt .

# 安装系统依赖（解决PyPDF2等库的编译问题）
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libc6-dev \
    && rm -rf /var/lib/apt/lists/*

# 升级pip（关键：避免旧版pip解析依赖出错）
RUN pip install --upgrade pip

# 安装Python依赖（清华源加速，避免下载慢）
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple --prefer-binary -r requirements.txt

# 关键修复：完整复制src目录（包含config子目录）到容器的/app/src/
COPY src/ /app/src/

# 暴露端口
EXPOSE 8001

# 启动命令：先进入src目录，再执行main.py（确保相对路径正确）
CMD ["sh", "-c", "cd /app/src && python main.py"]