from typing import List, Optional, Dict, Any
from fastapi import UploadFile
import os
from datetime import datetime
from utils.file_utils import calculate_md5, check_md5_exists_only, save_md5_with_filename, find_filename_by_md5, get_file_documents
from utils.log_utils import knowledge_logger
from model.dto.knowledge_management_dto import KnowledgeFileDTO
from model.common.response_wrapper import ResponseWrapper
import shutil
from langchain_core.documents import Document
import yaml

class KnowledgeFile:
    def __init__(self, filename: str, filepath: str, size: int, extension: str, md5: str):
        self.filename = filename
        self.filepath = filepath
        self.size = size
        self.extension = extension
        self.md5 = md5
        self.created_at = datetime.now()
        self.updated_at = datetime.now()

class KnowledgeManagementService:
    def __init__(self):
        chroma_config_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'config', 'chroma.yml')
        with open(chroma_config_path, 'r', encoding='utf-8') as f:
            chroma_config = yaml.safe_load(f)

        self.storage_path = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                                         chroma_config.get('data_path', './knowledge'))
        self.max_files = chroma_config.get('max_files', 100)
        self.allowed_extensions = chroma_config.get('allowed_extensions', ['.txt', '.pdf', '.docx', '.md'])

        os.makedirs(self.storage_path, exist_ok=True)

        self.md5_store_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "md5", "md5_record.txt")
        os.makedirs(os.path.dirname(self.md5_store_path), exist_ok=True)

        if not os.path.exists(self.md5_store_path):
            with open(self.md5_store_path, 'w', encoding='utf-8') as f:
                pass

    async def process_upload_file(self, file: UploadFile, vector_store_manager):
        knowledge_logger.info("\n" + "=" * 80)
        knowledge_logger.info(f"文件上传开始 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]}")
        knowledge_logger.info("=" * 80)
        knowledge_logger.info(f"原始文件名: {file.filename}")
        knowledge_logger.info(f"Content-Type: {file.content_type}")
        knowledge_logger.info(f"允许的文件类型: {self.allowed_extensions}")

        knowledge_logger.info(f"[UPLOAD_START] 开始上传文件 | 文件名: {file.filename}")

        file_location = None
        file_md5 = None

        try:
            file_ext = os.path.splitext(file.filename)[1].lower()
            knowledge_logger.info("\n步骤1：文件类型校验")
            knowledge_logger.info(f"   文件后缀: {file_ext}")

            if file_ext not in self.allowed_extensions:
                error_msg = f"不支持的文件类型: {file_ext}，仅支持{self.allowed_extensions}"
                knowledge_logger.warning(f"错误: {error_msg}")
                knowledge_logger.info("=" * 80 + "\n")
                knowledge_logger.warning(f"[UPLOAD_ERROR] {error_msg}")
                return ResponseWrapper.bad_request(msg=error_msg)

            current_count = len(self.get_all_knowledge_files())
            knowledge_logger.info("\n步骤2：文件数量校验")
            knowledge_logger.info(f"   当前文件数: {current_count}")
            knowledge_logger.info(f"   最大文件数: {self.max_files}")

            if current_count >= self.max_files:
                error_msg = f"文件数量达到上限: {self.max_files}，当前已有{current_count}个文件"
                knowledge_logger.warning(f"错误: {error_msg}")
                knowledge_logger.info("=" * 80 + "\n")
                knowledge_logger.warning(f"[UPLOAD_ERROR] {error_msg}")
                return ResponseWrapper.bad_request(msg=error_msg)

            original_filename = file.filename
            new_filename = original_filename
            file_location = os.path.join(self.storage_path, new_filename)

            if os.path.exists(file_location):
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]
                name, ext = os.path.splitext(original_filename)
                new_filename = f"{name}_{timestamp}{ext}"
                file_location = os.path.join(self.storage_path, new_filename)

            knowledge_logger.info("\n步骤3：文件保存")
            knowledge_logger.info(f"   保存路径: {file_location}")

            with open(file_location, "wb") as f:
                shutil.copyfileobj(file.file, f)

            file_size = os.path.getsize(file_location)
            knowledge_logger.info(f"   文件大小: {file_size} 字节 ({file_size / 1024:.2f} KB)")

            file_md5 = calculate_md5(file_location)
            knowledge_logger.info("\n步骤4：MD5计算")
            knowledge_logger.info(f"   文件MD5: {file_md5}")

            if check_md5_exists_only(file_md5, self.md5_store_path):
                os.remove(file_location)
                existing_filename = find_filename_by_md5(file_md5, self.md5_store_path)
                knowledge_logger.warning(f"文件已存在，跳过上传: {existing_filename}")
                knowledge_logger.info("=" * 80 + "\n")
                knowledge_logger.info(f"[UPLOAD_SKIP] 文件已存在 | MD5: {file_md5[:8]} | 文件名: {existing_filename}")
                return ResponseWrapper.success(msg="文件已存在，无需重复上传", data={
                    "filename": existing_filename,
                    "md5": file_md5,
                    "skipped": True
                })

            save_md5_with_filename(file_md5, new_filename, self.md5_store_path)
            knowledge_logger.info(f"   MD5记录保存到: {self.md5_store_path}")

            knowledge_logger.info("\n步骤5：文档解析")
            knowledge_logger.info(f"   开始解析文件: {file_location}")

            try:
                documents = get_file_documents(file_location)

                if file_ext == '.pdf' and (not documents or len(documents) == 0):
                    try:
                        import PyPDF2
                        with open(file_location, 'rb') as f:
                            pdf_reader = PyPDF2.PdfReader(f)
                            if pdf_reader.is_encrypted:
                                raise Exception("PDF文件已加密，无法解析")
                    except Exception as e:
                        raise Exception(f"PDF解析失败: {str(e)}")

            except Exception as doc_err:
                error_msg = f"文档解析失败: {str(doc_err)}"
                knowledge_logger.error(f"解析失败: {error_msg}")
                knowledge_logger.info(f"清理文件: {file_location}")
                knowledge_logger.info("=" * 80 + "\n")

                if file_location and os.path.exists(file_location):
                    os.remove(file_location)
                if file_md5:
                    self._remove_md5_record_by_md5(file_md5)

                knowledge_logger.error(f"[UPLOAD_DOC_ERROR] {error_msg}")
                return ResponseWrapper.error(msg=error_msg)

            if not documents or len(documents) == 0:
                error_msg = "文档解析后内容为空"
                knowledge_logger.warning(f"{error_msg}")
                knowledge_logger.info(f"清理空文件: {file_location}")
                knowledge_logger.info("=" * 80 + "\n")

                os.remove(file_location)
                self._remove_md5_record_by_md5(file_md5)

                knowledge_logger.warning(f"[UPLOAD_DOC_EMPTY] {error_msg}")
                return ResponseWrapper.error(msg=error_msg)

            knowledge_logger.info(f"解析成功 | 原始文档片段数: {len(documents)}")

            knowledge_logger.info("\n步骤6：文档分片")

            split_document = vector_store_manager.text_splitter.split_documents(documents)

            knowledge_logger.info(f"分片完成 | 总分片数: {len(split_document)}")
            knowledge_logger.info(f"前3个分片预览:")

            filename = os.path.basename(file_location)
            for idx, doc in enumerate(split_document):
                if doc.metadata is None:
                    doc.metadata = {}

                doc.metadata['file_md5'] = file_md5.strip().lower()
                doc.metadata['filename'] = filename
                doc.metadata['source'] = file_location
                doc.metadata['chunk_index'] = idx

                if idx < 3:
                    chunk_content = doc.page_content[:150] + "..." if len(doc.page_content) > 150 else doc.page_content
                    knowledge_logger.info(f"   分片{idx}: {chunk_content}")

            knowledge_logger.info("\n步骤7：清空旧向量")
            knowledge_logger.info(f"   MD5: {file_md5}")

            clear_success = vector_store_manager.clear_vectors_by_md5(file_md5)

            knowledge_logger.info(f"清空旧向量: 成功" if clear_success else "清空旧向量: 失败")

            knowledge_logger.info("\n步骤8：添加向量到数据库")

            add_ids = vector_store_manager.vectorstore.add_documents(split_document)

            knowledge_logger.info(f"向量添加成功 | 新增向量数: {len(add_ids)}")

            knowledge_logger.info("\n步骤9：验证向量")

            verify_docs = vector_store_manager.vectorstore.get(where={"file_md5": file_md5})
            verify_count = len(verify_docs['ids']) if verify_docs and verify_docs['ids'] else 0

            knowledge_logger.info(f"向量验证 | 该文件向量总数: {verify_count}")

            if verify_count == 0:
                raise Exception("向量添加后未找到对应数据")

            knowledge_logger.info("\n步骤10：上传完成")
            knowledge_logger.info(f"   原始文件名: {original_filename}")
            knowledge_logger.info(f"   保存文件名: {new_filename}")
            knowledge_logger.info(f"   文件MD5: {file_md5}")
            knowledge_logger.info(f"   向量总数: {verify_count}")
            knowledge_logger.info("=" * 80 + "\n")

            knowledge_logger.info(
                f"[UPLOAD_SUCCESS] 文件上传成功 | 文件名: {new_filename} | MD5: {file_md5} | 向量数: {verify_count}")

            return ResponseWrapper.success(msg="文件上传成功", data={
                "filename": new_filename,
                "original_filename": original_filename,
                "file_path": file_location,
                "md5": file_md5,
                "size": file_size,
                "vector_count": verify_count
            })

        except Exception as e:
            error_msg = f"上传失败: {str(e)}"
            knowledge_logger.error(f"\n上传异常: {error_msg}")
            if file_location:
                knowledge_logger.info(f"清理临时文件: {file_location}")
            knowledge_logger.info("=" * 80 + "\n")

            if file_location and os.path.exists(file_location):
                os.remove(file_location)
            if file_md5:
                self._remove_md5_record_by_md5(file_md5)

            knowledge_logger.error(f"[UPLOAD_ERROR] {error_msg} | 文件名: {file.filename}")
            return ResponseWrapper.error(msg=error_msg)

    def get_all_knowledge_files(self) -> List[KnowledgeFile]:
        try:
            files = []

            knowledge_logger.info(f"\n获取知识库文件列表 | 路径: {self.storage_path}")

            for filename in os.listdir(self.storage_path):
                filepath = os.path.join(self.storage_path, filename)

                if os.path.isfile(filepath):
                    extension = os.path.splitext(filename)[1].lower()

                    if extension in self.allowed_extensions:
                        size = os.path.getsize(filepath)
                        md5 = calculate_md5(filepath)

                        file_info = KnowledgeFile(
                            filename=filename,
                            filepath=filepath,
                            size=size,
                            extension=extension,
                            md5=md5
                        )
                        files.append(file_info)

            knowledge_logger.info(f"找到 {len(files)} 个有效知识库文件")

            knowledge_logger.info(f"[GET_FILE_LIST_SUCCESS] 获取文件列表成功 | 总数: {len(files)}")
            return files

        except Exception as e:
            knowledge_logger.error(f"\n获取文件列表失败: {str(e)}")
            knowledge_logger.error(f"[GET_FILE_LIST_ERROR] 获取文件列表失败 | 错误: {str(e)}")
            return []

    async def get_all_knowledge_files_api(self, file_type: str = None):
        knowledge_logger.info(f"\n接收到获取文件列表请求 | 文件类型过滤: {file_type if file_type else '无'}")

        try:
            all_files = self.get_all_knowledge_files()

            if file_type:
                all_files = [f for f in all_files if f.filename.lower().endswith(file_type.lower())]

            total = len(all_files)

            file_list = []
            for file in all_files:
                file_info = {
                    "filename": file.filename,
                    "filepath": file.filepath,
                    "size": file.size,
                    "extension": file.extension,
                    "md5": file.md5,
                    "created_at": file.created_at.isoformat() if file.created_at else None,
                    "updated_at": file.updated_at.isoformat() if file.updated_at else None
                }
                file_list.append(file_info)

            knowledge_logger.info(f"返回文件列表 | 总数: {total}")

            return ResponseWrapper.success(
                data={"files": file_list, "total": total},
                msg=f"成功获取所有知识库文件，共{total}个文件"
            )
        except Exception as e:
            error_msg = f"获取文件列表失败：{str(e)}"
            knowledge_logger.error(f"❌ {error_msg}")
            knowledge_logger.error(f"[GET_FILE_LIST_ERROR] {error_msg}")
            return ResponseWrapper.error(msg=error_msg, data={"error": str(e)})

    def delete_knowledge_file(self, file_md5: str, vector_store_manager) -> bool:
        return self.delete_knowledge_file_by_md5(file_md5, vector_store_manager)

    def delete_knowledge_file_by_md5(self, file_md5: str, vector_store_manager) -> bool:
        try:
            knowledge_logger.info(f"\n删除文件 | MD5: {file_md5}")

            filename = find_filename_by_md5(file_md5, self.md5_store_path)

            vector_clear_success = vector_store_manager.clear_vectors_by_md5(file_md5)

            if filename:
                file_path = os.path.join(self.storage_path, filename)
                if os.path.exists(file_path):
                    os.remove(file_path)
                    knowledge_logger.info(f"删除物理文件: {file_path}")

            self._remove_md5_record_by_md5(file_md5)

            knowledge_logger.info(f"删除完成 | MD5: {file_md5}")

            knowledge_logger.info(f"[DELETE_SUCCESS] 删除成功 | MD5: {file_md5}")
            return True

        except Exception as e:
            knowledge_logger.error(f"\n删除失败 | MD5: {file_md5} | 错误: {str(e)}")
            knowledge_logger.error(f"[DELETE_ERROR] 删除失败 | MD5: {file_md5} | 错误: {str(e)}")
            return False

    async def delete_multiple_knowledge_files_by_md5(self, md5_hashes: List[str], vector_store_manager) -> Dict[
        str, Any]:
        success_count = 0
        failed_md5s = []

        knowledge_logger.info(f"\n批量删除 | 待删除MD5数量: {len(md5_hashes)}")

        for file_md5 in md5_hashes:
            try:
                success = self.delete_knowledge_file(file_md5, vector_store_manager)
                if success:
                    success_count += 1
                else:
                    failed_md5s.append(file_md5)
            except Exception as e:
                knowledge_logger.error(f"批量删除异常 | MD5: {file_md5} | 错误: {str(e)}")
                knowledge_logger.error(f"[DELETE_MULTIPLE_ERROR] 删除异常 | MD5: {file_md5} | 错误: {str(e)}")
                failed_md5s.append(file_md5)

        knowledge_logger.info(f"\n批量删除结果 | 成功: {success_count} | 失败: {len(failed_md5s)}")

        return {
            "success_count": success_count,
            "failed_count": len(failed_md5s),
            "failed_md5s": failed_md5s
        }

    async def delete_multiple_knowledge_files_by_md5_api(self, request: dict, vector_store_manager):
        knowledge_logger.info(f"\n接收到删除文件请求 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

        try:
            md5_hashes = request.get("md5_hashes", [])
            if not md5_hashes or not isinstance(md5_hashes, list):
                error_msg = "请提供要删除的MD5哈希值列表"
                knowledge_logger.warning(f"{error_msg}")
                return ResponseWrapper.bad_request(msg=error_msg)

            result = await self.delete_multiple_knowledge_files_by_md5(md5_hashes, vector_store_manager)

            if result["success_count"] > 0:
                msg = f"成功删除{result['success_count']}个文件"
                if result["failed_md5s"]:
                    msg += f"，失败MD5: {', '.join(result['failed_md5s'])}"
            else:
                msg = "删除知识库文件全部失败"

            knowledge_logger.info(f"{msg}")

            return ResponseWrapper.success(msg=msg, data=result)
        except Exception as e:
            error_msg = f"删除文件失败：{str(e)}"
            knowledge_logger.error(f"{error_msg}")
            knowledge_logger.error(f"[DELETE_API_ERROR] {error_msg}")
            return ResponseWrapper.error(msg=error_msg, data={"error": str(e)})

    async def clear_all_knowledge(self, vector_store_manager) -> bool:
        try:
            knowledge_logger.info(f"\n清空所有知识库")

            knowledge_files = self.get_all_knowledge_files()

            for file in knowledge_files:
                success = self.delete_knowledge_file(file.md5, vector_store_manager)
                if not success:
                    knowledge_logger.warning(f"清空时删除失败 | MD5: {file.md5}")

            md5_records_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "md5", "md5_record.txt")
            if os.path.exists(md5_records_path):
                open(md5_records_path, "w").close()
                knowledge_logger.info(f"清空MD5记录文件: {md5_records_path}")

            vector_clear_success = vector_store_manager.clear_all_vectors()

            knowledge_logger.info(f"清空向量库: 成功" if vector_clear_success else "清空向量库: 失败")
            knowledge_logger.info(f"清空完成 | 共处理 {len(knowledge_files)} 个文件")

            if vector_clear_success:
                knowledge_logger.info(f"[CLEAR_ALL_SUCCESS] 清空成功 | 删除文件数: {len(knowledge_files)}")
                return True
            else:
                knowledge_logger.error("[CLEAR_ALL_VECTOR_ERROR] 清空向量库失败")
                return False

        except Exception as e:
            knowledge_logger.error(f"\n清空失败 | 错误: {str(e)}")
            knowledge_logger.error(f"[CLEAR_ALL_ERROR] 清空失败 | 错误: {str(e)}")
            return False

    async def clear_all_knowledge_api(self, vector_store_manager):
        knowledge_logger.info(f"\n接收到清空所有知识库请求 | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

        try:
            success = await self.clear_all_knowledge(vector_store_manager)
            if success:
                knowledge_files = self.get_all_knowledge_files()
                msg = f"成功清空所有知识库文件，共删除 {len(knowledge_files)} 个文件"
                knowledge_logger.info(f"{msg}")
                return ResponseWrapper.success(msg=msg)
            else:
                knowledge_logger.warning(f"清空知识库失败")
                return ResponseWrapper.error(msg="清空知识库失败")
        except Exception as e:
            error_msg = f"清空知识库失败：{str(e)}"
            knowledge_logger.error(f"{error_msg}")
            knowledge_logger.error(f"[CLEAR_ALL_ERROR] {error_msg}")
            return ResponseWrapper.error(msg=error_msg, data={"error": str(e)})

    async def verify_vector_metadata_api(self, file_md5: str, vector_store_manager):
        knowledge_logger.info(f"\n接收到向量验证请求 | MD5: {file_md5}")

        try:
            collection = vector_store_manager.vectorstore._client.get_collection(
                name=vector_store_manager.config['collection_name'])
            results = collection.get(where={"file_md5": file_md5.strip().lower()})
            count = len(results['ids'])
            metadata_sample = results['metadatas'][0] if results['metadatas'] else None

            knowledge_logger.info(f"向量验证成功 | MD5: {file_md5} | 向量数: {count}")

            return ResponseWrapper.success(data={
                "md5": file_md5,
                "vector_count": count,
                "metadata_sample": metadata_sample
            })
        except Exception as e:
            error_msg = f"向量验证失败：{str(e)}"
            knowledge_logger.error(f"{error_msg}")
            knowledge_logger.error(f"[VERIFY_VECTOR_ERROR] {error_msg} | MD5: {file_md5}")
            return ResponseWrapper.error(msg=error_msg, data={"error": str(e)})

    def _remove_md5_record(self, filename: str):
        try:
            if os.path.exists(self.md5_store_path):
                with open(self.md5_store_path, "r", encoding="utf-8") as f:
                    lines = f.readlines()

                file_path = os.path.join(self.storage_path, filename)
                if os.path.exists(file_path):
                    file_md5 = calculate_md5(file_path)

                    filtered_lines = [
                        line for line in lines
                        if not (line.startswith(file_md5 + ":") and filename in line)
                    ]

                    with open(self.md5_store_path, "w", encoding="utf-8") as f:
                        f.writelines(filtered_lines)

        except Exception as e:
            knowledge_logger.error(f"[REMOVE_MD5_ERROR] 移除MD5记录失败 | 文件名: {filename} | 错误: {str(e)}")

    def _remove_md5_record_by_md5(self, file_md5: str):
        try:
            if os.path.exists(self.md5_store_path):
                with open(self.md5_store_path, "r", encoding="utf-8") as f:
                    lines = f.readlines()

                filtered_lines = [
                    line for line in lines
                    if not line.startswith(file_md5 + ":")
                ]

                with open(self.md5_store_path, "w", encoding="utf-8") as f:
                    f.writelines(filtered_lines)

        except Exception as e:
            knowledge_logger.error(f"[REMOVE_MD5_BY_MD5_ERROR] 按MD5移除记录失败 | MD5: {file_md5} | 错误: {str(e)}")

knowledge_management_service = KnowledgeManagementService()