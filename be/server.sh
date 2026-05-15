#!/bin/bash

while true; do
  echo -e "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\n$(/test_bwrap.sh)" \
  | nc -l -p 8080 -q 1
done