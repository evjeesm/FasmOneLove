FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    fasm \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY test_fasm.sh /test_fasm.sh
COPY server.sh /server.sh
COPY tests /tests

RUN chmod +x /test_fasm.sh /server.sh




EXPOSE 8080
CMD ["/server.sh"]
