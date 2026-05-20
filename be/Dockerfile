# Build stage
FROM golang:1.25 AS builder
WORKDIR /app
COPY . .
RUN go mod tidy
RUN go build -o server .
RUN go build -o runner ./runner/

# Runtime stage
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    ca-certificates \
    fasm \
    && rm -rf /var/lib/apt/lists/*

# Copy Go binary
COPY --from=builder /app/server /server
COPY --from=builder /app/runner/runner /runner

EXPOSE 8080
CMD ["/server"]
