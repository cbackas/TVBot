FROM denoland/deno:debian
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openssl \
    && \
    rm -rf /var/lib/apt/lists/*
COPY . .
RUN deno install -r --node-modules-dir=auto
RUN deno run -A --unstable npm:prisma generate --no-engine
ENV TZ="America/Chicago"
ENTRYPOINT ["sh", "entrypoint.sh"]
