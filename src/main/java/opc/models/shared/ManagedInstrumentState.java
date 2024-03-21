package opc.models.shared;

import java.util.Map;


public class ManagedInstrumentState {

    private String destroyType;

    private Map<String, BlockDetails> blockTypes;

    public ManagedInstrumentState() {
    }

    public String getDestroyType() {
        return destroyType;
    }

    public void setDestroyType(final String destroyType) {
        this.destroyType = destroyType;
    }

}
