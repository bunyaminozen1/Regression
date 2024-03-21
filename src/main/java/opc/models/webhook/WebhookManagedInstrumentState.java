package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookManagedInstrumentState {

    private LinkedHashMap<String, String> blockTypes;
    private String destroyType;

    public LinkedHashMap<String, String> getBlockTypes() {
        return blockTypes;
    }

    public WebhookManagedInstrumentState setBlockTypes(LinkedHashMap<String, String> blockTypes) {
        this.blockTypes = blockTypes;
        return this;
    }

    public String getDestroyType() {
        return destroyType;
    }

    public WebhookManagedInstrumentState setDestroyType(String destroyType) {
        this.destroyType = destroyType;
        return this;
    }
}
