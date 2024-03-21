package opc.junit.secure;

import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;

public class CorporateAnonTokenizeTests extends AbstractAnonTokenizeTests {

    private static String corporateAuthenticationToken;

    @BeforeAll
    public static void Setup() {
        corporateSetup();
    }

    @Override
    protected String getIdentityToken() {
        return corporateAuthenticationToken;
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        final String corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }
}
