package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateCreateManagedCardsAuthyThreeDSecureTests extends AbstractCreateManagedCardAuthyThreeDSecureTests {
    private static String biometricIdentityToken;
    private static String authyIdentityToken;
    private static String authyIdentityCurrency;
    private static String identityPrepaidManagedCardProfileId;
    private static String identityDebitManagedCardProfileId;
    private static String identityManagedAccountProfileId;

    @BeforeAll
    public static void IdentitySetup() {
//    setup identity enabled for Biometric
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(threeDSCorporateProfileId).build();

        final Pair<String, String> threeDSAuthenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                threeDSCorporateDetails, secretKey);
        biometricIdentityToken = threeDSAuthenticatedCorporate.getRight();

//    setup identity enabled for Authy
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);
        authyIdentityToken = authenticatedCorporate.getRight();
        authyIdentityCurrency = corporateDetails.getBaseCurrency();

        identityPrepaidManagedCardProfileId = corporatePrepaidManagedCardsProfileId;
        identityDebitManagedCardProfileId = corporateDebitManagedCardsProfileId;
        identityManagedAccountProfileId = corporateManagedAccountsProfileId;
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
    public String getAuthyIdentityCurrency() {
        return authyIdentityCurrency;
    }

    @Override
    public String getIdentityPrepaidManagedCardProfileId() {
        return identityPrepaidManagedCardProfileId;
    }

    @Override
    public String getIdentityDebitManagedCardProfileId() {
        return identityDebitManagedCardProfileId;
    }

    @Override
    public String getIdentityManagedAccountProfileId() {
        return identityManagedAccountProfileId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

}
