package fpi.paymentrun.services.simulator;

import fpi.paymentrun.models.simulator.MockBankAccountAisModel;
import fpi.paymentrun.models.simulator.MockBankPaymentConsentPisModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class MockBankService extends BaseService {
    /**
     * API doc: https://previews.weavr.io/plugins/master/open-banking/#operation/postMockBankAccount
     * How it works: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2431418388/Payment+run+-+Mock+bank
     */

    final static private String MOCK_BANK_AUTHORISATION = "NDlkZTAxZTMtZTA3OC00YjNmLTgzNTctZGIzNTMwYzYxZmFm";

    public static Response createMockBankAccountAis(MockBankAccountAisModel mockBankAccountAisModel) {
        return getBodyAuthorisationKeyRequest(mockBankAccountAisModel, MOCK_BANK_AUTHORISATION)
                .when()
                .post(String.format("%s/1.0/mock-bank/create-account", getOpenBankingEnvironmentPrefix()));
    }

    public static Response createMockBankPaymentConsentPis(MockBankPaymentConsentPisModel mockBankPaymentConsentPisModel) {
        return getBodyAuthorisationKeyRequest(mockBankPaymentConsentPisModel, MOCK_BANK_AUTHORISATION)
                .when()
                .post(String.format("%s/1.0/mock-bank/create-payment", getOpenBankingEnvironmentPrefix()));
    }

    public static Response getAuthUrlCallback(final String path) {
        return restAssured()
                .header("Content-type", "application/json")
                .when()
                .get(path);
    }
}
