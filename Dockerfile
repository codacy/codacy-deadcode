FROM golang:1.15.3-alpine3.12 as builder

RUN apk add --no-cache git
RUN go get -u github.com/tsenart/deadcode

FROM alpine:3.11

RUN apk add --no-cache openjdk8-jre
COPY --from=builder /go/bin/deadcode /app/deadcode

