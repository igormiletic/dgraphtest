FROM anapsix/alpine-java

ENV APP_DIR /usr/local/graphtest/

RUN mkdir -p ${APP_DIR}

WORKDIR ${APP_DIR}

COPY target/universal/version.properties ${APP_DIR}

COPY target/universal/dgraphtest*.zip ${APP_DIR}

RUN unzip $(cat version.properties).zip

RUN apk add --no-cache tini

ENTRYPOINT ["/sbin/tini", "--"]

CMD ls $(cat version.properties)

CMD $(cat version.properties)/bin/dgraphtest

## create user
RUN addgroup -g 35432 graphino
RUN adduser -D -H -u 35432 -G graphino graphino
USER graphino

EXPOSE 9090
