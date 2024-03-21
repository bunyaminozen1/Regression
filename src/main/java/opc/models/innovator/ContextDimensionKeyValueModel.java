package opc.models.innovator;

public class ContextDimensionKeyValueModel {
    private String key;
    private String value;
    private boolean matchAny;

    public ContextDimensionKeyValueModel(String key, String value, boolean matchAny) {
        this.key = key;
        this.value = value;
        this.matchAny = matchAny;
    }

    public String getKey() {
        return key;
    }

    public ContextDimensionKeyValueModel setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public ContextDimensionKeyValueModel setValue(String value) {
        this.value = value;
        return this;
    }

    public boolean isMatchAny() {
        return matchAny;
    }

    public ContextDimensionKeyValueModel setMatchAny(boolean matchAny) {
        this.matchAny = matchAny;
        return this;
    }
}
