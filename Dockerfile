FROM litestream/litestream AS litestream
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
COPY entrypoint.sh /usr/local/bin/entrypoint
COPY litestream.yml /etc/litestream.yml

COPY --from=build /app/dist/tvbot /usr/local/bin/tvbot

ENV DENO_KV_SQLITE_PATH="/data/denokv.sqlite3"
ENV TZ="America/Chicago"

ENTRYPOINT ["entrypoint"]
