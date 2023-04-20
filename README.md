# TVBot

TVBot is a Discord bot that allows scheduling TV notifications to Discord channels or forums. It's powered by the TVDB API and is written in TypeScript. The bot helps users manage TV show notifications and announcements. It supports a variety of commands to search for shows, link and unlink shows to channels, create forum posts, and display upcoming episodes.

## Features

- Schedule TV notifications to Discord channels or forums
- Search for shows using the TVDB API
- Link and unlink shows to channels
- Create forum posts for shows
- Display upcoming episodes

## Commands

| Command                     | Description                                          | Options                                                                                                                                                                                                                                        |
|-----------------------------|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/post`                     | Create a forum post for a show                       | - `imdb_id` (required): The IMDB ID to search for.<br>- `forum` (optional): Destination Discord forum for show post (defaults to the value defined in `/setting tv_forum`).                                                                   |
| `/link here`               | Link a show to the current channel for notifications | - `imdb_id` (required): The IMDB ID to search for.                                                                                                                                                                                             |
| `/link channel`            | Link a show to a channel for notifications           | - `channel` (required): The channel to announce episodes in.<br>- `imdb_id` (required): The IMDB ID to search for.                                                                                                                             |
| `/unlink here`             | Unlink shows from the current channel for notifications |                                                                                                                                                                                                                                                |
| `/unlink channel`          | Unlink shows from a channel for notifications        | - `channel` (required): The channel to unlink from announcements.                                                                                                                                                                              |
| `/list shows here`         | List shows linked to the current channel             |                                                                                                                                                                                                                                                |
| `/list shows channel`      | List shows linked to a specific channel              | - `channel` (required): Channel to list shows from.                                                                                                                                                                                            |
| `/search`                  | Search for a show by IMDB ID or name                 | - `query` (required): Query to search for. Can be an IMDB ID or a show name.                                                                                                                                                                   |
| `/upcoming all`            | Get upcoming episodes this week for all tracked shows |                                                                                                                                                                                                                                                |
| `/upcoming here`           | Get upcoming episodes for this channel               |                                                                                                                                                                                                                                                |
| `/upcoming show`           | Get upcoming episodes for a show                     | - `query` (optional): Search for a show saved in the DB. Use the autocomplete!                                                                                                                                                                 |
| `/setting all_episodes add`      | Add a channel to the list that receives all episode notifications | - `channel` (required): Channel to add to the list that receives all episode notifications.                                                                                                                                                    |
| `/setting all_episodes remove`   | Remove a channel from the list that receives all episode notifications | - `channel` (required): Channel to remove from the list that receives all episode notifications.                                                                                                                                                |
| `/setting morning_summary add_channel` | Add a channel to the list that receives the morning summary message | - `channel` (required): Channel to add to the list that receives the morning summary message.                                                                                                                                                 |
| `/setting morning_summary remove_channel` | Remove a channel from the list that receives the morning summary message | - `channel` (required): Channel to remove from the list that receives the morning summary message.                                                                                                                                             |
| `/setting tv_forum`        | Set the forum ID for TV shows                        | - `channel` (required): The channel to set as the TV forum.                                                                                                                                                                                    |

