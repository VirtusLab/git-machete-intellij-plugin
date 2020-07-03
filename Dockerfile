# syntax=docker/dockerfile:experimental

# Explicit `docker.io` is necessary here when building with DOCKER_BUILDKIT=1
FROM docker.io/debian:buster-slim

RUN set -x \
  `# workaround for error in debian-slim during openjdk installation` \
  && mkdir -p /usr/share/man/man1 \
  && apt-get update \
  `# installing JDK and not just JRE to provide javadoc executable` \
  && apt-get install --no-install-recommends -y curl git openjdk-11-jdk-headless openssh-client python3 \
  && rm -rf /var/lib/apt/lists/*

ARG hub_version=2.14.2

# Install hub (GitHub CLI); Debian Buster package has an ancient version (e.g. `hub pr show` isn't supported there yet)
RUN set -x \
  && curl -Lsf https://github.com/github/hub/releases/download/v${hub_version}/hub-linux-amd64-${hub_version}.tgz -o hub.tgz \
  && tar --directory=/usr/local/bin/ --strip-components=2 -xvzf hub.tgz hub-linux-amd64-${hub_version}/bin/hub \
  && rm hub.tgz \
  && hub --version

RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y python3-pip \
  && cli_version=$(cut -d'=' -f2 backendImpl/src/test/resources/reference-cli-version.properties)
  && pip3 install git-machete==$cli_version \
  && apt-get purge --autoremove -y python3-pip \
  && rm -rf /var/lib/apt/lists/*

# Create gradle cache.
# `rw` option doesn't allow to make any changes on original dir but rather create something like overlayfs
# We need this to allow `./gradlew` to write in `./.gradle` directory (even though this directory won't make it to the final image anyway).
WORKDIR /stripped_repo
RUN --mount=type=bind,rw,source=.,target=.  set -x \
  `# no-daemon so that no data about the daemon active during the image build makes it to the final image under ~/.gradle/daemon/` \
  && find . \
  && ./gradlew --no-daemon --info resolveDependencies \
  && rm -v /root/.gradle/caches/modules-2/files-2.1/com.jetbrains.intellij.idea/ideaIC/*.*/*/ideaIC-*.zip
WORKDIR /root
RUN rmdir /stripped_repo

# Secondary packages needed in just one (or few) steps of the pipeline;
# subject to frequent change, thus moved towards the end of the pipeline.
# (package       => needed for command(s))
# binutils       => strings
# netcat         => nc
# procps         => ps
# xxd            => xxd
# unzip          => zipinfo
RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y binutils netcat procps xxd unzip \
  `# tools necessary to run non-headless UI tests in the screen-less environment of CI` \
  && apt-get install --no-install-recommends -y libx11-6 libxrender1 libxtst6 xauth xvfb \
  && rm -rf /var/lib/apt/lists/*

# Disable IntelliJ data sharing
RUN set -x \
  && dir=/root/.local/share/JetBrains/consentOptions \
  && mkdir -p "$dir" \
  && echo -n "rsch.send.usage.stat:1.1:0:$(date +%s)000" > "$dir/accepted"

# Accept End User Agreement/privacy policy
RUN set -x \
  && dir="/root/.java/.userPrefs/jetbrains/_!(!!cg\"p!(}!}@\"j!(k!|w\"w!'8!b!\"p!':!e@==" \
  && mkdir -p "$dir" \
  && echo '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n\
<!DOCTYPE map SYSTEM "http://java.sun.com/dtd/preferences.dtd">\n\
<map MAP_XML_VERSION="1.0">\n\
  <entry key="accepted_version" value="2.1"/>\n\
  <entry key="eua_accepted_version" value="1.1"/>\n\
  <entry key="privacyeap_accepted_version" value="2.1"/>\n\
</map>' > "$dir/prefs.xml" \
  && cat "$dir/prefs.xml"

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
