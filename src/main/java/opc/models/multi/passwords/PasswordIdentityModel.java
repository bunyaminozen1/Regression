package opc.models.multi.passwords;

public class PasswordIdentityModel {
    private final String type;
    private final String id;

    public PasswordIdentityModel(String type, String id) {
        this.type = type;
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }
}
