import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { editInteractionResponse } from "../lib/discord.js";
import { getEnv } from "../lib/env.js";
import { getStringOption } from "../lib/interactionOptions.js";
import { StepStatus } from "../lib/progressMessages.js";
import {
  checkForAiringEpisodes,
  pruneUnsubscribedShows,
  sweepDeadChannels,
} from "../lib/shows.js";
import type { Command } from "./index.js";

interface Task {
  /** Present-tense line shown while the task is running (🟦). */
  running: string;
  /** Past-tense line shown once the task finishes (🟩). */
  done: string;
  /** Line shown if the task throws (🟥). */
  error: string;
  run(): Promise<void>;
}

const TASKS: Record<string, Task> = {
  check_episodes: {
    running: "Refreshing episode data for tracked shows",
    done: "Refreshed episode data for tracked shows",
    error: "Failed to refresh episode data",
    run: () => checkForAiringEpisodes(),
  },
  prune_shows: {
    running: "Pruning shows with no subscriptions",
    done: "Pruned shows with no subscriptions",
    error: "Failed to prune shows",
    run: () => pruneUnsubscribedShows(),
  },
  sweep_channels: {
    running: "Sweeping destinations for deleted channels",
    done: "Swept destinations for deleted channels",
    error: "Failed to sweep dead channels",
    run: () => sweepDeadChannels(getEnv("DISCORD_TOKEN")),
  },
};

function formatDuration(ms: number): string {
  const totalSeconds = ms / 1000;
  if (totalSeconds < 60) {
    return `${totalSeconds.toFixed(1)}s`;
  }
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = Math.round(totalSeconds % 60);
  return `${minutes}m ${seconds}s`;
}

export default class TriggerCommand implements Command {
  public readonly name = "trigger";

  public readonly definition: RESTPostAPIChatInputApplicationCommandsJSONBody =
    {
      name: "trigger" as const,
      description:
        'Manually run a scheduled maintenance task. Requires "Administrator" permission.',
      default_member_permissions: "8",
      contexts: [InteractionContextType.Guild],
      options: [
        {
          type: ApplicationCommandOptionType.String,
          name: "task",
          description: "Which scheduled task to run now",
          required: true,
          choices: [
            { name: "Refresh episode data", value: "check_episodes" },
            { name: "Prune unsubscribed shows", value: "prune_shows" },
            { name: "Sweep dead channels", value: "sweep_channels" },
          ],
        },
      ],
    };

  async handler(interaction: APIApplicationCommandInteraction): Promise<void> {
    const token = interaction.token;
    const taskName = getStringOption(interaction, "task") ?? "";
    const task = TASKS[taskName];

    if (task == null) {
      await editInteractionResponse(token, {
        content: `Unknown task: \`${taskName}\``,
      });
      return;
    }

    // 🟦 while the task runs
    await editInteractionResponse(token, {
      content: `${StepStatus.PENDING} ${task.running}`,
    });

    const start = Date.now();
    try {
      await task.run();
      const elapsed = formatDuration(Date.now() - start);
      // 🟩 once complete, swapping in the done message + elapsed time
      await editInteractionResponse(token, {
        content: `${StepStatus.COMPLETE} ${task.done} (${elapsed})`,
      });
    } catch (error) {
      const elapsed = formatDuration(Date.now() - start);
      console.error(`Error running triggered task ${taskName}:`, error);
      // 🟥 on failure
      await editInteractionResponse(token, {
        content: `${StepStatus.ERROR} ${task.error} (${elapsed})`,
      });
    }
  }
}
