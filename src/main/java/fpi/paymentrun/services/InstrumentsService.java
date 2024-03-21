package fpi.paymentrun.services;

import io.restassured.response.Response;

public class InstrumentsService extends BaseService {

    public static Response getLinkedAccount(final String linkedAccountId,
                                            final String apiKey,
                                            final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .pathParam("id", linkedAccountId)
                .when()
                .get(String.format("%s/v1/linked_accounts/{id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getLinkedAccounts(final String apiKey,
                                             final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .when()
                .get(String.format("%s/v1/linked_accounts", getPaymentRunEnvironmentPrefix()));
    }

    public static Response deleteLinkedAccounts(final String linkedAccountId,
                                                final String apiKey,
                                                final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .pathParam("id", linkedAccountId)
                .when()
                .delete(String.format("%s/v1/linked_accounts/{id}", getPaymentRunEnvironmentPrefix()));
    }
}
