FROM golang:1.18.8-alpine3.17 as builder

RUN apk add --no-cache git
RUN go get -u github.com/tsenart/deadcode

FROM amazoncorretto:8-alpine3.17-jre

RUN apk add bash

COPY --from=builder /go/bin/deadcode /app/deadcode
COPY src/main/resources/docs /docs
COPY target/universal/stage/ /opt/docker/

RUN adduser -u 2004 -D docker && chmod +x /opt/docker/bin/codacy-deadcode

USER docker

WORKDIR /src
ENTRYPOINT ["/opt/docker/bin/codacy-deadcode"]
