import os
from typing import List, Tuple
import yaml
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_text_splitters import RecursiveCharacterTextSplitter
import sys
import traceback

# 路径导入
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from model.factory import create_embedding_model
from utils.log_utils import knowledge_logger
from utils.file_utils import (
    calculate_md5,
    check_md5_exists_only,
    check_md5_exists_permanently,
    save_md5_with_filename,
    save_md5_permanently,
    get_file_documents
)


class VectorStoreManager:
    def __init__(self):
        self.config = self.load_chroma_config()
        self.embedding_model = create_embedding_model()

        # 路径初始化
        current_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(current_dir)
        self.persist_directory = os.path.join(project_root, self.config['persist_directory'].lstrip('./'))
        self.data_path = os.path.join(project_root, self.config['data_path'].lstrip('./'))

        # 初始化向量库和分词器
        self.vectorstore = Chroma(
            collection_name=self.config['collection_name'],
            embedding_function=self.embedding_model,
            persist_directory=self.persist_directory,
        )
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.config['chunk_size'],
            chunk_overlap=self.config['chunk_overlap'],
            separators=self.config['separators'],
            length_function=len,
        )

        # 确保目录存在
        os.makedirs(self.persist_directory, exist_ok=True)
        os.makedirs(self.data_path, exist_ok=True)

        # 加载知识库
        self.load_knowledge_base()

    def load_chroma_config(self):
        config_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'config', 'chroma.yml')
        with open(config_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)

    def get_retriever(self):
        return self.vectorstore.as_retriever(search_kwargs={"k": self.config['k']})

    def calculate_md5(self, file_path: str) -> str:
        return calculate_md5(file_path)

    def load_knowledge_base(self):
        data_path = self.data_path
        md5_hex_store_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                                          self.config['md5_hex_store'].lstrip('./'))
        os.makedirs(data_path, exist_ok=True)
        os.makedirs(os.path.dirname(md5_hex_store_path), exist_ok=True)

        # 确保MD5记录文件存在
        if not os.path.exists(md5_hex_store_path):
            with open(md5_hex_store_path, 'w', encoding='utf-8') as f:
                pass

        # 筛选允许的文件
        allowed_files = []
        allowed_exts = self.config['allow_knowledge_file_type']
        try:
            for file_name in os.listdir(data_path):
                file_path = os.path.join(data_path, file_name)
                if os.path.isfile(file_path):
                    file_ext = os.path.splitext(file_name)[1].lower()
                    if file_ext in allowed_exts:
                        allowed_files.append(file_path)
        except FileNotFoundError:
            knowledge_logger.warning('[知识库加载] 目录不存在，已自动创建：' + data_path)
            os.makedirs(data_path, exist_ok=True)

        # 日志输出扫描结果
        file_count = str(len(allowed_files))
        knowledge_logger.info(
            '[KNOWLEDGE_BASE_SCAN] 开始扫描知识库目录：' + data_path + ' | 找到可处理文件数：' + file_count)
        knowledge_logger.info('[知识库加载] 开始扫描知识库目录：' + data_path)
        knowledge_logger.info('[知识库加载] 找到 ' + file_count + ' 个允许的知识库文件：' + str(allowed_files))

        for path in allowed_files:
            knowledge_logger.info('[FILE_PROCESS_START] 开始处理文件：' + path)
            md5_hex = self.calculate_md5(path)

            # 文件已存在则跳过
            if check_md5_exists_only(md5_hex, md5_hex_store_path):
                knowledge_logger.info('[FILE_DUPLICATE_SKIP] 文件已存在，跳过：' + path + ' | MD5：' + md5_hex)
                continue

            try:
                # 加载文档
                knowledge_logger.info('[DOCUMENT_LOAD_START] 开始加载文档内容：' + path)
                documents: List[Document] = get_file_documents(path)

                if not documents:
                    knowledge_logger.warning('[DOCUMENT_EMPTY] 文档无有效内容，跳过：' + path)
                    continue

                doc_num = str(len(documents))
                knowledge_logger.info('[DOCUMENT_LOADED] 文档加载完成：' + path + ' | 原始片段数：' + doc_num)

                # 分片文档
                split_document: List[Document] = self.text_splitter.split_documents(documents)

                if not split_document:
                    knowledge_logger.warning('[DOCUMENT_SPLIT_EMPTY] 文档分片后为空，跳过：' + path)
                    continue

                split_num = str(len(split_document))
                knowledge_logger.info('[DOCUMENT_SPLIT] 文档分片完成：' + path + ' | 分片后片段数：' + split_num)

                # 修改文档元数据以包含MD5信息
                for doc in split_document:
                    if doc.metadata is None:
                        doc.metadata = {}
                    doc.metadata['file_md5'] = md5_hex
                    doc.metadata['source'] = path

                # 存入向量库
                self.vectorstore.add_documents(split_document)
                knowledge_logger.info('[VECTOR_STORE_ADD] 片段已添加到向量库：' + path + ' | 新增片段数：' + split_num)

                # 保存MD5记录
                save_md5_with_filename(md5_hex, os.path.basename(path), md5_hex_store_path)
                knowledge_logger.info('[MD5_SAVED] MD5已保存：' + path + ' | MD5：' + md5_hex)
                knowledge_logger.info('[FILE_PROCESS_SUCCESS] 文件处理成功：' + path + ' | 最终新增片段数：' + split_num)

            except Exception as e:
                err_info = '[FILE_PROCESS_ERROR] 文件处理失败：' + path + ' | 错误信息：' + str(e)
                knowledge_logger.error(err_info)
                traceback.print_exc()
                continue

        # 最终知识库加载完成日志
        total_docs = str(self.vectorstore._collection.count())
        knowledge_logger.info('[KNOWLEDGE_BASE_LOADED] 知识库加载完成 | 向量库总片段数：' + total_docs)

    def similarity_search(self, query: str, k: int = None) -> List[Document]:
        if k is None:
            k = self.config['k']
        return self.vectorstore.similarity_search(query, k=k)

    def similarity_search_with_score(self, query: str, k: int = None) -> List[Tuple[Document, float]]:
        if k is None:
            k = self.config['k']
        return self.vectorstore.similarity_search_with_score(query, k=k)

    def clear_all_vectors(self):
        try:
            # 获取所有文档ID
            collection = self.vectorstore._client.get_collection(name=self.config['collection_name'])
            all_ids = collection.get()['ids']

            if all_ids:
                # 删除所有文档
                collection.delete(ids=all_ids)
                knowledge_logger.info('[VECTOR_STORE_CLEAR] 成功清空向量库，删除了 ' + str(len(all_ids)) + ' 个向量')
            else:
                knowledge_logger.info('[VECTOR_STORE_CLEAR] 向量库已经是空的')

            return True
        except Exception as e:
            err_info = str(e)
            knowledge_logger.error('[VECTOR_STORE_CLEAR_ERROR] 清空向量库失败：' + err_info)
            return False

    def clear_file_vectors(self, file_md5: str):
        return self.clear_vectors_by_md5(file_md5)

    def clear_vectors_by_md5(self, file_md5: str):
        try:
            # 获取所有文档ID及其元数据
            collection = self.vectorstore._client.get_collection(name=self.config['collection_name'])
            results = collection.get(include=['metadatas', 'documents'])

            # 找到与特定MD5相关的文档ID
            ids_to_delete = []
            for i, metadata in enumerate(results['metadatas']):
                # 检查metadata中是否包含file_md5信息
                if 'file_md5' in metadata and metadata['file_md5'] == file_md5:
                    ids_to_delete.append(results['ids'][i])

            if ids_to_delete:
                # 删除相关文档
                collection.delete(ids=ids_to_delete)
                knowledge_logger.info('[VECTOR_STORE_CLEAR_MD5] 成功清理MD5向量数据，删除了 ' + str(
                    len(ids_to_delete)) + ' 个向量 | file_md5: ' + file_md5)
                return True
            else:
                knowledge_logger.info('[VECTOR_STORE_CLEAR_MD5] 未找到MD5的向量数据 | file_md5: ' + file_md5)
                return True
        except Exception as e:
            err_info = str(e)
            knowledge_logger.error(
                '[VECTOR_STORE_CLEAR_MD5_ERROR] 清理MD5向量数据失败 | file_md5: ' + file_md5 + ' | error: ' + err_info)
            return False