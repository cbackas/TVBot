/*
  Warnings:

  - You are about to drop the `Channel` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the `ChannelShow` table. If the table is not empty, all the data it contains will be lost.
  - You are about to drop the column `channelShowId` on the `Show` table. All the data in the column will be lost.

*/
-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "Channel";
PRAGMA foreign_keys=on;

-- DropTable
PRAGMA foreign_keys=off;
DROP TABLE "ChannelShow";
PRAGMA foreign_keys=on;

-- CreateTable
CREATE TABLE "ForumPost" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "postId" TEXT NOT NULL,
    "forumId" TEXT NOT NULL
);

-- CreateTable
CREATE TABLE "ShowPost" (
    "showId" TEXT NOT NULL,
    "forumPostId" TEXT NOT NULL,

    PRIMARY KEY ("showId", "forumPostId"),
    CONSTRAINT "ShowPost_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT "ShowPost_forumPostId_fkey" FOREIGN KEY ("forumPostId") REFERENCES "ForumPost" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);

-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "imdbId" TEXT NOT NULL,
    "tmdbId" INTEGER NOT NULL,
    "tvdbId" INTEGER NOT NULL
);
INSERT INTO "new_Show" ("id", "imdbId", "name", "tmdbId", "tvdbId") SELECT "id", "imdbId", "name", "tmdbId", "tvdbId" FROM "Show";
DROP TABLE "Show";
ALTER TABLE "new_Show" RENAME TO "Show";
CREATE UNIQUE INDEX "Show_imdbId_key" ON "Show"("imdbId");
CREATE UNIQUE INDEX "Show_tmdbId_key" ON "Show"("tmdbId");
CREATE UNIQUE INDEX "Show_tvdbId_key" ON "Show"("tvdbId");
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
