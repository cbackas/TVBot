package cback;

public enum Rules {
    one("1", "Rule 1: Stay Civil", "Respect all members of the community. No racial, sexual, homophobic or cultural slurs. This also includes being civil in all general chat done within the Discord server. No incivility, excessive vulgarity and non-offensive avatars."),
    two("2", "Rule 2: No Spam", "Spam will not be tolerated at all. Excessive letters, emojis, and pictures especially. Use of zalgo text is not allowed in any way. Names with special text that is not available on a traditional keyboard can be changed. Shitposting will be decided by staff on a case by case basis."),
    three("3", "Rule 3: No Self-Promotion", "No linking to personal discords or blatant self promotion. If the only reason you came here is to advertise then please refrain from doing it. No misleading or clickbait links. If you came here to talk about a partnership contact either Zock, Barky, or any other admin and we will reply as soon as possible."),
    four("4", "Rule 4: Keep spoilers in their respective channels", "If you accidentally post a spoiler please try to delete it as fast as possible. Most shows will have live discussions so if you are trying to avoid spoilers don't join the chat while the show is airing. Keep discussions in their respective channels as best you can."),
    five("5", "Rule 5: No NSFW content of any kind", "Here’s a direct quote from Discord’s terms of service - “you agree to not post, upload, transmit or otherwise disseminate information that is obscene, indecent, vulgar, pornographic, sexual or otherwise objectionable” Cursing is allowed but use your best judgement whenever typing anything that could be misconstrued."),
    six("6", "Rule 6: Do not abuse or add bots", "Do not spam bots in any place besides #commands. If the bots have a glitch you are expected to alert staff imediately. Do not add your own bots to the server."),
    other("other", "Other", "Punishment is decided by staff, they have the final say in ALL matters. If you decide to leave instantly after being warned you will be banned without any chance of it being revoked. These rules are not final and are subject to change at any moment.");

    public String number;
    public String title;
    public String specifics;

    Rules(String number, String title, String specifics) {
        this.number = number;
        this.title = title;
        this.specifics = specifics;
    }

    public static Rules getRule(String number) {
        for (Rules rule : values()) {
            if (rule.number.equalsIgnoreCase(number)) {
                return rule;
            }
        }
        return null;
    }
}