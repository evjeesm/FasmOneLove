# Build stage
FROM golang:1.22 AS builder
WORKDIR /app
COPY main.go .
RUN go build -o server .

# Runtime stage
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    fasm
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Copy Go binary
COPY --from=builder /app/server /server

EXPOSE 8080
CMD ["/server"]
