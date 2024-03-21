package opc.junit.multi.security.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;

public class CorporatePasswordSecurityEnabledTests extends AbstractPasswordSecurityEnabledTests {

    @BeforeEach
    public void IndividualSetup(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate =
                CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey, tokenizeAnon(TestHelper.getDefaultPassword(secretKey)));
        identityId = authenticatedCorporate.getLeft();
        authenticationToken = authenticatedCorporate.getRight();
        identityEmail = createCorporateModel.getRootUser().getEmail();
        identityType = IdentityType.CORPORATE;
        identityProfileId = corporateProfileId;

        CorporatesHelper.verifyKyb(secretKey, identityId);

        associateRandom = associate(authenticationToken);
    }

    @Override
    protected String createIdentity() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(identityProfileId).build();

        return CorporatesHelper.createCorporate(createCorporateModel, secretKey);
    }
}
