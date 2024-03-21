package opc.models.multi.managedcards;

import opc.enums.opc.CardMode;

public class CardDetailsModel {

    private CardMode cardMode;

    public CardDetailsModel(CardMode cardMode) {
        this.cardMode = cardMode;
    }

    public CardMode getCardMode() {
        return cardMode;
    }

    public CardDetailsModel setCardMode(CardMode cardMode) {
        this.cardMode = cardMode;
        return this;
    }
}
