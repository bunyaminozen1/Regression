package opc.models.simulator;

public class DetokenizeModel {
    private String token;
    private String field;

    public DetokenizeModel(final String token, final String field) {
        this.token = token;
        this.field = field;
    }

    public String getToken() {
        return token;
    }

    public DetokenizeModel setToken(String token) {
        this.token = token;
        return this;
    }

    public String getField() {
        return field;
    }

    public DetokenizeModel setField(String field) {
        this.field = field;
        return this;
    }
}
