import { defineRelations } from "drizzle-orm";
import * as schema from "./schema.js";

export const relations = defineRelations(schema, (helpers) => ({
  shows: {
    episodes: helpers.many.episodes(),
    destinations: helpers.many.showDestinations(),
  },
  episodes: {
    show: helpers.one.shows({
      from: helpers.episodes.showId,
      to: helpers.shows.id,
    }),
  },
  showDestinations: {
    show: helpers.one.shows({
      from: helpers.showDestinations.showId,
      to: helpers.shows.id,
    }),
  },
}));
