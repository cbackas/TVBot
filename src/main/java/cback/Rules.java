package cback;

public enum Rules {
    one("1", Util.getRule("263187086595719171")),
    two("2", Util.getRule("263187123769704458")),
    three("3", Util.getRule("263187157361885194")),
    four("4", Util.getRule("263187200550764545")),
    five("5", Util.getRule("263187237397594113")),
    six("6", Util.getRule("263187288152866817")),
    other("other", Util.getRule("263187326061117442"));

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