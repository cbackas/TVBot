import client from "./prisma"
import { Prisma, Settings } from "@prisma/client"

type SettingsNoId = Omit<Settings, 'id'>

/**
 * Manager to handle fetching and saving settings in the DB
 */
export class SettingsManager {
  // set the defaults for the settings
  private settings?: SettingsNoId

  /**
   * Save initial settings data to the DB
   */
  private initData = async () => {
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
  refresh = async () => {
    try {
      // fetch the settings from the DB
      this.settings = await client.settings.findUniqueOrThrow({
        where: {
          id: 0
        }
      })
    } catch (error) {
      if (!(error instanceof Prisma.PrismaClientKnownRequestError)) throw error
      if (error.code === 'P2025') await this.initData()
    }
  }

  /**
   * Update settings in the DB
   * @param inputData settings data to update
   */
  update = async (inputData: Partial<SettingsNoId>) => {
    const data = Prisma.validator<Prisma.SettingsUpdateInput>()(inputData)

    await client.settings.update({
      where: {
        id: 0,
      },
      data
    })

    await this.refresh()
  }

  fetch = (): SettingsNoId | undefined => this.settings
}
