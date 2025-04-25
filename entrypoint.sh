#!/bin/bash
set -euo pipefail

# Setup data directory
mkdir -p /data
[[ -f "$DENO_KV_SQLITE_PATH" ]] || litestream restore --if-replica-exists /data/denokv.sqlite3

# Start Litestream replication and apps
litestream replicate &
LITESTREAM_PID=$!

denokv serve &
DENO_PID=$!

tvbot &
TVBOT_PID=$!

# Forward termination signals to all children
trap 'kill -TERM $LITESTREAM_PID $DENO_PID $TVBOT_PID' SIGTERM SIGINT

# Wait for any process to exit, then cleanup
wait -n $LITESTREAM_PID $DENO_PID $TVBOT_PID
kill $LITESTREAM_PID $DENO_PID $TVBOT_PID 2>/dev/null
exit 1
