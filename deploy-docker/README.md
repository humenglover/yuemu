# 悦木图库项目部署

1. 将后端打包好的jar报放到与docker-compose.yml和Dockerfile同级目录下

2. 建立docker-compose.yml所需的文件夹，并赋予权限

3. 将nginx的配置文件中的your_server_url改成自己的域名，要有自己的ssl证书。

4. 构建并运行项目

   ```
   docker compose up
   ```

   docker将自动拉取镜像并构建。
