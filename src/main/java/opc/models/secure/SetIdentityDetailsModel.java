package opc.models.secure;

public class SetIdentityDetailsModel {

    private String idDocumentHasAddress;

    public SetIdentityDetailsModel(boolean idDocumentHasAddress) {
        this.idDocumentHasAddress = Boolean.toString(idDocumentHasAddress).toUpperCase();
    }

    public String getIdDocumentHasAddress() {
        return idDocumentHasAddress;
    }

    public SetIdentityDetailsModel setIdDocumentHasAddress(String idDocumentHasAddress) {
        this.idDocumentHasAddress = idDocumentHasAddress;
        return this;
    }
}
