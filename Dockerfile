FROM golang:1.15.3-alpine3.12 as builder

RUN apk add --no-cache git
RUN go get -u github.com/tsenart/deadcode

FROM openjdk:8-jre-alpine

RUN apk add bash

COPY --from=builder /go/bin/deadcode /app/deadcode
COPY src/main/resources/docs /docs
COPY target/universal/stage/ /opt/docker/

RUN adduser -u 2004 -D docker && chmod +x /opt/docker/bin/codacy-deadcode

USER docker

WORKDIR /src
ENTRYPOINT ["/opt/docker/bin/codacy-deadcode"]
