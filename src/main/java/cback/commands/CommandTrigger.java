package cback.commands;

import cback.TVBot;
import cback.Util;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class CommandTrigger extends Command {

    private TVBot bot;

    public CommandTrigger() {
        this.bot = TVBot.getInstance();
        this.name = "trigger";
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        Util.getLogger().info("trigger");
        if (commandEvent.getAuthor().getId().equals("73416411443113984") || commandEvent.getAuthor().getId().equalsIgnoreCase("109109946565537792")) {

            List<Long> permCategories = Arrays.asList(TVBot.STAFF_CAT_ID, TVBot.INFO_CAT_ID, TVBot.DISCUSSION_CAT_ID, TVBot.FUN_CAT_ID, TVBot.CARDS_CAT_ID, TVBot.NEW_CAT_ID);
            List<GuildChannel> permChannels = permCategories.stream()
                    .map(catID -> commandEvent.getGuild().getCategoryById(catID))
                    .map(cat -> cat.getChannels())
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            System.out.println(permChannels);
        }
    }
}