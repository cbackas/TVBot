import { Collection } from "discord.js"

type Step = {
  status: typeof StepStatus[keyof typeof StepStatus]
  message: string
}

export const StepStatus = {
  PENDING: '🔴' as const,
  IN_PROGRESS: '🟡' as const,
  COMPLETE: '🟢' as const
} as const

export class ProgressMessageBuilder {
  private steps: Collection<number, Step>
  private currentStep = 0
  private totalSteps = 0

  constructor() {
    this.steps = new Collection<number, Step>()
  }

  public addStep = (message: string): ProgressMessageBuilder => {
    this.totalSteps += 1

    this.steps.set(this.totalSteps, {
      status: StepStatus.PENDING,
      message
    })

    return this
  }

  setStatus = (stepNumber: number, status: Step['status']): ProgressMessageBuilder => {
    const step = this.steps.get(stepNumber)

    if (step === undefined) throw new Error(`Step ${stepNumber} does not exist`)

    step.status = status

    return this
  }

  nextStep = (): string => {
    const isFirstStep = this.currentStep === 0
    if (!isFirstStep) {
      this.setStatus(this.currentStep, StepStatus.COMPLETE)
    }

    if (this.currentStep != this.totalSteps) {
      this.setStatus(this.currentStep + 1, StepStatus.IN_PROGRESS)
    }

    this.currentStep += 1

    return this.toString()
  }

  public toString = (): string => {
    const messages = this.steps.map((step) => `${step.status} ${step.message}`)
    return messages.join('\n')
  }
}
