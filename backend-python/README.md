# 悦木 Python AI 服务

## 项目介绍
悦木图片管理系统的 Python AI 子服务，基于 FastAPI 构建。提供 RAG 智能客服问答、知识库管理、YOLO 目标检测、AI 图像处理（去背景、人脸模糊、换背景）等能力，作为 Java 后端的 AI 引擎层独立部署运行。

## 技术栈
- **Web 框架**：FastAPI + Uvicorn
- **大模型**：通义千问（阿里云 DashScope）
- **向量数据库**：DashScope Embeddings + 本地向量存储
- **RAG 框架**：LangChain（Prompt 模板、Chain、Agent）
- **目标检测**：YOLOv8（ONNX Runtime 推理）
- **图像处理**：rembg（去背景）、OpenCV（人脸检测/模糊）、MODNet（换背景）
- **并发处理**：ThreadPoolExecutor 线程池

## API 接口一览

### RAG 智能问答
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/rag/sync` | 同步问答（阻塞返回完整回答） |
| POST | `/api/rag/stream` | 流式问答（SSE 逐字输出） |
| POST | `/api/rag/summarize` | 专用摘要生成（绕过知识库约束，供超长记忆使用） |

### 知识库管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/knowledge/upload` | 上传知识库文件（TXT/PDF 等） |
| GET | `/api/knowledge/list` | 获取已上传的知识库文件列表 |
| POST | `/api/knowledge/delete` | 按 MD5 删除指定知识库文件 |
| POST | `/api/knowledge/clear-all` | 清空所有知识库文件 |
| GET | `/api/vector/verify/{file_md5}` | 验证向量元数据 |

### YOLO 目标检测
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/yolo/detect` | 上传图片进行目标检测 |
| GET | `/api/yolo/detect-url` | 通过 URL 进行目标检测 |

### AI 图像处理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/remove_bg` | 智能去除图片背景（返回透明 PNG） |
| POST | `/api/ai/face_blur` | 人脸自动打马赛克 |
| POST | `/api/ai/change_background` | 智能更换图片背景（纯色或自定义图片） |

## 项目结构
```
python-rag/src/
├── main.py                  # FastAPI 入口，所有路由定义
├── config/                  # 配置文件
│   ├── rag.yml              # RAG 核心配置（模型、API Key）
│   ├── tool.yml             # 工具配置
│   └── concurrency.yml      # 并发配置
├── model/                   # 数据模型
│   ├── factory.py           # 模型工厂（创建 LLM、Embedding）
│   ├── dto/                 # 请求/响应 DTO
│   └── common/              # 通用模型（ResponseWrapper 等）
├── rag/                     # RAG 核心逻辑
│   ├── rag_summarize.py     # RAG 摘要器（含 direct_summarize）
│   └── vector_store.py      # 向量存储管理
├── service/                 # 业务服务层
│   ├── rag_service.py       # RAG 问答服务
│   ├── knowledge_management_service.py  # 知识库管理服务
│   ├── yolo_service.py      # YOLO 检测服务
│   └── image_service.py     # AI 图像处理服务
├── agent/                   # ReAct Agent
│   └── react_agent.py       # 智能体（工具调用）
├── prompts/                 # Prompt 模板
├── models/                  # AI 模型文件（YOLO ONNX 等）
├── utils/                   # 工具类（日志、文件处理）
└── logs/                    # 运行日志
```

## 快速启动
```bash
cd python-rag/src
pip install -r requirements.txt
python main.py  # 默认 http://127.0.0.1:8001
```

## 配置说明
在 `config/rag.yml` 中填入：
```yaml
qwen_api_key: "你的通义千问 API Key"
chat_model_name: "qwen-plus"
embedding_model_name: "text-embedding-v2"
fastapi_host: "127.0.0.1"
fastapi_port: 8001
```

## 与 Java 后端的交互
Java 后端通过 HTTP 调用本服务的各接口，配置在 `application.yml` 中：
```yaml
rag:
  python-service:
    base-url: "http://127.0.0.1:8001"
    sync-endpoint: "/api/rag/sync"
    stream-endpoint: "/api/rag/stream"
    summarize-endpoint: "/api/rag/summarize"
```

## 联系方式
- 作者：鹿梦
- 邮箱：109484028@qq.com
