#!/bin/bash

set -e
# migrate the database
deno run -A --allow-scripts=npm:prisma,npm:@prisma/engines npm:prisma@latest db push
set +e

deno -A --unstable-cron ./src/app.ts
