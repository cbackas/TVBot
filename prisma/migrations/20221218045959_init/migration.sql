-- CreateTable
CREATE TABLE "Show" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "postId" TEXT NOT NULL
);

-- CreateTable
CREATE TABLE "Episode" (
    "id" TEXT NOT NULL PRIMARY KEY,
    "name" TEXT NOT NULL,
    "showId" TEXT NOT NULL,
    CONSTRAINT "Episode_showId_fkey" FOREIGN KEY ("showId") REFERENCES "Show" ("id") ON DELETE RESTRICT ON UPDATE CASCADE
);
