package cback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.TextChannel;

@RequiredArgsConstructor
public enum Channels {
    ANNOUNCEMENT_CH_ID(345774506373021716L),
    NEWEPISODE_CH_ID(263184398894104577L),
    GENERAL_CH_ID(192441520178200577L),
    SUGGEST_CH_ID(192444470942236672L),
    MESSAGELOG_CH_ID(305073652280590339L),
    SERVERLOG_CH_ID(217456105679224846L),
    DEV_CH_ID(269638376376893440L),

    //hub channels
    TEST_CH_ID(576886075994275841L),
    ERRORLOG_CH_ID(346104666796589056L),
    BOTLOG_CH_ID(346483682376286208L),
    BOTPM_CH_ID(346104720903110656L);
    @Getter
    private final long id;

    public TextChannel getChannel() {
        return TVBot.getInstance().getClient().getTextChannelById(id);
    }
}
