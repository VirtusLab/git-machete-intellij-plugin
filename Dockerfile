# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

# Creating man directory is a workaround for error in debian-slim during openjdk installation
RUN set -x && \
    mkdir -p /usr/share/man/man1 && \
    apt-get update && \
    apt-get install --no-install-recommends -y git openjdk-11-jre openssh-client python3 python3-pip && \
    pip3 install git-machete==2.13.5 && \
    apt-get autoremove -y python3-pip && \
    rm -rf /var/lib/apt/lists/*

# Enable `globstar` shell option in `RUN` script below
RUN ln -sf /bin/bash /bin/sh
# Create gradle cache
RUN --mount=type=bind,source=.,target=/original_repo \
  set -x && \
  mkdir /prepared_repo && \
  shopt -s globstar && \
  cd /original_repo && \
  cp --parents -r gradle/ gradlew **/*.gradle  /prepared_repo/ && \
  cd /prepared_repo && \
  ./gradlew --info && \
  rm -r /root/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIC/LATEST-EAP-SNAPSHOT/ # workaround for https://github.com/JetBrains/gradle-intellij-plugin/issues/443
