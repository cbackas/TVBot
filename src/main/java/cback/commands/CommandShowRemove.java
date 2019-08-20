package cback.commands;

import cback.Channels;
import cback.TVBot;
import cback.TVRoles;
import cback.Util;
import cback.database.tv.Show;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;


public class CommandShowRemove extends Command {

    private TVBot bot;

    public CommandShowRemove() {
        this.bot = TVBot.getInstance();
        this.name = "removeshow";
        this.arguments = "removeshow [imdbID]";
        this.help = "Removes a show from the database and disassociates it from any channels";
        this.requiredRole = TVRoles.NETWORKMOD.name;
    }

    @Override
    protected void execute(CommandEvent commandEvent) {
        String[] args = commandEvent.getArgs().split("\\s+", 1);

        if(args.length >= 1) {
            String showID = args[0];
            Show show = bot.getDatabaseManager().getTV().getShow(showID);
            int entriesDeleted = bot.getDatabaseManager().getTV().deleteShow(showID);
            if(show != null && entriesDeleted > 0) {
                Util.sendMessage(commandEvent.getTextChannel(), "Removed show: " + show.getShowName() + ".");
                System.out.println("@" + commandEvent.getAuthor().getName() + " removed show " + show.getShowName());
                Util.simpleEmbed(commandEvent.getGuild().getTextChannelById(Channels.BOTLOG_CH_ID.getId()), show.getShowName() + " removed from the database.");
            } else {
                Util.simpleEmbed(commandEvent.getTextChannel(), "No saved show found by this IMDB ID.");
            }
        } else {
            Util.syntaxError(this, commandEvent.getMessage());
        }
        Util.deleteMessage(commandEvent.getMessage());
    }
}