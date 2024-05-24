FROM node:20-slim as build
WORKDIR /build
COPY . .
RUN npm ci
RUN npm run build

FROM node:20-slim as prod
WORKDIR /app
RUN apt-get update && apt-get -y install openssl
RUN npm install pm2 prisma --save-dev && npm install @prisma/client
COPY --from=build /build/prisma ./prisma
COPY --from=build /build/dist .
COPY --from=build /build/entrypoint.sh .
ENV TZ="America/Chicago"
RUN npx prisma generate
ENTRYPOINT ["sh", "entrypoint.sh"]
