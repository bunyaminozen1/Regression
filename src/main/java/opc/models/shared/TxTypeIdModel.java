package opc.models.shared;

public class TxTypeIdModel {

    private final String type;
    private final String id;

    public TxTypeIdModel(final String type, final String id){
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
