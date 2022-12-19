-- AlterTable
ALTER TABLE "Show" ADD COLUMN "channelShowId" TEXT;

-- CreateTable
CREATE TABLE "Channel" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "channelId" TEXT NOT NULL
);

-- CreateTable
CREATE TABLE "ChannelShow" (
    "channelId" TEXT NOT NULL,
    "showId" TEXT NOT NULL,

    PRIMARY KEY ("channelId", "showId"),
    CONSTRAINT "ChannelShow_channelId_fkey" FOREIGN KEY ("channelId") REFERENCES "Channel" ("id") ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT "ChannelShow_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);
