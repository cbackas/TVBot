#!/bin/bash

if ! test -d /data; then
	mkdir /data
fi
if ! test -f "$DENO_KV_SQLITE_PATH"; then
	litestream restore --if-replica-exists /data/denokv.sqlite3
fi

litestream replicate -exec 'tvbot'
