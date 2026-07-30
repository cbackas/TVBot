import { editInteractionResponse } from "./discord.js";

interface Step {
  status: (typeof StepStatus)[keyof typeof StepStatus];
  message: string;
}

export const StepStatus = {
  PENDING: "🟦" as const,
  IN_PROGRESS: "🟨" as const,
  COMPLETE: "🟩" as const,
  ERROR: "🟥" as const,
  SKIPPED: "🟧" as const,
} as const;

export class ProgressMessageBuilder {
  private readonly interactionToken: string;

  private readonly steps: Map<number, Step>;
  private currentStep = 0;
  private totalSteps = 0;

  constructor(interactionToken: string) {
    this.steps = new Map<number, Step>();
    this.interactionToken = interactionToken;
  }

  /**
   * add a new step to the progress message
   * @param message the message to display for the step
   * @returns ProgressMessageBuilder object
   */
  public addStep = (message: string): ProgressMessageBuilder => {
    this.totalSteps += 1;

    this.steps.set(this.totalSteps, {
      status: StepStatus.PENDING,
      message,
    });

    return this;
  };

  /**
   * set the status of a step
   * @param stepNumber index of step to manipulate the status of
   * @param status desired status
   * @returns ProgressMessageBuilder object
   */
  setStatus = (
    stepNumber: number,
    status: Step["status"],
  ): ProgressMessageBuilder => {
    const step = this.steps.get(stepNumber);

    if (step === undefined)
      throw new Error(`Step ${stepNumber} does not exist`);

    step.status = status;

    return this;
  };

  setCurrentStatus = (status: Step["status"]): ProgressMessageBuilder => {
    if (this.currentStep === 0) return this;
    this.setStatus(this.currentStep, status);
    if (status === StepStatus.ERROR) {
      this.skipRemaining();
    }
    return this;
  };

  /**
   * Mark every step after the current one as SKIPPED.
   * Useful when an early step short-circuits the rest of the flow.
   */
  skipRemaining = (): ProgressMessageBuilder => {
    for (let i = this.currentStep + 1; i <= this.totalSteps; i++) {
      this.setStatus(i, StepStatus.SKIPPED);
    }
    return this;
  };

  /**
   * updates the current step and returns the updated progress message
   * @returns the updated progress message
   */
  nextStep = (): string => {
    const isFirstStep = this.currentStep === 0;
    if (!isFirstStep) {
      this.setStatus(this.currentStep, StepStatus.COMPLETE);
    }

    if (this.currentStep !== this.totalSteps) {
      this.setStatus(this.currentStep + 1, StepStatus.IN_PROGRESS);
    }

    this.currentStep += 1;

    return this.toString();
  };

  /**
   * wrapper function that updates the ProgressMessage object and sends it to the user
   * @returns void
   */
  sendNextStep = async (additionalMessage?: string): Promise<void> => {
    const content =
      this.nextStep() +
      (additionalMessage !== undefined ? `\n\n${additionalMessage}` : "");

    await editInteractionResponse(this.interactionToken, { content });
  };

  public toString = (): string => {
    const messages: string[] = [];
    for (const [, step] of this.steps) {
      messages.push(`${step.status} ${step.message}`);
    }
    return messages.join("\n");
  };
}
