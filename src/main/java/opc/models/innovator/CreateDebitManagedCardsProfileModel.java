package opc.models.innovator;

import commons.enums.Jurisdiction;
import lombok.Builder;
import lombok.Getter;
import opc.enums.opc.CardBureau;
import opc.enums.opc.IdentityType;

@Getter
@Builder
public class CreateDebitManagedCardsProfileModel {

    private final AbstractCreateManagedCardsProfileModel createManagedCardsProfileRequest;

    public static CreateDebitManagedCardsProfileModelBuilder DefaultCorporateCreateDebitManagedCardsProfileModel() {
        return DefaultCreateDebitManagedCardsProfileModel(IdentityType.CORPORATE, CardBureau.NITECREST);
    }

    public static CreateDebitManagedCardsProfileModelBuilder DefaultConsumerCreateDebitManagedCardsProfileModel() {
        return DefaultCreateDebitManagedCardsProfileModel(IdentityType.CONSUMER, CardBureau.NITECREST);
    }

    public static CreateDebitManagedCardsProfileModelBuilder DefaultCreateDebitManagedCardsProfileModel(final IdentityType identityType,
                                                                                                        final CardBureau cardBureau) {
        return new CreateDebitManagedCardsProfileModelBuilder()
                .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                        .DefaultAbstractCreateManagedCardsProfileModel(identityType, "DEBIT", cardBureau).build());
    }

    public static CreateDebitManagedCardsProfileModelBuilder DefaultCreateDebitManagedCardsProfileModel(final IdentityType identityType,
                                                                                                        final CardBureau cardBureau,
                                                                                                        final Jurisdiction jurisdiction) {
        return new CreateDebitManagedCardsProfileModelBuilder()
                .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                        .DefaultAbstractCreateManagedCardsProfileModel(identityType, "DEBIT", cardBureau, jurisdiction).build());
    }
}