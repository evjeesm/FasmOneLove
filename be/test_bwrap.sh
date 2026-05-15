#!/bin/bash

RESULT=$(bwrap \
  --ro-bind /usr /usr \
  --ro-bind /bin /bin \
  --ro-bind /lib /lib \
  --tmpfs /tmp \
  --unshare-net \
  --unshare-pid \
  --die-with-parent \
  /bin/echo "bubblewrap_works" 2>&1)

echo "$RESULT"