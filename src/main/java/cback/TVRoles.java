package cback;

public enum TVRoles {
    STAFF("STAFF", 227213155917496330L),
    ADMIN("ADMIN", 192441946210435072L),
    HEADMOD("HEADMOD", 263126026261889024L),
    NETWORKMOD("NETWORKMOD", 264130466062401536L),
    MOD("MOD", 192442068981776384L),
    MOVIENIGHT("MOVIENIGHT", 226443478664609792L),
    TRUSTED("TRUSTED", 318910861425246209L),
    REDDITMOD("REDDITMOD", 221973215948308480L);

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