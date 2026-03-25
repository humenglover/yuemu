# 基础镜像
FROM alpine/java:11-jre

# 设定时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

# 拷贝jar包
COPY yuemu-picture-backend.jar /app.jar

# 入口
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app.jar"]