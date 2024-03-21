package fpi.paymentrun.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import fpi.paymentrun.models.CreatePaymentRunModel;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Optional;

public class PaymentRunsService extends BaseService {

    public static Response createPaymentRun(final CreatePaymentRunModel createPaymentRunModel,
                                            final String apiKey,
                                            final String authenticationToken) {
        return getBodyApiKeyAuthenticationRequest(createPaymentRunModel, apiKey, authenticationToken)
                .when()
                .post(String.format("%s/v1/payment_runs", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for NoApiKey cases
     */
    public static Response createPaymentRunNoApiKey(final CreatePaymentRunModel createPaymentRunModel,
                                                    final String authenticationToken) throws JsonProcessingException {
        return getIgnoreNullBodyAuthenticatedRequest(createPaymentRunModel, authenticationToken)
                .when()
                .post(String.format("%s/v1/payment_runs", getPaymentRunEnvironmentPrefix()));
    }

    public static Response cancelPaymentRun(final String paymentRunId,
                                            final String apiKey,
                                            final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .pathParam("id", paymentRunId)
                .when()
                .post(String.format("%s/v1/payment_runs/{id}/cancel", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for NoApiKey cases
     */
    public static Response cancelPaymentRunNoApiKey(final String paymentRunId,
                                                    final String authenticationToken) {
        return getAuthenticatedRequest(authenticationToken)
                .pathParam("id", paymentRunId)
                .when()
                .post(String.format("%s/v1/payment_runs/{id}/cancel", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getPaymentRuns(final String apiKey,
                                          final Optional<Map<String, Object>> filters,
                                          final String authenticationToken) {
        return assignQueryParams(getApiKeyAuthenticationRequest(apiKey, authenticationToken), filters)
                .when()
                .get(String.format("%s/v1/payment_runs", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getPaymentRun(final String paymentRunId,
                                         final String apiKey,
                                         final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .pathParam("id", paymentRunId)
                .when()
                .get(String.format("%s/v1/payment_runs/{id}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getPaymentRunFundingInstructions(final String paymentRunId,
                                                            final Optional<Map<String, Object>> filters,
                                                            final String apiKey,
                                                            final String authenticationToken) {
        return assignQueryParams(getApiKeyAuthenticationRequest(apiKey, authenticationToken), filters)
                .pathParam("id", paymentRunId)
                .when()
                .get(String.format("%s/v1/payment_runs/{id}/fund", getPaymentRunEnvironmentPrefix()));
    }

    public static Response confirmPaymentRun(final String paymentRunId,
                                            final String apiKey,
                                            final String authenticationToken) {
        return getApiKeyAuthenticationRequest(apiKey, authenticationToken)
                .pathParam("id", paymentRunId)
                .when()
                .post(String.format("%s/v1/payment_runs/{id}/confirm", getPaymentRunEnvironmentPrefix()));
    }
}
