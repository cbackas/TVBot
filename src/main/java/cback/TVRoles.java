package cback;

public enum TVRoles {
    STAFF("STAFF", 227213155917496330l),
    ADMIN("ADMIN", 192441946210435072l),
    HEADMOD("HEADMOD", 263126026261889024l),
    NETWORKMOD("NETWORKMOD", 264130466062401536l),
    MOD("MOD", 192442068981776384l),
    MOVIENIGHT("MOVIENIGHT", 226443478664609792l),
    HELPER("HELPER", 228231762113855489l),
    REDDITMOD("REDDITMOD", 221973215948308480l);

    public String name;
    public Long id;

    TVRoles(String name, Long id) {
        this.name = name;
        this.id = id;
    }

    public static TVRoles getRole(String name) {
        for (TVRoles role : values()) {
            if (role.name.equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}