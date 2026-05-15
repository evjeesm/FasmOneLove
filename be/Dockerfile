FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    bubblewrap \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY test_bwrap.sh /test_bwrap.sh
COPY server.sh /server.sh
RUN chmod +x /test_bwrap.sh /server.sh

EXPOSE 8080
CMD ["/server.sh"]