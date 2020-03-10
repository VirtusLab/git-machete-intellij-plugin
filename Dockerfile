# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

WORKDIR /app

# Creating man directory is a workaround for error in debian-slim during openjdk installation
RUN set -x && \
    mkdir -p /usr/share/man/man1 && \
    apt-get update && \
    apt-get install --no-install-recommends -y git openjdk-11-jre openssh-client python3 python3-pip && \
    pip3 install git-machete==2.13.5 && \
    apt-get autoremove -y python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Create gradle cache
# `rw` option doesn't allow to make any changes on original dir but rather create something like overlayfs
RUN --mount=type=bind,rw,source=.,target=. \
  set -x && \
  ./gradlew --info