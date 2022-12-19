/*
  Warnings:

  - Added the required column `tmdbid` to the `Show` table without a default value. This is not possible if the table is not empty.
  - Added the required column `tvdbid` to the `Show` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "imdbId" TEXT NOT NULL,
    "tmdbid" TEXT NOT NULL,
    "tvdbid" TEXT NOT NULL
);
INSERT INTO "new_Show" ("id", "imdbId", "name") SELECT "id", "imdbId", "name" FROM "Show";
DROP TABLE "Show";
ALTER TABLE "new_Show" RENAME TO "Show";
CREATE UNIQUE INDEX "Show_imdbId_key" ON "Show"("imdbId");
CREATE UNIQUE INDEX "Show_tmdbid_key" ON "Show"("tmdbid");
CREATE UNIQUE INDEX "Show_tvdbid_key" ON "Show"("tvdbid");
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
