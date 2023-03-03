import client from './prisma'
import { Prisma, type Settings as DBSettings, type Destination } from '@prisma/client'

export type Settings = Omit<DBSettings, 'id'>

/**
 * Manager to handle fetching and saving settings in the DB
 */
export class SettingsManager {
  // set the defaults for the settings
  private settings?: Settings

  /**
   * Save initial settings data to the DB
   */
  private async initData (): Promise<void> {
    try {
      await client.settings.create({
        data: {
          id: 0,
          allEpisodes: []
        }
      })
    } catch (error) {
      if (!(error instanceof Prisma.PrismaClientKnownRequestError)) throw error
      if (error.code !== 'P2002') throw error
    }
  }

  /**
   * Fetches the settings from the DB and updates the SettingsManager instance with the latest values
   */
  async refresh (): Promise<Settings | undefined> {
    try {
      // fetch the settings from the DB
      const settings = await client.settings.findUniqueOrThrow({
        where: {
          id: 0
        }
      })
      this.settings = settings
      return settings
    } catch (error) {
      if (!(error instanceof Prisma.PrismaClientKnownRequestError)) throw error
      if (error.code === 'P2025') await this.initData()
    }
  }

  /**
   * Update settings in the DB
   * @param inputData settings data to update
   */
  async update (inputData: Partial<Settings>): Promise<void> {
    const data = Prisma.validator<Prisma.SettingsUpdateInput>()(inputData)

    await client.settings.update({
      where: {
        id: 0
      },
      data
    })

    await this.refresh()
  }

  /**
   * check if a channel is already in settings 'allEpisodes' list global destiations list
   * @param channelId channel to check
   * @returns true if channel is in list, false if not
   */
  private async channelIsAlreadyGlobal (channelId: string): Promise<boolean> {
    const matchingChannels = await client.settings.count({
      where: {
        id: 0,
        allEpisodes: {
          some: {
            channelId
          }
        }
      }
    })

    return matchingChannels > 0
  }

  async addGlobalDestination (channelId: string): Promise<Destination[]> {
    if (await this.channelIsAlreadyGlobal(channelId)) return this.settings?.allEpisodes ?? []

    const settings = await client.settings.update({
      where: {
        id: 0
      },
      data: {
        allEpisodes: {
          push: {
            channelId
          }
        }
      },
      select: {
        allEpisodes: true
      }
    })

    console.info(`Added ${channelId} to global destinations`)

    await this.refresh()
    return settings.allEpisodes
  }

  async removeGlobalDestination (channelId: string): Promise<Destination[]> {
    if (!await this.channelIsAlreadyGlobal(channelId)) return this.settings?.allEpisodes ?? []

    const settings = await client.settings.update({
      where: {
        id: 0
      },
      data: {
        allEpisodes: {
          deleteMany: {
            where: {
              channelId
            }
          }
        }
      }
    })

    console.info(`Removed ${channelId} from global destinations`)

    await this.refresh()
    return settings.allEpisodes
  }

  fetch = (): Settings | undefined => this.settings
}
