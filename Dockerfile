# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

RUN set -x \
  `# workaround for error in debian-slim during openjdk installation` \
  && mkdir -p /usr/share/man/man1 \
  && apt-get update \
  `# installing JDK and not just JRE to provide javadoc executable` \
  && apt-get install --no-install-recommends -y curl git openjdk-11-jdk-headless openssh-client python3 python3-pip \
  && pip3 install git-machete==2.14.0 \
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
RUN --mount=type=bind,rw,source=.,target=.  set -x \
  `# no-daemon so that no data about the daemon active during the image build makes it to the final image under ~/.gradle/daemon/` \
  && find . \
  && ./gradlew --no-daemon --info resolveDependencies \
  && rm -v /root/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIC/*.*/*/ideaIC-*.zip

# Secondary packages needed in just one (or few) steps of the pipeline; subject to frequent change, thus moved to the end of the pipeline
# (package => needed for command(s))
# binutils       => strings
# xml-twig-tools => xml_grep
# xxd            => xxd
# unzip          => zipinfo
RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y binutils xml-twig-tools xxd unzip \
  `# tools necessary to run non-headless UI tests in the screen-less environment of CI` \
  && apt-get install --no-install-recommends -y libx11-6 libxrender1 libxtst6 xauth xvfb \
  && rm -rf /var/lib/apt/lists/*

# Disable IntelliJ data sharing
RUN set -x \
  && mkdir -p /root/.local/share/JetBrains/consentOptions/ \
  && echo -n rsch.send.usage.stat:1.1:0:1574939222872 > /root/.local/share/JetBrains/consentOptions/accepted

# Enable NON-headless tests of plugin's UI (xvfb = X virtual framebuffer)
ENV IDEPROBE_DISPLAY=xvfb
