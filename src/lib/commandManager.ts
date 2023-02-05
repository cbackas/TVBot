import { AnySelectMenuInteraction, AutocompleteInteraction, ChatInputCommandInteraction, Collection, Interaction, REST, RESTPostAPIChatInputApplicationCommandsJSONBody, RESTPostAPIContextMenuApplicationCommandsJSONBody, Routes, SlashCommandBuilder } from "discord.js"
import { App } from "../app"
import { CommandV2 } from "../interfaces/command"

type Getter<TInput> = { command: TInput }

/**
 * all commands required here will be registered on app startup
 */
const commandModules: Getter<CommandV2>[] = [
  require('../commands/post'),
  require('../commands/link'),
  require('../commands/unlink'),
  require('../commands/list'),
  require('../commands/search'),
  require('../commands/upcoming'),
  require('../commands/setting')
]

export class CommandManager {
  private commands = new Collection<string, CommandV2>()

  private app: App
  private clientId: string
  private token: string
  private guildId: string

  constructor(app: App, clientId: string, token: string, guildId: string) {
    this.app = app
    this.clientId = clientId
    this.token = token
    this.guildId = guildId
  }

  /**
   * Register all commands with Discord
   */
  public registerCommands = async (): Promise<void> => {
    type SlashCommandData = RESTPostAPIChatInputApplicationCommandsJSONBody | RESTPostAPIContextMenuApplicationCommandsJSONBody
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
    if (process.env.REGISTER_COMMANDS === 'false') return

    try {
      console.log('Starting to register slash commands')
      const rest = new REST({ version: '10' }).setToken(this.token)
      await rest.put(Routes.applicationGuildCommands(this.clientId, this.guildId), { body: slashCommandData })
      console.log('Slash commands registered: ' + this.commands.map((cmd) => cmd.slashCommand.main.name))
    } catch (error) {
      console.error(error)
    }
  }

  /**
   * All discord interactions come through here, this function will route them to the correct handler
   * @param interaction discord interaction
   */
  public interactionHandler = async (interaction: Interaction) => {
    if (interaction.isChatInputCommand()) return await this.commandInteractionHandler(interaction)
    if (interaction.isAnySelectMenu()) return await this.selectMenuInteractionHandler(interaction)
    if (interaction.isAutocomplete()) return await this.autocompleteInteractionHandler(interaction)
  }

  private commandInteractionHandler = async (interaction: ChatInputCommandInteraction) => {
    const command = this.commands.get(interaction.commandName)
    if (command === undefined) return

    // checks if the recieved has exactly the same subcommand and/or subcommand group as the command
    const subcommands = command.slashCommand.subCommands
    const subgroups = command.slashCommand.subGroups
    const hasSubCommands = subcommands === undefined || subcommands?.some((subcommand) => subcommand.name === interaction.options.getSubcommand())
    const hasSubGroups = subgroups == undefined || subgroups?.some((subgroup) => subgroup.main.name === interaction.options.getSubcommandGroup())
    const hasSubgroupSubCommands = hasSubGroups || subgroups?.some((subgroup) => subgroup.subCommands.some((subcommand) => subcommand.name === interaction.options.getSubcommand()))
    if (!hasSubCommands && !hasSubGroups && !hasSubgroupSubCommands) return

    console.log(`Recieved Command: ${command.slashCommand.main.name}`)

    await interaction.deferReply({ ephemeral: true })

    try {
      await command.execute(this.app, interaction)
    } catch (error) {
      console.error(error)
      await interaction.editReply('There was an error while executing this command!')
    }
  }

  private selectMenuInteractionHandler = async (interaction: AnySelectMenuInteraction) => {
    const command = this.commands.find(c => c.selectMenuIds?.includes(interaction.customId))
    if (command !== undefined) {
      await interaction.deferUpdate()

      try {
        command.execute(this.app, interaction)
      } catch (error) {
        console.error(error)
        await interaction.editReply('There was an error while executing this command!')
      }
    }
  }

  private autocompleteInteractionHandler = async (interaction: AutocompleteInteraction) => {
    const command = this.commands.get(interaction.commandName)
    if (command === undefined || command.autocomplete === undefined) return

    try {
      await command.autocomplete(this.app, interaction)
    } catch (e) {
      console.error(e)
    }
  }
}

/**
 * Build a slash command from weird customer slash command wrapper object
 * @param slashCommand Custom command SlashCommand object that is used to build the discord.js SlashCommandBuilder
 * @returns regular SlashCommandBuilder
 */
const buildSlashCommand = (slashCommand: CommandV2['slashCommand']): SlashCommandBuilder => {
  let builtCommand = slashCommand.main as SlashCommandBuilder
  // adds subGroups to the slash command
  const subGroups = slashCommand.subGroups
  builtCommand = (subGroups ?? []).reduce((accSubGroups, currSubGroup) => {
    const subGroupCommands = currSubGroup.subCommands
    const fullSubgroup = subGroupCommands.reduce((accSubGroupCommands, currSubCommand) => {
      accSubGroupCommands.addSubcommand(currSubCommand)
      return accSubGroupCommands
    }, currSubGroup.main)

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
