package opc.models.innovator;

import commons.enums.Jurisdiction;
import lombok.Builder;
import lombok.Getter;
import opc.enums.opc.CardBureau;
import opc.enums.opc.IdentityType;

@Getter
@Builder
public class CreateManagedCardsProfileV2Model {

    private final CreatePrepaidManagedCardsProfileModel createPrepaidProfileRequest;
    private final CreateDebitManagedCardsProfileModel createDebitProfileRequest;
    private final String cardFundingType;

    public static CreateManagedCardsProfileV2ModelBuilder DefaultCreatePrepaidManagedCardsProfileV2Model(final IdentityType identityType,
                                                                         final CardBureau cardBureau,
                                                                         final Jurisdiction jurisdiction) {
        return new CreateManagedCardsProfileV2ModelBuilder()
                .createPrepaidProfileRequest(CreatePrepaidManagedCardsProfileModel.DefaultCreatePrepaidManagedCardsProfileModel(identityType, cardBureau, jurisdiction).build())
                .cardFundingType("PREPAID");
    }

    public static CreateManagedCardsProfileV2ModelBuilder DefaultCreateDebitManagedCardsProfileV2Model(final IdentityType identityType,
                                                                       final CardBureau cardBureau,
                                                                       final Jurisdiction jurisdiction) {
        return new CreateManagedCardsProfileV2ModelBuilder()
                .createDebitProfileRequest(CreateDebitManagedCardsProfileModel.DefaultCreateDebitManagedCardsProfileModel(identityType, cardBureau, jurisdiction).build())
                .cardFundingType("DEBIT");
    }
}
