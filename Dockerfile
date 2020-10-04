
# Not using openjdk:11-jdk-slim-buster due to https://github.com/AdoptOpenJDK/openjdk-docker/issues/75
FROM debian:buster-slim

RUN set -x \
  `# workaround for error in debian-slim during openjdk installation` \
  && mkdir -p /usr/share/man/man1 \
  && apt-get update \
  `# installing JDK and not just JRE to provide javadoc executable` \
  && apt-get install --no-install-recommends -y curl git openjdk-11-jdk-headless openssh-client python3-pip \
  && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

ARG hub_version=2.14.2
# Install hub (GitHub CLI); Debian Buster package has an ancient version (e.g. `hub pr show` isn't supported there yet)
RUN set -x \
  && curl -Lsf https://github.com/github/hub/releases/download/v${hub_version}/hub-linux-amd64-${hub_version}.tgz -o hub.tgz \
  && tar --directory=/usr/local/bin/ --strip-components=2 -xvzf hub.tgz hub-linux-amd64-${hub_version}/bin/hub \
  && rm hub.tgz \
  && hub --version

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

# Secondary packages needed in just one (or few) steps of the pipeline:
# (package       => needed for command(s))
# binutils       => strings
# netcat         => nc
# procps         => ps
# psmisc         => fuser
# xxd            => xxd
# unzip          => zipinfo
RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y binutils netcat procps psmisc xxd unzip \
  `# tools necessary to run non-headless UI tests in the screen-less environment of CI` \
  && apt-get install --no-install-recommends -y libx11-6 libxrender1 libxtst6 xauth xvfb \
  && rm -rf /var/lib/apt/lists/*
