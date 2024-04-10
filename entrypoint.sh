# /bin/bash

set -e
# migrate the database and erorr if it fails
bunx prisma db push
set +e

# runs 'bun src/app.ts' wrapped in pm2
# pm2 keeps the process alive if it crashed for some reason or something
bunx pm2 start bun --no-daemon --max-memory-restart 1G --restart-delay 1000 -- src/app.ts
