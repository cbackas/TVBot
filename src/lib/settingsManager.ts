import client from "lib/prisma.ts"
import {
  type Destination,
  Prisma,
  type Settings as DBSettings,
} from "prisma-client/client.ts"

export type SettingsType = Omit<DBSettings, "id">

/**
 * Manager to handle fetching and saving settings in the DB
 */
export class Settings {
  private static instance: Settings

  public static getInstance(): Settings {
    if (!Settings.instance) {
      Settings.instance = new Settings()
    }
    return Settings.instance
  }

  // set the defaults for the settings
  private settings?: SettingsType

  /**
   * Save initial settings data to the DB
   */
  private readonly initData = async (): Promise<void> => {
    try {
      await client.settings.create({
        data: {
          id: 0,
          allEpisodes: [],
        },
      })
    } catch (error) {
      if (!(error instanceof Prisma.PrismaClientKnownRequestError)) throw error
      if (error.code !== "P2002") throw error
    }
  }

  /**
   * Fetches the settings from the DB and updates the SettingsManager instance with the latest values
   */
  refresh = async (): Promise<SettingsType | undefined> => {
    try {
      // fetch the settings from the DB
      const settings = await client.settings.findUniqueOrThrow({
        where: {
          id: 0,
        },
      })
      this.settings = settings
      return settings
    } catch (error) {
      if (!(error instanceof Prisma.PrismaClientKnownRequestError)) throw error
      if (error.code === "P2025") await this.initData()
    }
  }

  public static refresh = async (): Promise<SettingsType | undefined> => {
    const instance = Settings.getInstance()
    const settings = await instance.refresh()
    return settings
  }

  /**
   * Update settings in the DB
   * @param inputData settings data to update
   */
  public static update = async (
    inputData: Partial<SettingsType>,
  ): Promise<void> => {
    const data = Prisma.validator<Prisma.SettingsUpdateInput>()(inputData)

    await client.settings.update({
      where: {
        id: 0,
      },
      data,
    })

    await Settings.getInstance().refresh()
  }

  /**
   * check if a channel is already in settings 'allEpisodes' list global destiations list
   * @param channelId channel to check
   * @returns true if channel is in list, false if not
   */
  private readonly channelIsAlreadyGlobal = async (
    channelId: string,
  ): Promise<boolean> => {
    const matchingChannels = await client.settings.count({
      where: {
        id: 0,
        allEpisodes: {
          some: {
            channelId,
          },
        },
      },
    })

    return matchingChannels > 0
  }

  public static addGlobalDestination = async (
    channelId: string,
  ): Promise<Destination[]> => {
    const instance = Settings.getInstance()
    if (await instance.channelIsAlreadyGlobal(channelId)) {
      return instance.settings?.allEpisodes ?? []
    }

    const settings = await client.settings.update({
      where: {
        id: 0,
      },
      data: {
        allEpisodes: {
          push: {
            channelId,
          },
        },
      },
      select: {
        allEpisodes: true,
      },
    })

    console.info(`Added ${channelId} to global destinations`)

    await instance.refresh()
    return settings.allEpisodes
  }

  public static removeGlobalDestination = async (
    channelId: string,
  ): Promise<Destination[]> => {
    const instance = Settings.getInstance()
    if (!await instance.channelIsAlreadyGlobal(channelId)) {
      return instance.settings?.allEpisodes ?? []
    }

    const settings = await client.settings.update({
      where: {
        id: 0,
      },
      data: {
        allEpisodes: {
          deleteMany: {
            where: {
              channelId,
            },
          },
        },
      },
    })

    console.info(`Removed ${channelId} from global destinations`)

    await instance.refresh()
    return settings.allEpisodes
  }

  public static fetch = (): SettingsType | undefined =>
    Settings.getInstance().settings
}
