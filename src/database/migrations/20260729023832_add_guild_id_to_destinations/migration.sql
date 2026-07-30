ALTER TABLE `global_destinations` ADD `guild_id` text DEFAULT '1054158011742044160' NOT NULL;--> statement-breakpoint
ALTER TABLE `show_destinations` ADD `guild_id` text DEFAULT '1054158011742044160' NOT NULL;--> statement-breakpoint
PRAGMA foreign_keys=OFF;--> statement-breakpoint
CREATE TABLE `__new_global_destinations` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`guild_id` text DEFAULT '1054158011742044160' NOT NULL,
	`channel_id` text NOT NULL,
	`type` text NOT NULL,
	CONSTRAINT `global_destinations_guild_id_channel_id_type_unique` UNIQUE(`guild_id`,`channel_id`,`type`)
);
--> statement-breakpoint
INSERT INTO `__new_global_destinations`(`id`, `channel_id`, `type`) SELECT `id`, `channel_id`, `type` FROM `global_destinations`;--> statement-breakpoint
DROP TABLE `global_destinations`;--> statement-breakpoint
ALTER TABLE `__new_global_destinations` RENAME TO `global_destinations`;--> statement-breakpoint
PRAGMA foreign_keys=ON;--> statement-breakpoint
DROP INDEX IF EXISTS `idx_global_destinations_type`;--> statement-breakpoint
CREATE INDEX `idx_global_destinations_guild_type` ON `global_destinations` (`guild_id`,`type`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_guild_id` ON `show_destinations` (`guild_id`);