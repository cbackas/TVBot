package cback;

public enum Rules {
    one("1", "Rule One | Civil Discussion", Util.getRule("345792703046615043")),
    two("2", "Rule Two | Spam**", Util.getRule("345792898371026945")),
    three("3", "Rule Three | Self-Promotion", Util.getRule("345792952255250442")),
    four("4", "Rule Four | Spoilers", Util.getRule("345793033171894272")),
    five("5", "Rule Five | NSFW Content", Util.getRule("345793123261349889")),
    six("6", "Rule Six | Bots and Exploits", Util.getRule("345793187861757953")),
    other("other", "Other", Util.getRule("345793249933525022"));

    public String number;
    public String title;
    public String fullRule;

    Rules(String number, String title, String fullRule) {
        this.title = title;
        this.number = number;
        this.fullRule = fullRule;
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