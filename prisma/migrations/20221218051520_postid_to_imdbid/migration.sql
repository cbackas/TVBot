/*
  Warnings:

  - You are about to drop the column `postId` on the `Show` table. All the data in the column will be lost.
  - Added the required column `imdbId` to the `Show` table without a default value. This is not possible if the table is not empty.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "imdbId" TEXT NOT NULL
);
INSERT INTO "new_Show" ("id", "name") SELECT "id", "name" FROM "Show";
DROP TABLE "Show";
ALTER TABLE "new_Show" RENAME TO "Show";
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
