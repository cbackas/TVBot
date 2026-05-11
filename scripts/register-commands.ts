import { commands } from "../src/commands/index.js";

const token = process.env.DISCORD_TOKEN;
const clientId = process.env.DISCORD_CLIENT_ID;
const guildId = process.env.DISCORD_GUILD_ID;

if (!token || !clientId || !guildId) {
  console.error(
    "Missing required env vars: DISCORD_TOKEN, DISCORD_CLIENT_ID, DISCORD_GUILD_ID",
  );
  process.exit(1);
}

const definitions = [...commands.values()].map((cmd) => cmd.definition);

console.log(
  `Registering ${definitions.length} commands to guild ${guildId}...`,
);

const url = `https://discord.com/api/v10/applications/${clientId}/guilds/${guildId}/commands`;

const response = await fetch(url, {
  method: "PUT",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bot ${token}`,
  },
  body: JSON.stringify(definitions),
});

if (!response.ok) {
  const body = await response.text();
  console.error(`Failed to register commands: ${response.status}`);
  console.error(body);
  process.exit(1);
}

const result = await response.json();
console.log(`Successfully registered ${(result as unknown[]).length} commands:`);
for (const cmd of result as { name: string }[]) {
  console.log(`  /${cmd.name}`);
}
