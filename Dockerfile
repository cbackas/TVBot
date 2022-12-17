FROM node:18-alpine as build
WORKDIR /build
COPY . .
RUN npm ci
RUN npm run build

FROM node:18-alpine as prod
RUN npm install -g pm2
WORKDIR /app
COPY --from=build /build/dist .
# runs 'node bundle.mjs' wrapped in pm2
# pm2 keeps the process alive if it crashed for some reason or something
ENTRYPOINT ["npx", "pm2", "start", "node", "--no-daemon", "--max-memory-restart", "1G", "--restart-delay", "1000", "--", "bundle.mjs"]
