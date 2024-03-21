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

public class CorporateAssignManagedCardAuthyThreeDSecureTests extends AbstractAssignManagedCardAuthyThreeDSecureTests {
    private static String biometricIdentityToken;
    private static List<UnassignedManagedCardDetails> biometricIdentityUnassignedCards;
    private static String authyIdentityToken;
    private static List<UnassignedManagedCardDetails> authyIdentityUnassignedCards;

    @BeforeAll
    public static void IdentitySetup() {
//    setup 3ds identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(threeDSCorporateProfileId).build();

        final Pair<String, String> threeDSAuthenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                threeDSCorporateDetails, secretKey);
        biometricIdentityToken = threeDSAuthenticatedCorporate.getRight();
        final String threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();

//    setup identity not enabled for 3ds
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);
        authyIdentityToken = authenticatedCorporate.getRight();
        final String identityCurrency = corporateDetails.getBaseCurrency();

        final String identityManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, identityCurrency, authyIdentityToken);
        final String threeDSIdentityManagedAccountId =
                createManagedAccount(corporateManagedAccountsProfileId, threeDSIdentityCurrency, biometricIdentityToken);

        authyIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(identityManagedAccountId, corporatePrepaidManagedCardsProfileId,
                        corporateDebitManagedCardsProfileId, identityCurrency, CardLevelClassification.CORPORATE, innovatorToken);
        biometricIdentityUnassignedCards =
                ManagedCardsHelper.replenishCardPool(threeDSIdentityManagedAccountId, corporatePrepaidManagedCardsProfileId,
                        corporateDebitManagedCardsProfileId, threeDSIdentityCurrency, CardLevelClassification.CORPORATE, innovatorToken);
    }

    @Override
    protected String getBiometricIdentityToken() {
        return biometricIdentityToken;
    }

    @Override
    protected String getAuthyIdentityToken() {
        return authyIdentityToken;
    }

    @Override
    public List<UnassignedManagedCardDetails> getBiometricIdentityUnassignedCards() {
        return biometricIdentityUnassignedCards;
    }

    @Override
    public List<UnassignedManagedCardDetails> getAuthyIdentityUnassignedCards() {
        return authyIdentityUnassignedCards;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }
}