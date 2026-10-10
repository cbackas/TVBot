import {
  ActionRowBuilder,
  type APIEmbed,
  ButtonBuilder,
  ButtonStyle,
  type ChatInputCommandInteraction,
  ContainerBuilder,
  type Message,
  MessageFlags,
  SectionBuilder,
  SeparatorBuilder,
  SeparatorSpacingSize,
  StringSelectMenuBuilder,
  StringSelectMenuOptionBuilder,
  TextDisplayBuilder,
  ThumbnailBuilder,
} from "discord.js"

const PREVIOUS_ID = "carousel_previous"
const NEXT_ID = "carousel_next"
const SELECT_ID = "carousel_select"

/** how long the controls keep working after the reply is sent */
const TIMEOUT_MS = 10 * 60 * 1000

/** discord allows at most 25 options in a select menu */
const MAX_PAGES = 25

/** discord allows 4000 characters of text per message, this leaves room for the rest */
const MAX_DESCRIPTION_LENGTH = 2000

export interface CarouselPage {
  /** shown in the dropdown, discord limits this to 100 characters */
  label: string
  /** shown under the label in the dropdown, discord limits this to 100 characters */
  description?: string
  embed: APIEmbed
}

function buildSelect(
  pages: CarouselPage[],
  index: number,
  disabled: boolean,
): ActionRowBuilder<StringSelectMenuBuilder> {
  return new ActionRowBuilder<StringSelectMenuBuilder>().addComponents(
    new StringSelectMenuBuilder()
      .setCustomId(SELECT_ID)
      .setPlaceholder("Choose a result")
      .setDisabled(disabled)
      .addOptions(
        pages.map((page, i) => {
          const option = new StringSelectMenuOptionBuilder()
            .setLabel(page.label.slice(0, 100))
            .setValue(String(i))
            // the selected option is what the dropdown displays
            .setDefault(i === index)
          if (page.description != null && page.description !== "") {
            option.setDescription(page.description.slice(0, 100))
          }
          return option
        }),
      ),
  )
}

function buildButtons(
  index: number,
  count: number,
  disabled: boolean,
): ActionRowBuilder<ButtonBuilder> {
  return new ActionRowBuilder<ButtonBuilder>().addComponents(
    new ButtonBuilder()
      .setCustomId(PREVIOUS_ID)
      .setLabel("◀ Previous")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabled),
    // not clickable, only shows the position
    new ButtonBuilder()
      .setCustomId("carousel_position")
      .setLabel(`${index + 1} / ${count}`)
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(true),
    new ButtonBuilder()
      .setCustomId(NEXT_ID)
      .setLabel("Next ▶")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabled),
  )
}

/**
 * Lays an embed out as a container, with the dropdown above it where the title was
 * Embeds can't be combined with the layout components needed to put the dropdown on top
 * The embed title is dropped, the dropdown shows it instead
 */
function buildContainer(
  pages: CarouselPage[],
  index: number,
  disabled: boolean,
): ContainerBuilder {
  const { embed } = pages[index]
  const container = new ContainerBuilder()

  if (embed.color != null) container.setAccentColor(embed.color)

  container.addActionRowComponents(buildSelect(pages, index, disabled))

  if (embed.description != null && embed.description !== "") {
    const description = embed.description.length > MAX_DESCRIPTION_LENGTH
      ? embed.description.slice(0, MAX_DESCRIPTION_LENGTH - 1) + "…"
      : embed.description
    const text = new TextDisplayBuilder().setContent(description)

    if (embed.thumbnail?.url != null) {
      container.addSectionComponents(
        new SectionBuilder()
          .addTextDisplayComponents(text)
          .setThumbnailAccessory(
            new ThumbnailBuilder().setURL(embed.thumbnail.url),
          ),
      )
    } else {
      container.addTextDisplayComponents(text)
    }
  }

  if (embed.fields != null && embed.fields.length > 0) {
    container
      .addSeparatorComponents(
        new SeparatorBuilder().setSpacing(SeparatorSpacingSize.Small),
      )
      .addTextDisplayComponents(
        new TextDisplayBuilder().setContent(
          embed.fields
            .map((f) => `**${f.name}**: ${f.value.replaceAll("\n", " • ")}`)
            .join("\n"),
        ),
      )
  }

  container
    .addSeparatorComponents(
      new SeparatorBuilder().setSpacing(SeparatorSpacingSize.Small),
    )
    .addActionRowComponents(buildButtons(index, pages.length, disabled))

  if (embed.footer?.text != null) {
    container.addTextDisplayComponents(
      // small text, like an embed footer
      new TextDisplayBuilder().setContent(`-# ${embed.footer.text}`),
    )
  }

  return container
}

/**
 * Replies to a deferred command with a private card: one show at a time
 * with a dropdown above it to jump to any page, and previous and next buttons
 * that wrap around at either end
 * The controls stop working after TIMEOUT_MS or when the bot restarts
 * With a single page the embed is sent as it is, without controls
 * @param interaction deferred command interaction to reply to
 * @param allPages the pages in order, only the first MAX_PAGES are used
 */
export async function sendCarousel(
  interaction: ChatInputCommandInteraction,
  allPages: CarouselPage[],
): Promise<Message> {
  const pages = allPages.slice(0, MAX_PAGES)
  const count = pages.length

  if (count === 1) {
    return await interaction.editReply({ embeds: [pages[0].embed] })
  }

  let index = 0

  // a message with layout components can't have embeds or content
  const buildPage = (disabled = false) => ({
    components: [buildContainer(pages, index, disabled)],
    flags: MessageFlags.IsComponentsV2 as const,
  })

  const message = await interaction.editReply(buildPage())

  const collector = message.createMessageComponentCollector({
    time: TIMEOUT_MS,
    filter: (i) => i.user.id === interaction.user.id,
  })

  collector.on("collect", async (componentInteraction) => {
    try {
      if (componentInteraction.isStringSelectMenu()) {
        const selected = Number(componentInteraction.values[0])
        if (Number.isInteger(selected) && selected >= 0 && selected < count) {
          index = selected
        }
      } else if (componentInteraction.customId === NEXT_ID) {
        index = (index + 1) % count
      } else if (componentInteraction.customId === PREVIOUS_ID) {
        index = (index - 1 + count) % count
      }

      await componentInteraction.update(buildPage())
    } catch (error) {
      console.error("Error changing carousel page:", error)
    }
  })

  collector.on("end", async () => {
    try {
      await interaction.editReply(buildPage(true))
    } catch (error) {
      // the reply may have been dismissed or the interaction token expired
      console.debug("Could not disable carousel controls:", error)
    }
  })

  return message
}
