FROM litestream/litestream AS litestream
FROM ghcr.io/denoland/denokv:latest AS denokv

FROM denoland/deno:debian AS build
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openssl \
    && \
    rm -rf /var/lib/apt/lists/*
COPY . .
RUN deno install -r --allow-scripts=npm:@prisma/client,npm:prisma,npm:@prisma/engines
RUN deno task prisma:generate
RUN deno task build

FROM debian:latest
COPY --from=litestream /usr/local/bin/litestream /usr/local/bin/litestream
COPY --from=denokv /usr/local/bin/denokv /usr/local/bin/denokv
COPY --from=build /app/dist/tvbot /usr/local/bin/tvbot

COPY litestream.yml /etc/litestream.yml

ENV DENO_KV_SQLITE_PATH="/data/denokv.sqlite3"

WORKDIR /app
COPY entrypoint.sh /app/entrypoint.sh
ENV TZ="America/Chicago"
ENTRYPOINT ["sh", "entrypoint.sh"]
