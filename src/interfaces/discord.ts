import {
  type AnyThreadChannel,
  type Channel,
  ChannelType,
  type ForumChannel,
} from "discord.js"

export function isForumChannel(channel: Channel): channel is ForumChannel {
  return channel.type === ChannelType.GuildForum
}

export function isThreadChannel(channel: Channel): channel is AnyThreadChannel {
  return channel.type === ChannelType.PublicThread ||
    channel.type === ChannelType.PrivateThread
}
