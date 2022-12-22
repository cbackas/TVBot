/*
  Warnings:

  - You are about to drop the column `channelType` on the `ShowDestination` table. All the data in the column will be lost.
  - You are about to drop the column `forumId` on the `ShowDestination` table. All the data in the column will be lost.

*/
-- RedefineTables
PRAGMA foreign_keys=OFF;
CREATE TABLE "new_ShowDestination" (
    "showId" TEXT NOT NULL,
    "channelId" TEXT NOT NULL,

    PRIMARY KEY ("showId", "channelId"),
    CONSTRAINT "ShowDestination_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);
INSERT INTO "new_ShowDestination" ("channelId", "showId") SELECT "channelId", "showId" FROM "ShowDestination";
DROP TABLE "ShowDestination";
ALTER TABLE "new_ShowDestination" RENAME TO "ShowDestination";
PRAGMA foreign_key_check;
PRAGMA foreign_keys=ON;
