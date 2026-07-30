CREATE TABLE `episodes` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`show_id` integer NOT NULL,
	`season` integer NOT NULL,
	`number` integer NOT NULL,
	`title` text DEFAULT '' NOT NULL,
	`air_date` text NOT NULL,
	`message_sent` integer DEFAULT false NOT NULL,
	CONSTRAINT `fk_episodes_show_id_shows_id_fk` FOREIGN KEY (`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE,
	CONSTRAINT `episodes_show_id_season_number_unique` UNIQUE(`show_id`,`season`,`number`)
);
--> statement-breakpoint
CREATE TABLE `global_destinations` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`channel_id` text NOT NULL,
	`type` text NOT NULL,
	CONSTRAINT `global_destinations_channel_id_type_unique` UNIQUE(`channel_id`,`type`)
);
--> statement-breakpoint
CREATE TABLE `show_destinations` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`show_id` integer NOT NULL,
	`channel_id` text NOT NULL,
	`forum_id` text,
	CONSTRAINT `fk_show_destinations_show_id_shows_id_fk` FOREIGN KEY (`show_id`) REFERENCES `shows`(`id`) ON DELETE CASCADE,
	CONSTRAINT `show_destinations_show_id_channel_id_unique` UNIQUE(`show_id`,`channel_id`)
);
--> statement-breakpoint
CREATE TABLE `shows` (
	`id` integer PRIMARY KEY AUTOINCREMENT,
	`imdb_id` text NOT NULL UNIQUE,
	`tvdb_id` integer NOT NULL,
	`name` text NOT NULL
);
--> statement-breakpoint
CREATE INDEX `idx_episodes_show_id` ON `episodes` (`show_id`);--> statement-breakpoint
CREATE INDEX `idx_episodes_air_date` ON `episodes` (`air_date`);--> statement-breakpoint
CREATE INDEX `idx_episodes_message_sent` ON `episodes` (`message_sent`);--> statement-breakpoint
CREATE INDEX `idx_global_destinations_type` ON `global_destinations` (`type`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_show_id` ON `show_destinations` (`show_id`);--> statement-breakpoint
CREATE INDEX `idx_show_destinations_channel_id` ON `show_destinations` (`channel_id`);