package fpi.paymentrun.services.SpiOpenBanking;

import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Optional;

public class SpiOpenBankingService extends BaseService {
    /**
     * API doc: https://previews.weavr.io/plugins/master/open-banking/
     */

    final static private String AUTHORISATION = getOpenBankingApiKey();

    public static Response getInstitutions(final Optional<Map<String, Object>> filters) {
        return assignQueryParams(getAuthorisationKeyRequest(AUTHORISATION), filters)
                .when()
                .get(String.format("%s/1.0/institutions", getOpenBankingEnvironmentPrefix()));
    }

    public static Response deleteAccountById(final String accountId) {
        return getAuthorisationKeyRequest(AUTHORISATION)
                .pathParam("accountId", accountId)
                .when()
                .delete(String.format("%s/1.0/accounts/{accountId}", getOpenBankingEnvironmentPrefix()));
    }

    public static Response getAccounts(final String consentid) {
        return getAuthorisationKeyRequest(AUTHORISATION)
                .header("consentid", consentid)
                .when()
                .get(String.format("%s/1.0/accounts", getOpenBankingEnvironmentPrefix()));
    }
}
