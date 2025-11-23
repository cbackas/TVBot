FROM denoland/deno:debian
WORKDIR /app
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    openssl \
    && \
    rm -rf /var/lib/apt/lists/*
COPY . .
RUN deno task prisma:generate
RUN deno install -r --allow-scripts=npm:@prisma/client,npm:prisma,npm:@prisma/engines
ENV TZ="America/Chicago"
ENTRYPOINT ["sh", "entrypoint.sh"]
