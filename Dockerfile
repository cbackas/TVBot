FROM denoland/deno:debian
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openssl \
    && \
    rm -rf /var/lib/apt/lists/*
COPY . .
RUN deno install -r --allow-scripts=npm:@prisma/client,npm:prisma,npm:@prisma/engines
RUN deno run -A --unstable-node-globals npm:prisma generate --no-engine
ENV TZ="America/Chicago"
ENTRYPOINT ["sh", "entrypoint.sh"]
