package cback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.dv8tion.jda.core.entities.TextChannel;

@RequiredArgsConstructor
public enum Channels {
    ANNOUNCEMENT_CH_ID("345774506373021716"),
    NEWEPISODE_CH_ID("263184398894104577"),
    GENERAL_CH_ID("192441520178200577"),
    SUGGEST_CH_ID("192444470942236672"),
    MESSAGELOG_CH_ID("305073652280590339"),
    SERVERLOG_CH_ID("217456105679224846"),
    DEV_CH_ID("269638376376893440"),

    //hub channels
    TEST_CH_ID("576886075994275841"),
    ERRORLOG_CH_ID("346104666796589056"),
    BOTLOG_CH_ID("346483682376286208"),
    BOTPM_CH_ID("346104720903110656");

    @Getter
    private final String id;

    public TextChannel getChannel() {
        return TVBot.getGuild().getTextChannelById(id);
    }
}
