#!/bin/bash

set -e
# migrate the database
deno run -A --allow-scripts=npm:prisma,npm:@prisma/engines npm:prisma db push --skip-generate
set +e

deno -A --unstable-cron ./src/app.ts
