package opc.junit.multi.gpsthreedsecure;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateCreateManagedCardsThreeDSecureTests extends AbstractCreateManagedCardThreeDSecureTests {
    private static String threeDSIdentityToken;
    private static String threeDSIdentityCurrency;
    private static String identityToken;
    private static String identityCurrency;
    private static String identityPrepaidManagedCardProfileId;
    private static String identityDebitManagedCardProfileId;
    private static String identityManagedAccountProfileId;

    @BeforeAll
    public static void IdentitySetup() {
//    setup 3ds identity
        final CreateCorporateModel threeDSCorporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(threeDSCorporateProfileId).build();

        final Pair<String, String> threeDSAuthenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                threeDSCorporateDetails, secretKey);
        threeDSIdentityToken = threeDSAuthenticatedCorporate.getRight();
        threeDSIdentityCurrency = threeDSCorporateDetails.getBaseCurrency();

//    setup identity not enabled for 3ds
        final CreateCorporateModel corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                corporateDetails, secretKey);
        identityToken = authenticatedCorporate.getRight();
        identityCurrency = corporateDetails.getBaseCurrency();
        identityPrepaidManagedCardProfileId = corporatePrepaidManagedCardsProfileId;
        identityDebitManagedCardProfileId = corporateDebitManagedCardsProfileId;
        identityManagedAccountProfileId = corporateManagedAccountsProfileId;
    }

    @Override
    protected String getThreeDSIdentityToken() {
        return threeDSIdentityToken;
    }

    @Override
    public String getThreeDSIdentityCurrency() {
        return threeDSIdentityCurrency;
    }

    @Override
    protected String getIdentityToken() {
        return identityToken;
    }

    @Override
    public String getIdentityCurrency() {
        return identityCurrency;
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
