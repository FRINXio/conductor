#
# conductor:server - Netflix conductor server
#

# ===========================================================================================================
# 0. Builder stage
# ===========================================================================================================
FROM eclipse-temurin:17-jdk-focal AS builder

LABEL maintainer="Netflix OSS <conductor@netflix.com>"

# Copy the project directly onto the image
COPY . /conductor
WORKDIR /conductor

RUN apt-get update && apt-get install -y git

# Build the server on run
RUN ./gradlew -Prelease.version=2.25.4 build -Dhttp.socketTimeout=60000 -Dhttp.connectionTimeout=60000 -x test

# ===========================================================================================================
# 1. Bin stage
# ===========================================================================================================
FROM eclipse-temurin:17-jre-focal

ARG git_commit=unspecified
LABEL git_commit="${git_commit}"
LABEL maintainer="Netflix OSS <conductor@netflix.com>"
LABEL org.opencontainers.image.source="https://github.com/FRINXio/conductor"

# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs

# Copy the compiled output to new image
COPY --from=builder /conductor/docker/server/bin /app
COPY --from=builder /conductor/docker/server/config /app/config
COPY --from=builder /conductor/server/build/libs/conductor-server-*-boot.jar /app/libs

RUN groupadd -r conductor && \
    useradd -m conductor -r -g conductor && \
    chown -R conductor:conductor /app

USER conductor

# Copy the files for the server into the app folders
RUN chmod +x /app/startup.sh

EXPOSE 8080
EXPOSE 8090

HEALTHCHECK --interval=60s --timeout=30s --retries=10 CMD curl -I -XGET http://localhost:8080/health || exit 1

CMD [ "/app/startup.sh" ]
ENTRYPOINT [ "/bin/sh"]
