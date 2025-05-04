#!/bin/bash

set -e
umask 0002

#### external config ####
if [ ! -z "${KAFKA_REST_PROXY_CONFIG}" ]; then
  echo "${KAFKA_REST_PROXY_CONFIG}" > /kafka-rest-proxy/config/kafka-rest.yml
fi
if [ ! -f /kafka-rest-proxy/config/kafka-rest.yml ]; then
  cp /kafka-rest-proxy/kafka-rest.yml.default /kafka-rest-proxy/config/kafka-rest.yml
fi
########


#### logback ####
if [ ! -z "${KAFKA_REST_PROXY_LOGBACK_XML}" ]; then
  echo "${KAFKA_REST_PROXY_LOGBACK_XML}" > /kafka-rest-proxy/config/logback.xml
fi

if [ ! -f /kafka-rest-proxy/config/logback.xml ]; then
  cp /kafka-rest-proxy/logback.xml.default /kafka-rest-proxy/config/logback.xml
fi
#######

## wait other host
/bin/wait

## execute kafka-rest-proxy
JAR_FILE=$(ls /kafka-rest-proxy/libs/kafka-rest-proxy-*.jar)
umask 0002 && exec java -jar "${JAR_FILE}"
