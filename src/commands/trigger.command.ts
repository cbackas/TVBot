import {
  type APIApplicationCommandInteraction,
  ApplicationCommandOptionType,
  InteractionContextType,
  type RESTPostAPIChatInputApplicationCommandsJSONBody,
} from "discord-api-types/v10";
import { editInteractionResponse } from "../lib/discord.js";
import { getEnv } from "../lib/env.js";
import { getStringOption } from "../lib/interactionOptions.js";
import { ProgressMessageBuilder, StepStatus } from "../lib/progressMessages.js";
import {
  checkForAiringEpisodes,
  pruneUnsubscribedShows,
  sweepDeadChannels,
} from "../lib/shows.js";
import type { Command } from "./index.js";

/** Push a live progress line under the current step (best-effort). */
type ProgressReporter = (detail: string) => Promise<void>;

interface Task {
  label: string;
  /** Runs the task and returns a one-line summary of what it did. */
  run(report: ProgressReporter): Promise<string>;
}

function plural(n: number, word: string): string {
  return `${n} ${word}${n === 1 ? "" : "s"}`;
}

const TASKS: Record<string, Task> = {
  check_episodes: {
    label: "Refreshing episode data for tracked shows",
    run: async (report) => {
      const s = await checkForAiringEpisodes(async (processed, total) => {
        await report(`Refreshing… ${processed}/${total} shows`);
      });
      const parts = [
        `${plural(s.showsRefreshed, "show")} refreshed`,
        `${plural(s.episodesFound, "upcoming episode")} (${s.newEpisodes} new)`,
      ];
      if (s.showsFailed > 0)
        parts.push(`${plural(s.showsFailed, "show")} failed`);
      return parts.join(" · ");
    },
  },
  prune_shows: {
    label: "Pruning shows with no subscriptions",
    run: async () => {
      const count = await pruneUnsubscribedShows();
      return `${plural(count, "show")} pruned`;
    },
  },
  sweep_channels: {
    label: "Sweeping destinations for deleted channels",
    run: async () => {
      const { probed, pruned } = await sweepDeadChannels(
        getEnv("DISCORD_TOKEN"),
      );
      return `${plural(probed, "channel")} probed · ${plural(pruned, "dead channel")} pruned`;
    },
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

    const progress = new ProgressMessageBuilder(token).addStep(task.label);
    const start = Date.now();

    // Best-effort live progress line under the in-progress step. A failed edit
    // must never abort the task, so swallow errors here.
    const report: ProgressReporter = async (detail) => {
      try {
        await editInteractionResponse(token, {
          content: `${progress.toString()}\n\n${detail}`,
        });
      } catch (error) {
        console.error("Error updating trigger progress:", error);
      }
    };

    try {
      await progress.sendNextStep();
      const summary = await task.run(report);

      const elapsed = formatDuration(Date.now() - start);
      progress.setCurrentStatus(StepStatus.COMPLETE);
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\n${summary}\nTook ${elapsed}`,
      });
    } catch (error) {
      const elapsed = formatDuration(Date.now() - start);
      console.error(`Error running triggered task ${taskName}:`, error);
      progress.setCurrentStatus(StepStatus.ERROR);
      await editInteractionResponse(token, {
        content: `${progress.toString()}\n\nFailed after ${elapsed}`,
      });
    }
  }
}
