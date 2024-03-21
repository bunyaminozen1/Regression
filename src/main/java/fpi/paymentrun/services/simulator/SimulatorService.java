package fpi.paymentrun.services.simulator;

import fpi.paymentrun.models.simulator.SimulateLinkedAccountModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class SimulatorService extends BaseService {
    public static Response simulateLinkedAccount(final String apiKey,
                                                 final SimulateLinkedAccountModel simulateLinkedAccount) {
        return getBodyApiKeyRequest(simulateLinkedAccount, apiKey)
                .when()
                .post(String.format("%s/simulate/v1/linked_accounts", getPaymentRunEnvironmentPrefix()));
    }

    public static Response simulateFunding(final String paymentRunId,
                                           final String groupReference,
                                           final String apiKey) {
        return getApiKeyRequest(apiKey)
                .pathParam("payment_run_id", paymentRunId)
                .pathParam("group_reference", groupReference)
                .when()
                .post(String.format("%s/simulate/v1/payment_runs/{payment_run_id}/fund_group/{group_reference}", getPaymentRunEnvironmentPrefix()));
    }
}
