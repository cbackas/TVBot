import { createSelectSchema } from "drizzle-orm/zod";
import * as z from "zod";
import {
  episodes,
  globalDestinations,
  showDestinations,
  shows,
} from "./schema.js";

export const episodeSchema = createSelectSchema(episodes, {
  airDate: z.coerce.date(),
});
export type Episode = z.infer<typeof episodeSchema>;

export const destinationSchema = createSelectSchema(showDestinations);
export type Destination = z.infer<typeof destinationSchema>;

export const globalDestinationSchema = createSelectSchema(globalDestinations);
export type GlobalDestination = z.infer<typeof globalDestinationSchema>;

export const showSchema = createSelectSchema(shows).extend({
  episodes: z.array(episodeSchema),
  destinations: z.array(destinationSchema),
});
export type Show = z.infer<typeof showSchema>;
