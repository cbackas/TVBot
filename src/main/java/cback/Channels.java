package cback;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import net.dv8tion.jda.core.entities.TextChannel;

@RequiredArgsConstructor
public enum Channels {
    ANNOUNCEMENT_CH_ID(345774506373021716l),
    NEWEPISODE_CH_ID(263184398894104577l),
    GENERAL_CH_ID(192441520178200577l),
    SUGGEST_CH_ID(192444470942236672l),
    MESSAGELOG_CH_ID(305073652280590339l),
    SERVERLOG_CH_ID(217456105679224846l),
    DEV_CH_ID(269638376376893440l),

    //hub channels
    TEST_CH_ID(576886075994275841l),
    ERRORLOG_CH_ID(346104666796589056l),
    BOTLOG_CH_ID(346483682376286208l),
    BOTPM_CH_ID(346104720903110656l);
    @Getter
    private final long id;

    public TextChannel getChannel() {
        return TVBot.getClient().getTextChannelById(id);
    }
}
