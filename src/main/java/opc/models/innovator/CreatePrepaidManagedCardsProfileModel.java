package opc.models.innovator;

import commons.enums.Jurisdiction;
import lombok.Builder;
import lombok.Getter;
import opc.enums.opc.CardBureau;
import opc.enums.opc.IdentityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class CreatePrepaidManagedCardsProfileModel {
    private final AbstractCreateManagedCardsProfileModel createManagedCardsProfileRequest;
    private final List<String> currency;
    private final List<String> fiProvider;
    private final List<String> channelProvider;

    public static CreatePrepaidManagedCardsProfileModelBuilder DefaultCorporateCreatePrepaidManagedCardsProfileModel() {
        return DefaultCreatePrepaidManagedCardsProfileModel(IdentityType.CORPORATE, CardBureau.NITECREST);
    }

    public static CreatePrepaidManagedCardsProfileModelBuilder DefaultConsumerCreatePrepaidManagedCardsProfileModel() {
        return DefaultCreatePrepaidManagedCardsProfileModel(IdentityType.CONSUMER, CardBureau.NITECREST);
    }

    public static CreatePrepaidManagedCardsProfileModelBuilder DefaultCreatePrepaidManagedCardsProfileModel(final IdentityType identityType,
                                                                                                            final CardBureau cardBureau) {
        return new CreatePrepaidManagedCardsProfileModelBuilder()
                .currency(Arrays.asList("EUR", "USD", "GBP"))
                .fiProvider(Collections.singletonList("paynetics_eea"))
                .channelProvider(Collections.singletonList("gps"))
                .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                        .DefaultAbstractCreateManagedCardsProfileModel(identityType, "PREPAID", cardBureau).build());
    }

    public static CreatePrepaidManagedCardsProfileModelBuilder DefaultCreatePrepaidManagedCardsProfileModel(final IdentityType identityType,
                                                                                                            final CardBureau cardBureau,
                                                                                                            final Jurisdiction jurisdiction) {
        return new CreatePrepaidManagedCardsProfileModelBuilder()
                .currency(Arrays.asList("EUR", "USD", "GBP"))
                .fiProvider(Collections.singletonList(String.format("paynetics_%s", jurisdiction.name().toLowerCase())))
                .channelProvider(Collections.singletonList("gps"))
                .createManagedCardsProfileRequest(AbstractCreateManagedCardsProfileModel
                        .DefaultAbstractCreateManagedCardsProfileModel(identityType, "PREPAID", cardBureau, jurisdiction).build());
    }
}
