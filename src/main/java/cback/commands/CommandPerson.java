package cback.commands;

import cback.TVBot;
import cback.Util;
import com.uwetrottmann.trakt5.entities.Person;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandPerson implements Command {
    @Override
    public String getName() {
        return "person";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("actor", "actress", "people");
    }

    @Override
    public void execute(TVBot bot, IDiscordClient client, String[] args, IGuild guild, IMessage message, boolean isPrivate) {
        String personName = Arrays.stream(args).collect(Collectors.joining(" "));
        Person personData = bot.getTraktManager().personSummaryFromName(personName);
        if (personData != null) {
            String name = "**" + personData.name + "**";
            String homepage = "<http://www.imdb.com/name/" + personData.ids.imdb + ">";
            String birthPlace = personData.birthplace;

            String dates = new SimpleDateFormat("MMM dd, yyyy").format(personData.birthday.toDate());
            if (personData.death != null) {
                String deathDate = new SimpleDateFormat("MMM dd, yyyy").format(personData.death.toDate());
                dates += "\nDIED: " + deathDate;
            }

            String overview = personData.biography;
            String [] arr = overview.split("\\s+");
            String nWords = "";

            for(int i = 0; i < 100 ; i++){
                nWords = nWords + " " + arr[i];
            }


            Util.sendMessage(message.getChannel(),
                    name + "\n" +
                            nWords + "...\n" +
                            homepage + "\n" +
                            "```\n" +
                            "BORN: " + birthPlace + "\n" +
                            "BIRTHPLACE: " + dates + "\n" +
                            "```\n");
        } else {
            Util.sendMessage(message.getChannel(), "Error: Person not found.");
        }
    }

    @Override
    public boolean isLogged() {
        return false;
    }
}
