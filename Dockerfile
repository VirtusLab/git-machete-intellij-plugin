# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

RUN set -x \
  `# workaround for error in debian-slim during openjdk installation` \
  && mkdir -p /usr/share/man/man1 \
  && apt-get update \
  `# installing JDK and not just JRE to provide javadoc executable` \
  && apt-get install --no-install-recommends -y \
    curl git openjdk-11-jdk-headless openssh-client python3 python3-pip xxd unzip \
  && pip3 install git-machete==2.13.6 \
  && apt-get purge --autoremove -y python3-pip \
  && rm -rf /var/lib/apt/lists/*

ARG hub_version=2.14.2

# Install hub (GitHub CLI); Debian Buster package has an ancient version (e.g. `hub pr show` isn't supported there yet)
RUN set -x \
  && curl -Lsf https://github.com/github/hub/releases/download/v${hub_version}/hub-linux-amd64-${hub_version}.tgz -o hub.tgz \
  && tar --directory=/usr/local/bin/ --strip-components=2 -xvzf hub.tgz hub-linux-amd64-${hub_version}/bin/hub \
  && rm hub.tgz \
  && hub --version

WORKDIR /stripped_repo

# Create gradle cache.
# `rw` option doesn't allow to make any changes on original dir but rather create something like overlayfs
# We need this to allow `./gradlew` to write in `./.gradle` directory (even though this directory won't make it to the final image anyway).
RUN --mount=type=bind,rw,source=.,target=. \
  `# no-daemon so that no data about the daemon active during the image build makes it to the final image under ~/.gradle/daemon/` \
  ./gradlew --no-daemon --info resolveDependencies \
  && rm -v /root/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIC/*.*/*/ideaIC-*.zip
