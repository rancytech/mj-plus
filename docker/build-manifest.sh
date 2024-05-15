#!/bin/bash
set -e -u -o pipefail

if [ $# -lt 1 ]; then
  echo 'version is required'
  exit 1
fi

VERSION=$1
PROJECT_NAME=plus-jdk17

echo "remove old manifest..."
docker manifest rm novicezk/${PROJECT_NAME}:${VERSION}

echo "create manifest..."
docker manifest create novicezk/${PROJECT_NAME}:${VERSION} novicezk/${PROJECT_NAME}-amd64:${VERSION} novicezk/${PROJECT_NAME}-arm64v8:${VERSION}

echo "annotate amd64..."
docker manifest annotate novicezk/${PROJECT_NAME}:${VERSION} novicezk/${PROJECT_NAME}-amd64:${VERSION} --os linux --arch amd64

echo "annotate arm64v8..."
docker manifest annotate novicezk/${PROJECT_NAME}:${VERSION} novicezk/${PROJECT_NAME}-arm64v8:${VERSION} --os linux --arch arm64 --variant v8

echo "push manifest..."
docker manifest push novicezk/${PROJECT_NAME}:${VERSION}