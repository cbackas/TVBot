#!/bin/bash

set -e
# migrate the database and erorr if it fails
deno run -A --node-modules-dir=auto npm:prisma@latest db push
set +e

deno -A --unstable-cron ./src/app.ts
