# Step1. Compile and Build
## Ready for Compile and Build with OpenJDK-11
# FROM openjdk:11-jdk-buster as builder
FROM openjdk:17-alpine3.14 as builder
WORKDIR /src

COPY ./src .
COPY ./docker-entrypoint.sh .

# get --build-arg param, default=detached
ARG GIT_BRANCH="detached"
# set env
ENV GIT_BRANCH=${GIT_BRANCH}

RUN ./gradlew clean build -x test
# .Step1. Compile and Build
FROM openjdk:17-alpine3.14

RUN apk update && apk upgrade && apk add --no-cache curl bash snappy

## download tools
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.7.3/wait /bin/wait

WORKDIR /kafka-rest-proxy

## copy files from build
COPY --from=builder /src/docker-entrypoint.sh /docker-entrypoint.sh
COPY --from=builder /src/build/libs /kafka-rest-proxy/libs
COPY --from=builder /src/config/logback.xml /kafka-rest-proxy/logback.xml.default
# COPY --from=builder /src/config/kafka-rest.yml /kafka-rest-proxy/config/kafka-rest.yml

## dir permissions
RUN mkdir -p /kafka-rest-proxy/logs /kafka-rest-proxy/config; \
    chgrp -R 0 /kafka-rest-proxy/logs /kafka-rest-proxy/config; \
    chmod -R g+rwX /kafka-rest-proxy/logs /kafka-rest-proxy/config

RUN chmod +x /bin/wait

## service port
EXPOSE 8080

# Run Java
ENTRYPOINT ["/docker-entrypoint.sh"]

