
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
# Note that if we were to run `./gradlew runIdeForUiTests`,
# we'd need to populate /root/.java/.userPrefs/jetbrains/.../prefs.xml to accept End User Agreement/privacy policy.
# But in our setup, it's sorted out by ide-probe instead (org.virtuslab.ideprobe.ide.intellij.IntellijPrivacyPolicy).

# Tools necessary to run non-headless UI tests in the screen-less environment of CI
RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y libx11-6 libxrender1 libxtst6 xauth xvfb \
  && rm -rf /var/lib/apt/lists/*

# Secondary packages needed in just one (or few) steps of the pipeline:
# (package       => needed for command(s))
# binutils       => strings
# nodejs         => npm (Docker build only), remark
# procps         => kill (as a standalone command and not shell built-in, to be executed by ide-probe)
# xxd            => xxd
# unzip          => zipinfo
RUN set -x \
  && apt-get update \
  && apt-get install --no-install-recommends -y binutils nodejs procps xxd unzip \
  && rm -rf /var/lib/apt/lists/*

# Markdown validation utilities
RUN set -x \
  && curl -Lf https://npmjs.org/install.sh | sh \
  && npm install --global remark-cli remark-lint-no-dead-urls remark-validate-links \
  && npm uninstall --global npm
