package opc.junit.secure;

import com.google.common.collect.ImmutableMap;
import opc.enums.opc.SecurityModelConfiguration;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

public class CorporateDeTokenizeTests extends AbstractDeTokenizeTests {
    @Override
    protected String getAuthToken() {
        return corporateAuthenticationToken;
    }

    @Override
    protected String getPrepaidManagedCardsProfileId() {
        return corporatePrepaidManagedCardsProfileId;
    }

    @Override
    protected String getCurrency() {
        return corporateCurrency;
    }

    @Override
    protected String getAssociateRandom() {
        return corporateAssociateRandom;
    }
    @BeforeAll
    public static void Setup(){
        corporateSetup();
    }

    @AfterAll
    public static void Reset() {

        final Map<String, Boolean> securityModelConfig =
                ImmutableMap.of(SecurityModelConfiguration.PIN.name(), true,
                        SecurityModelConfiguration.PASSWORD.name(), false,
                        SecurityModelConfiguration.CVV.name(), true,
                        SecurityModelConfiguration.CARD_NUMBER.name(), true);

        updateSecurityModel(securityModelConfig);
    }

    private static void corporateSetup() {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();

        CorporatesHelper.verifyKyb(secretKey, corporateId);

        corporateAssociateRandom = associate(corporateAuthenticationToken);
    }
}
