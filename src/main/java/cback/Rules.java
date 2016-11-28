package cback;

public enum Rules {
    one("1", Util.getRule("251922065047683073")),
    two("2", Util.getRule("251922120446181387")),
    three("3", Util.getRule("251922158786314240")),
    four("4", Util.getRule("251922192147808256")),
    five("5", Util.getRule("251922232069193728")),
    six("6", Util.getRule("251922268408512515")),
    other("other", Util.getRule("251922307335979018"));

    public String number;
    public String fullRule;

    Rules(String number, String fullRule) {
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