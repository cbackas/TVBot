PRAGMA foreign_keys=OFF;--> statement-breakpoint
CREATE TABLE `__new_global_destinations` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`guild_id` text NOT NULL,
	`channel_id` text NOT NULL,
	`type` text NOT NULL,
	CONSTRAINT `global_destinations_guild_id_channel_id_type_unique` UNIQUE(`guild_id`,`channel_id`,`type`)
);
--> statement-breakpoint
INSERT INTO `__new_global_destinations`(`id`, `guild_id`, `channel_id`, `type`) SELECT `id`, `guild_id`, `channel_id`, `type` FROM `global_destinations`;--> statement-breakpoint
DROP TABLE `global_destinations`;--> statement-breakpoint
ALTER TABLE `__new_global_destinations` RENAME TO `global_destinations`;--> statement-breakpoint
PRAGMA foreign_keys=ON;--> statement-breakpoint
PRAGMA foreign_keys=OFF;--> statement-breakpoint
CREATE TABLE `__new_show_destinations` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`show_id` integer NOT NULL,
	`guild_id` text NOT NULL,
	`channel_id` text NOT NULL,
	`forum_id` text,
	CONSTRAINT `fk_show_destinations_show_id_shows_id_fk` FOREIGN KEY (`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE,
	CONSTRAINT `show_destinations_show_id_channel_id_unique` UNIQUE(`show_id`,`channel_id`)
);
--> statement-breakpoint
INSERT INTO `__new_show_destinations`(`id`, `show_id`, `guild_id`, `channel_id`, `forum_id`) SELECT `id`, `show_id`, `guild_id`, `channel_id`, `forum_id` FROM `show_destinations`;--> statement-breakpoint
DROP TABLE `show_destinations`;--> statement-breakpoint
ALTER TABLE `__new_show_destinations` RENAME TO `show_destinations`;--> statement-breakpoint
PRAGMA foreign_keys=ON;--> statement-breakpoint
CREATE INDEX `idx_global_destinations_guild_type` ON `global_destinations` (`guild_id`,`type`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_show_id` ON `show_destinations` (`show_id`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_channel_id` ON `show_destinations` (`channel_id`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_guild_id` ON `show_destinations` (`guild_id`);