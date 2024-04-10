FROM oven/bun:debian
WORKDIR /app
COPY package.json ./package.json
COPY bun.lockb ./bun.lockb
RUN bun run ci

COPY src/ /app/src/

COPY prisma/ /app/prisma/
RUN bunx prisma generate

ENV TZ="America/Chicago"
COPY entrypoint.sh ./entrypoint.sh
ENTRYPOINT ["sh", "entrypoint.sh"]
