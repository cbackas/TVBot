# /bin/bash

set -e
# migrate the database and erorr if it fails
npx prisma db push && npx prisma generate
set +e

# runs 'node bundle.mjs' wrapped in pm2
# pm2 keeps the process alive if it crashed for some reason or something
npx pm2 start node --no-daemon --max-memory-restart 1G --restart-delay 1000 -- --enable-source-maps bundle.mjs
