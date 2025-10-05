# 定制镜像,以一个镜像为基础，在其上进行定制,基础镜像是必须指定的.FROM就是指定基础镜像
# FROM 是必备的指令，并 且必须是第一条指令。
FROM openjdk:8u212-jdk-alpine
VOLUME /tmp

ENV LANG en_US.UTF-8

# openjdk:8u212-jdk-alpine镜像字体
# 国内龟速 RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
RUN echo -e "https://mirrors.aliyun.com/alpine/v3.6/main/" > /etc/apk/repositories && apk update && apk upgrade && apk --no-cache add ttf-dejavu fontconfig
# RUN echo -e "https://mirrors.aliyun.com/alpine/v3.6/community/" > /etc/apk/repositories && apk update && apk upgrade && apk --no-cache add ttf-dejavu fontconfig

ADD netty-message-server.jar netty-message-server.jar
# RUN 指令是用来执行命令行命令的。
RUN sh -c 'touch /netty-message-server.jar'
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -server -Xms256m -Xmx1024m -XX:+PrintGCDetails -Xloggc:/root/applogs/gc/netty-message-server.gc -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/root/applogs/gc/netty-message-server.dump"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /netty-message-server.jar" ]
# * Dockerfile 中每一个指令都会建立一层


# 创建gc目录且删除已有文件，否则gc打印时创建文件失败
# sudo mkdir -p ~/applogs/gc/ && sudo rm -f ~/applogs/gc/netty-message-server.gc
# sudo mkdir -p ~/applogs/gc/ && sudo rm -f ~/applogs/gc/netty-message-server.dump


# 运行命令: 
# spring_profiles_active=test为改变运行环境的参数
# docker run -d --restart=always --net=host \
#  --name netty-message-server \
#  --ulimit nofile=65535:65535 --ulimit nproc=65535:65535 \
#  --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 \
#  -v ~/applogs:/root/applogs \
#  -e "spring_profiles_active=test" \
#  -e "JAVA_OPTS=-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Duser.timezone=GMT+08 -server -Xms256m -Xmx1024m -XX:+PrintGCDetails -Xloggc:/root/applogs/gc/netty-message-server.gc -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/root/applogs/gc/netty-message-server.dump" \
#  netty-message-server


# 手动创建images（Dockerfile和netty-message-server.jar放同一目录，然后执行命令）: docker build -t netty-message-server .

# 进入容器：sudo docker exec -it 容器名称或id /bin/sh   或者：docker exec -it 容器名称或id /bin/bash

