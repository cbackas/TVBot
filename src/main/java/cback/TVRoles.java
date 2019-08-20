package cback;

public enum TVRoles {
    STAFF("Staff", 227213155917496330L),
    ADMIN("Admins", 192441946210435072L),
    NETWORKMOD("Network Mods", 264130466062401536L),
    MOD("Mods", 192442068981776384L),
    MOVIENIGHT("MovieNight", 226443478664609792L),
    TRUSTED("Trusted", 318910861425246209L),
    REDDITMOD("Reddit Mods", 221973215948308480L);

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