# JFXChess - A Chess Graphical User Interface
# Copyright (C) 2020-2026 Dominik Klein
#
# Reproducible Ubuntu 22.04 LTS (glibc 2.35) builder for the JFXChess AppImage.
# Uses Eclipse Temurin 21 (bundled libjpeg/giflib/libpng/harfbuzz/lcms2) so
# jpackage output does not depend on Ubuntu-only packages like libjpeg-turbo8.

FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

ARG TEMURIN_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13.1%2B2/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz"
ARG TEMURIN_SHA256="bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e"
ARG MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz"
ARG MAVEN_SHA512="a555254d6b53d267965a3404ecb14e53c3827c09c3b94b5678835887ab404556bfaf78dcfe03ba76fa2508649dca8531c74bca4d5846513522404d48e8c4ac8b"
ARG APPIMAGETOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage"
ARG RUNTIME_URL="https://github.com/AppImage/type2-runtime/releases/download/continuous/runtime-x86_64"

# Install build tools, ELF audit utilities, and X11 extension libs from glibc 2.35 base.
RUN apt-get update && apt-get install -y --no-install-recommends \
        binutils \
        ca-certificates \
        curl \
        desktop-file-utils \
        file \
        libxext6 \
        libxi6 \
        libxrender1 \
        libxtst6 \
        patchelf \
        squashfs-tools \
    && rm -rf /var/lib/apt/lists/*

# Install pinned Eclipse Temurin 21 JDK (verifies SHA-256).
RUN curl -fsSL "${TEMURIN_URL}" -o /tmp/temurin21.tar.gz \
    && echo "${TEMURIN_SHA256}  /tmp/temurin21.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/temurin-21 \
    && tar -xzf /tmp/temurin21.tar.gz -C /opt/temurin-21 --strip-components=1 \
    && rm -f /tmp/temurin21.tar.gz

# Install pinned Apache Maven 3.9.9 (Ubuntu 22.04 apt ships 3.6.3 with compiler-plugin 3.1,
# which predates maven.compiler.release=21).
RUN curl -fsSL "${MAVEN_URL}" -o /tmp/maven.tar.gz \
    && echo "${MAVEN_SHA512}  /tmp/maven.tar.gz" | sha512sum -c - \
    && mkdir -p /opt/maven \
    && tar -xzf /tmp/maven.tar.gz -C /opt/maven --strip-components=1 \
    && rm -f /tmp/maven.tar.gz

# Install appimagetool unpacked (runs without FUSE in Docker) and static type2-runtime.
RUN mkdir -p /opt/appimagetool /opt/appimage-runtime \
    && curl -fsSL "${APPIMAGETOOL_URL}" -o /tmp/appimagetool.AppImage \
    && chmod +x /tmp/appimagetool.AppImage \
    && (cd /tmp && /tmp/appimagetool.AppImage --appimage-extract >/dev/null) \
    && mv /tmp/squashfs-root/* /opt/appimagetool/ \
    && rm -rf /tmp/squashfs-root /tmp/appimagetool.AppImage \
    && curl -fsSL "${RUNTIME_URL}" -o /opt/appimage-runtime/runtime-x86_64 \
    && chmod +x /opt/appimage-runtime/runtime-x86_64

ENV JAVA_HOME=/opt/temurin-21
ENV MAVEN_HOME=/opt/maven
ENV PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"

COPY build_inside_docker.sh /usr/local/bin/build_inside_docker.sh
RUN chmod +x /usr/local/bin/build_inside_docker.sh

ENTRYPOINT ["/usr/local/bin/build_inside_docker.sh"]
