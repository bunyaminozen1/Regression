package opc.models.admin;

public class GetAuthorisationsModel {
    private String cardId;

    public GetAuthorisationsModel(String cardId) {
        this.cardId = cardId;
    }

    public String getCardId() {
        return cardId;
    }

    public GetAuthorisationsModel setCardId(String cardId) {
        this.cardId = cardId;
        return this;
    }
}
