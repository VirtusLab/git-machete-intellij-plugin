# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

RUN set -x && \
  `# workaround for error in debian-slim during openjdk installation` \
  && mkdir -p /usr/share/man/man1 \
  && apt-get update \
  `# installing JDK and not just JRE to provide javadoc executable` \
  && apt-get install --no-install-recommends -y \
    curl git hub openjdk-11-jdk-headless openssh-client python3 python3-pip xxd \
  && pip3 install git-machete==2.13.6 \
  && apt-get purge --autoremove -y python3-pip \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /stripped_repo

# Create gradle cache.
# `rw` option doesn't allow to make any changes on original dir but rather create something like overlayfs
# We need this to allow `./gradlew` to write in `./.gradle` directory (even though this directory won't make it to the final image anyway).
RUN --mount=type=bind,rw,source=.,target=. \
  ./gradlew --info resolveDependencies \
  && rm /root/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIC/*.*/*/ideaIC-*.zip

# Script for enforcing compatibility is being run to download IntelliJ dependencies.
# We don't want Gradle build caches to be populated during the plugin build
# since they consume space in the image and are going to become out-of-date pretty soon after the image is built anyway.
RUN --mount=type=bind,rw,source=.,target=. \
  ./gradlew -Dorg.gradle.caching=false --info buildPlugin \
  && scripts/enforce-binary-compatibility
