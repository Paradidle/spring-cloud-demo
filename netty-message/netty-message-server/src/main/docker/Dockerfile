FROM openjdk:8u212-jdk-alpine

# 设置工作目录
WORKDIR /app

# 设置时区和语言
ENV LANG en_US.UTF-8
ENV TZ=Asia/Shanghai

# 使用阿里云镜像源安装字体
RUN echo -e "https://mirrors.aliyun.com/alpine/v3.6/main/" > /etc/apk/repositories \
    && echo -e "https://mirrors.aliyun.com/alpine/v3.6/community/" >> /etc/apk/repositories \
    && apk update && apk upgrade \
    && apk --no-cache add ttf-dejavu fontconfig tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建日志目录
RUN mkdir -p /root/applogs/gc

# 复制 JAR 文件 - Maven 插件会自动处理这个复制
COPY *.jar netty-message-server.jar

# 暴露端口
EXPOSE 20007

# JVM 参数
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -server -Xms256m -Xmx1024m -XX:+PrintGCDetails -Xloggc:/root/applogs/gc/netty-message-server.gc -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/root/applogs/gc/netty-message-server.dump"

# 启动应用
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/netty-message-server.jar" ]