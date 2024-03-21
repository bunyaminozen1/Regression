package opc.models.multi.passwords;

public class LostPasswordStartModel {
    private final String email;

    public LostPasswordStartModel(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
