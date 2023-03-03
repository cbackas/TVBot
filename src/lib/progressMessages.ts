import { type ChatInputCommandInteraction, Collection, type Message } from 'discord.js'

interface Step {
  status: typeof StepStatus[keyof typeof StepStatus]
  message: string
}

export const StepStatus = {
  PENDING: '🔴' as const,
  IN_PROGRESS: '🟡' as const,
  COMPLETE: '🟢' as const
} as const

export class ProgressMessageBuilder {
  // a discord interaction can optionally be passed in, this is useful for updating the message in place
  private readonly interaction?: ChatInputCommandInteraction

  private readonly steps: Collection<number, Step>
  private currentStep = 0
  private totalSteps = 0

  constructor (interaction?: ChatInputCommandInteraction) {
    this.steps = new Collection<number, Step>()
    this.interaction = interaction
  }

  /**
   * add a new step to the progress message
   * @param message the message to display for the step
   * @returns ProgressMessageBuilder object
   */
  public addStep (message: string): ProgressMessageBuilder {
    this.totalSteps += 1

    this.steps.set(this.totalSteps, {
      status: StepStatus.PENDING,
      message
    })

    return this
  }

  /**
   * set the status of a step
   * @param stepNumber index of step to manipulate the status of
   * @param status desired status
   * @returns ProgressMessageBuilder object
   */
  setStatus (stepNumber: number, status: Step['status']): ProgressMessageBuilder {
    const step = this.steps.get(stepNumber)

    if (step === undefined) throw new Error(`Step ${stepNumber} does not exist`)

    step.status = status

    return this
  }

  /**
   * updates the current step and returns the updated progress message
   * @returns the updated progress message
   */
  nextStep (): string {
    const isFirstStep = this.currentStep === 0
    if (!isFirstStep) {
      this.setStatus(this.currentStep, StepStatus.COMPLETE)
    }

    if (this.currentStep !== this.totalSteps) {
      this.setStatus(this.currentStep + 1, StepStatus.IN_PROGRESS)
    }

    this.currentStep += 1

    return this.toString()
  }

  /**
   * wrapper function that updates the ProgressMessage object and sends it to the user
   * only works if the ProgressMessageBuilder was initialized with a chat interaction
   * @returns the sent discord message
   */
  async sendNextStep (additionalMessage?: string): Promise<Message<boolean>> {
    if (this.interaction == null) throw new Error('ProgressMessageBuilder was not initialized with an interaction')

    // send the message to the user
    return await this.interaction.editReply(this.nextStep() + (additionalMessage !== undefined ? `\n\n${additionalMessage}` : ''))
  }

  public toString = (): string => {
    const messages = this.steps.map((step) => `${step.status} ${step.message}`)
    return messages.join('\n')
  }
}
