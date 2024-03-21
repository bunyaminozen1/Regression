package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedCardsHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.testmodels.UnassignedManagedCardDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public class CorporateAssignManagedCardThreeDSecureTests extends AbstractAssignManagedCardThreeDSecureTests {
    private static String threeDSIdentityToken;
    private static List<UnassignedManagedCardDetails> threeDSecureIdentityUnassignedCards;
    private static String identityToken;
    private static List<UnassignedManagedCardDetails> identityUnassignedCards;

    @BeforeAll
    public static void IdentitySetup() {
//    setup 3ds identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(threeDSCorporateProfileId).build();

        final Pair<String, String> threeDSAuthenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                threeDSCorporateDetails, secretKey);
        threeDSIdentityToken = threeDSAuthenticatedCorporate.getRight();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();

//    setup identity not enabled for 3ds
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);
        identityToken = authenticatedCorporate.getRight();
        final String identityCurrency = corporateDetails.getBaseCurrency();

        final String identityManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, identityCurrency, identityToken);
        final String threeDSIdentityManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, threeDSIdentityCurrency, threeDSIdentityToken);

        identityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(identityManagedAccountId, corporatePrepaidManagedCardsProfileId,
                        corporateDebitManagedCardsProfileId, identityCurrency, CardLevelClassification.CORPORATE, innovatorToken);
        threeDSecureIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(threeDSIdentityManagedAccountId, corporatePrepaidManagedCardsProfileId,
                        corporateDebitManagedCardsProfileId, threeDSIdentityCurrency, CardLevelClassification.CORPORATE, innovatorToken);
    }

    @Override
    protected String getThreeDSIdentityToken() {
        return threeDSIdentityToken;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    public List<UnassignedManagedCardDetails> getThreeDSIdentityUnassignedCards() {
        return threeDSecureIdentityUnassignedCards;
    }

    @Override
    public List<UnassignedManagedCardDetails> getIdentityUnassignedCards() {
        return identityUnassignedCards;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }
}