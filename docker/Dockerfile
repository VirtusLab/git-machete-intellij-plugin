
# Not using openjdk:11-jdk-slim-... due to https://github.com/AdoptOpenJDK/openjdk-docker/issues/75
FROM debian:11-slim

SHELL ["/bin/bash", "-euxo", "pipefail", "-c"]

RUN \
  `# workaround for error in debian-slim during openjdk installation` \
  mkdir -p /usr/share/man/man1; \
  apt-get update; \
  `# installing JDK and not just JRE to provide javadoc executable` \
  apt-get install --no-install-recommends -y curl git openjdk-11-jdk-headless openssh-client python3-pip; \
  rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

ARG hub_version=2.14.2
# Install hub (GitHub CLI); Debian Buster package has an ancient version (e.g. `hub pr show` isn't supported there yet)
RUN \
  curl -Lsf https://github.com/github/hub/releases/download/v${hub_version}/hub-linux-amd64-${hub_version}.tgz -o hub.tgz; \
  tar --directory=/usr/local/bin/ --strip-components=2 -xvzf hub.tgz hub-linux-amd64-${hub_version}/bin/hub; \
  rm hub.tgz; \
  hub --version

# Disable IntelliJ data sharing
RUN \
  dir=/root/.local/share/JetBrains/consentOptions; \
  mkdir -p "$dir"; \
  echo -n "rsch.send.usage.stat:1.1:0:$(date +%s)000" > "$dir/accepted"
# Note that if we were to run `./gradlew runIdeForUiTests`,
# we'd need to populate ~/.java/.userPrefs/jetbrains/.../prefs.xml to accept End User Agreement/privacy policy.
# But in our setup, it's sorted out by ide-probe instead (org.virtuslab.ideprobe.ide.intellij.IntellijPrivacyPolicy).

# Tools necessary to run non-headless UI tests in the screen-less environment of CI
RUN \
  apt-get update; \
  apt-get install --no-install-recommends -y libxtst6 xvfb xauth x11-apps imagemagick zip; \
  `# libraries needed by JCEF` \
  apt-get install --no-install-recommends -y libatk1.0-0 libatk-bridge2.0-0 libgbm1 libglib2.0 libxcomposite1 libxkbcommon0 libxrandr2; \
  rm -rf /var/lib/apt/lists/*

# Secondary packages needed in just one (or few) steps of the pipeline:
# (package       => needed for command(s))
# binutils       => strings
# jq             => jq
# nodejs         => npm (Docker build only), remark
# procps         => kill (as a standalone command and not shell built-in, to be executed by ide-probe)
# sudo           => sudo
# xxd            => xxd
# unzip          => zipinfo
RUN \
  apt-get update; \
  curl -fsSL https://deb.nodesource.com/setup_16.x | bash -; \
  apt-get install --no-install-recommends -y binutils jq nodejs procps sudo xxd unzip; \
  rm -rf /var/lib/apt/lists/*

# Markdown validation utilities
RUN \
  npm install --location=global remark-cli remark-lint-no-dead-urls remark-validate-links; \
  npm uninstall --location=global npm

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
