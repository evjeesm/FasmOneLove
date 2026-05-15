FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    fasm \
    netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

COPY test_bwrap.sh /test_bwrap.sh
COPY server.sh /server.sh
COPY tests /tests

RUN chmod +x /test_bwrap.sh /server.sh




EXPOSE 8080
CMD ["/server.sh"]
