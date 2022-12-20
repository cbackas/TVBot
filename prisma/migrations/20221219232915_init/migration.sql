-- CreateTable
CREATE TABLE "Settings" (
    "key" TEXT NOT NULL PRIMARY KEY,
    "value" TEXT NOT NULL
);

-- CreateTable
CREATE TABLE "Episode" (
    "showId" TEXT NOT NULL,
    "season" INTEGER NOT NULL,
    "number" INTEGER NOT NULL,
    "title" TEXT NOT NULL,
    "airDate" DATETIME NOT NULL,
    "messageSent" BOOLEAN NOT NULL DEFAULT false,

    PRIMARY KEY ("showId", "season", "number"),
    CONSTRAINT "Episode_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateTable
CREATE TABLE "Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "imdbId" TEXT NOT NULL,
    "tvdbId" INTEGER NOT NULL
);

-- CreateTable
CREATE TABLE "ShowDestination" (
    "showId" TEXT NOT NULL,
    "channelId" TEXT NOT NULL,
    "forumId" TEXT,
    "channelType" TEXT NOT NULL,

    PRIMARY KEY ("showId", "channelId"),
    CONSTRAINT "ShowDestination_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- CreateIndex
CREATE UNIQUE INDEX "Show_imdbId_key" ON "Show"("imdbId");

-- CreateIndex
CREATE UNIQUE INDEX "Show_tvdbId_key" ON "Show"("tvdbId");
