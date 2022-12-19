/*
  Warnings:

  - A unique constraint covering the columns `[imdbId]` on the table `Show` will be added. If there are existing duplicate values, this will fail.

*/
-- CreateIndex
CREATE UNIQUE INDEX "Show_imdbId_key" ON "Show"("imdbId");
