import {
  type AnySelectMenuInteraction,
  type AutocompleteInteraction,
  type ChatInputCommandInteraction,
  Collection,
  type Interaction,
  REST,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
  type RESTPostAPIContextMenuApplicationCommandsJSONBody,
  Routes,
  type SlashCommandBuilder,
} from "npm:discord.js"
import { type CommandV2 } from "interfaces/command.ts"
import { getEnv } from "lib/env.ts"

interface Getter<TInput> {
  command: TInput
}

/**
 * all commands required here will be registered on app startup
 */
const commandModules: Array<Getter<CommandV2>> = [
  await import("commands/post.ts"),
  await import("commands/link.ts"),
  await import("commands/unlink.ts"),
  await import("commands/list.ts"),
  await import("commands/search.ts"),
  await import("commands/upcoming.ts"),
  await import("commands/setting.ts"),
]

export class CommandManager {
  private readonly commands = new Collection<string, CommandV2>()

  private readonly clientId: string
  private readonly token: string
  private readonly guildId: string

  constructor(clientId: string, token: string, guildId: string) {
    this.clientId = clientId
    this.token = token
    this.guildId = guildId
  }

  /**
   * Register all commands with Discord
   */
  public registerCommands = async (): Promise<void> => {
    type SlashCommandData =
      | RESTPostAPIChatInputApplicationCommandsJSONBody
      | RESTPostAPIContextMenuApplicationCommandsJSONBody
    const slashCommandData: SlashCommandData[] = []

    // loop through command modules
    // build command collection and slash command object used for discord command registration
    for (const module of commandModules) {
      const command = module.command
      this.commands.set(command.slashCommand.main.name, command)
      const slashCommand = buildSlashCommand(command.slashCommand)
      slashCommandData.push(slashCommand.toJSON())
    }

    // when testing locally you dont always need to register commands
    if (getEnv("REGISTER_COMMANDS") === false) return

    try {
      console.log("Starting to register slash commands")
      const rest = new REST({ version: "10" }).setToken(this.token)
      await rest.put(
        Routes.applicationGuildCommands(this.clientId, this.guildId),
        { body: slashCommandData },
      )
      console.log(
        "Slash commands registered:\n" +
          this.commands.map((cmd) => cmd.slashCommand.main.name).join("\n"),
      )
    } catch (error) {
      console.error(error)
    }
  }

  public commandInteractionHandler = async (
    interaction: ChatInputCommandInteraction,
  ): Promise<void> => {
    const command = this.commands.get(interaction.commandName)
    if (command === undefined) return

    // checks if the recieved has exactly the same subcommand and/or subcommand group as the command
    const subcommands = command.slashCommand.subCommands
    const subgroups = command.slashCommand.subGroups
    const hasSubCommands = subcommands === undefined ||
      subcommands?.some((subcommand) =>
        subcommand.name === interaction.options.getSubcommand()
      )
    const hasSubGroups = subgroups == null ||
      subgroups?.some((subgroup) =>
        subgroup.main.name === interaction.options.getSubcommandGroup()
      )
    const hasSubgroupSubCommands = hasSubGroups ||
      subgroups?.some((subgroup) =>
        subgroup.subCommands.some((subcommand) =>
          subcommand.name === interaction.options.getSubcommand()
        )
      )
    if (!hasSubCommands && !hasSubGroups && !hasSubgroupSubCommands) return

    console.debug(
      `[Command Recieved] ${command.slashCommand.main.name} - ${interaction.user.username}#${interaction.user.discriminator}`,
    )

    await interaction.deferReply({ ephemeral: true })

    try {
      await command.executeCommand(interaction)
    } catch (error) {
      console.error(error)
      await interaction.editReply(
        "There was an error while executing this command!",
      )
    }
  }

  public selectMenuInteractionHandler = async (
    interaction: AnySelectMenuInteraction,
  ): Promise<void> => {
    const command = this.commands.find((c) =>
      c.selectMenuIds?.includes(interaction.customId)
    )
    if (command == null || command.executeSelectMenu == null) return

    await interaction.deferUpdate()

    try {
      await command.executeSelectMenu(interaction)
    } catch (error) {
      console.error(error)
      await interaction.editReply(
        "There was an error while executing this command!",
      )
    }
  }

  public autocompleteInteractionHandler = async (
    interaction: AutocompleteInteraction,
  ): Promise<void> => {
    const command = this.commands.get(interaction.commandName)
    if (command == null || command.executeAutoComplate == null) return

    try {
      await command.executeAutoComplate(interaction)
    } catch (e) {
      console.error(e)
    }
  }

  /**
   * All discord interactions come through here, this function will route them to the correct handler
   * @param interaction discord interaction
   */
  public interactionHandler = async (
    interaction: Interaction,
  ): Promise<void> => {
    if (interaction.isChatInputCommand()) {
      await this.commandInteractionHandler(interaction)
      return
    }
    if (interaction.isAnySelectMenu()) {
      await this.selectMenuInteractionHandler(interaction)
      return
    }
    if (interaction.isAutocomplete()) {
      await this.autocompleteInteractionHandler(interaction)
    }
  }
}

/**
 * Build a slash command from weird customer slash command wrapper object
 * @param slashCommand Custom command SlashCommand object that is used to build the discord.js SlashCommandBuilder
 * @returns regular SlashCommandBuilder
 */
function buildSlashCommand(
  slashCommand: CommandV2["slashCommand"],
): SlashCommandBuilder {
  let builtCommand = slashCommand.main as SlashCommandBuilder
  // adds subGroups to the slash command
  const subGroups = slashCommand.subGroups
  builtCommand = (subGroups ?? []).reduce((accSubGroups, currSubGroup) => {
    const subGroupCommands = currSubGroup.subCommands
    const fullSubgroup = subGroupCommands.reduce(
      (accSubGroupCommands, currSubCommand) => {
        accSubGroupCommands.addSubcommand(currSubCommand)
        return accSubGroupCommands
      },
      currSubGroup.main,
    )

    accSubGroups.addSubcommandGroup(fullSubgroup)
    return accSubGroups
  }, builtCommand)

  // adds subCommands to the slash command
  const subCommands = slashCommand.subCommands
  builtCommand = (subCommands ?? []).reduce((acc, currSubCommand) => {
    acc.addSubcommand(currSubCommand)
    return acc
  }, builtCommand)

  return builtCommand
}
