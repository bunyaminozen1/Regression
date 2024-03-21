package opc.junit.helpers.backoffice;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.models.backoffice.ImpersonateIdentityModel;
import opc.models.backoffice.SpendRulesModel;
import opc.services.backoffice.multi.BackofficeMultiService;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class BackofficeHelper {

    public static String impersonateIdentity(final String identityId, final IdentityType identityType, final String secretKey){
        return TestHelper.ensureAsExpected(20,
                () -> BackofficeMultiService.impersonateIdentity(new ImpersonateIdentityModel(identityId, identityType), secretKey),
                SC_OK)
                .jsonPath()
                .get("token.token");
    }

    public static String impersonateIdentityAccessToken(final String identityId, final IdentityType identityType, final String secretKey){
        return TestHelper.ensureAsExpected(20,
                        () -> BackofficeMultiService.impersonateIdentityAccessToken(new ImpersonateIdentityModel(identityId, identityType), secretKey),
                        SC_OK)
                .jsonPath()
                .get("token.token");
    }

    public static void postManagedCardsSpendRules(final SpendRulesModel spendRulesModel,
                                                  final String secretKey,
                                                  final String managedCardId,
                                                  final String impersonatedToken) {
        TestHelper.ensureAsExpected(15,
                () -> BackofficeMultiService.postManagedCardsSpendRules(spendRulesModel, secretKey, managedCardId, impersonatedToken),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(5,
                () -> BackofficeMultiService.getManagedCardsSpendRules(secretKey, managedCardId, impersonatedToken),
                x -> x.statusCode() == SC_OK && Boolean.valueOf(x.jsonPath().getString("cardLevelSpendRules.allowCreditAuthorisations")).equals(spendRulesModel.isAllowCreditAuthorisations()),
                Optional.of(String.format("Spend rules not added for card with id %s", managedCardId)));
    }
}
