#!/bin/bash
set -e -u -o pipefail

if [ $# -lt 1 ]; then
  echo 'version is required'
  exit 1
fi

VERSION=$1
ARCH=amd64
PROJECT_NAME=plus-jdk17

if [ $# -ge 2 ]; then
  ARCH=$2
fi

docker build . -t ${PROJECT_NAME}:${VERSION}

docker tag ${PROJECT_NAME}:${VERSION} novicezk/${PROJECT_NAME}-${ARCH}:${VERSION}
docker push novicezk/${PROJECT_NAME}-${ARCH}:${VERSION}