FROM debian:11-slim
SHELL ["/bin/bash", "-euxo", "pipefail", "-c"]
RUN \
  apt-get update; \
  apt-get -y install sudo
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
