/*
  Warnings:

  - You are about to drop the column `tmdbid` on the `Show` table. All the data in the column will be lost.
  - You are about to drop the column `tvdbid` on the `Show` table. All the data in the column will be lost.
  - Added the required column `tmdbId` to the `Show` table without a default value. This is not possible if the table is not empty.
  - Added the required column `tvdbId` to the `Show` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "imdbId" TEXT NOT NULL,
    "tmdbId" INTEGER NOT NULL,
    "tvdbId" INTEGER NOT NULL
);
INSERT INTO "new_Show" ("id", "imdbId", "name") SELECT "id", "imdbId", "name" FROM "Show";
DROP TABLE "Show";
ALTER TABLE "new_Show" RENAME TO "Show";
CREATE UNIQUE INDEX "Show_imdbId_key" ON "Show"("imdbId");
CREATE UNIQUE INDEX "Show_tmdbId_key" ON "Show"("tmdbId");
CREATE UNIQUE INDEX "Show_tvdbId_key" ON "Show"("tvdbId");
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
