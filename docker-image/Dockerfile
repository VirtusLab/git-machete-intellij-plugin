# syntax=docker/dockerfile:experimental

FROM debian:buster-slim

WORKDIR /app

# Creating of this man directory i a workaround for error in debian-slim while openjdk installation
RUN mkdir -p /usr/share/man/man1 && \
    apt-get update && \
    apt-get install -y git openssh-client python3 python3-pip openjdk-11-jre && \
    pip3 install git-machete==2.12.5 && \
    apt-get autoremove -y python3-pip

#Creating gradle cache
RUN --mount=type=bind,source=.,target=/original_repo /original_repo/docker-image/prepare-image.sh

CMD ["bash"]
