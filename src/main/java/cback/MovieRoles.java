package cback;

public enum MovieRoles {
    STAFF("STAFF", "227213155917496330"),
    ADMIN("ADMIN", "192441946210435072"),
    MOD("MOD", "192442068981776384"),
    MOVIENIGHT("MOVIENIGHT", "226443478664609792"),
    TRIALMOD("TRIALMOD", "228231762113855489"),
    REDDITMOD("REDDITMOD", "221973215948308480");

    public String name;
    public String id;

    MovieRoles(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public static MovieRoles getRole(String name) {
        for (MovieRoles role : values()) {
            if (role.name.equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }
}