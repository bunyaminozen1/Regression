package opc.models.shared;

public class FeeSourceModel {

    private final String type;
    private final String id;

    public FeeSourceModel(final String type, final String id){
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
