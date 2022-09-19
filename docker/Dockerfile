
# Use JDK and not just JRE to provide `javadoc` executable
# Jammy = Ubuntu 22.04 LTS
FROM eclipse-temurin:11-jdk-jammy

SHELL ["/bin/bash", "-euxo", "pipefail", "-c"]

RUN \
  apt-get update; \
  apt-get install --no-install-recommends -y git hub openssh-client \

#Install GitHub Cli
Run \
  curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg \
  && chmod go+r /usr/share/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
  && apt-get update \
  && apt-get install gh -y

# Disable IntelliJ data sharing
RUN \
  dir=/root/.local/share/JetBrains/consentOptions; \
  mkdir -p "$dir"; \
  echo -n "rsch.send.usage.stat:1.1:0:$(date +%s)000" > "$dir/accepted"
# Note that if we were to run `./gradlew runIdeForUiTests`,
# we'd need to populate ~/.java/.userPrefs/jetbrains/.../prefs.xml to accept End User Agreement/privacy policy.
# But in our setup, it's sorted out by ide-probe instead (org.virtuslab.ideprobe.ide.intellij.IntellijPrivacyPolicy).

# Tools necessary to run non-headless UI tests in the screen-less environment of CI
RUN apt-get install --no-install-recommends -y libxi6 libxtst6 x11-apps xauth xvfb

# Tools necessary to run manipulate screenshots from ide-probe
RUN apt-get install --no-install-recommends -y imagemagick zip

# Secondary packages needed in just one (or few) steps of the pipeline:
# (package       => needed for command(s))
# binutils       => strings
# jq             => jq
# sudo           => sudo
# unzip          => zipinfo
# xxd            => xxd
RUN apt-get install --no-install-recommends -y binutils jq sudo unzip xxd

# Markdown validation utilities
RUN \
  curl -sL https://deb.nodesource.com/setup_18.x | bash -; \
  apt-get install -y nodejs; \
  npm install --global remark-cli remark-lint-no-dead-urls remark-validate-links; \
  remark --help

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
