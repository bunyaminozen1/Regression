package opc.junit.admin.authsessions;

import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateGetChallengesTests extends AbstractGetChallengesTests {
    private static String identityToken;
    private static String identityId;
    private static String identityManagedAccountId;
    private static String identityManagedCardId;
    private static String identityCurrency;

    @BeforeAll
    public static void IdentitySetup(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(
                createCorporateModel, secretKey);
        identityId = authenticatedCorporate.getLeft();
        identityToken = authenticatedCorporate.getRight();
        identityCurrency = createCorporateModel.getBaseCurrency();
        identityManagedAccountId = createManagedAccount(corporateManagedAccountProfileId,
                identityCurrency, identityToken).getLeft();

        final CreateCorporateModel destinationCorporateModel =
                CreateCorporateModel.EurCurrencyCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> destinationCorporate = CorporatesHelper.createEnrolledVerifiedCorporate(destinationCorporateModel, secretKey);
        identityManagedCardId = createPrepaidManagedCard(corporatePrepaidManagedCardsProfileId,
                destinationCorporateModel.getBaseCurrency(), destinationCorporate.getRight()).getLeft();

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(identityId, secretKey, identityToken);
        fundManagedAccount(identityManagedAccountId, identityCurrency, 100000L);
    }

    @Override
    public String getIdentityToken() {
        return identityToken;
    }

    @Override
    public String getIdentityId() {
        return identityId;
    }

    @Override
    public String getIdentityManagedAccountId() {
        return identityManagedAccountId;
    }

    @Override
    public String getIdentityManagedCardId() {
        return identityManagedCardId;
    }

    @Override
    public String getIdentityCurrency() {
        return identityCurrency;
    }
}
