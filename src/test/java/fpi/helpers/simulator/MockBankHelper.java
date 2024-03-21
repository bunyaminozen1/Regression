package fpi.helpers.simulator;

import fpi.paymentrun.models.simulator.MockBankAccountAisModel;
import fpi.paymentrun.models.simulator.MockBankPaymentConsentPisModel;
import fpi.paymentrun.services.simulator.MockBankService;
import opc.junit.helpers.TestHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

public class MockBankHelper {
    /**
     * API doc: https://qa.weavr.io/open-banking/docs#operation/postMockBankAccount
     * How it works: https://weavr-payments.atlassian.net/wiki/spaces/PM/pages/2431418388/Payment+run+-+Mock+bank
     */

    public static String getAuthUrlRequestId(final String authorisationUrl) throws URISyntaxException, MalformedURLException {
        final URL url = new URI(authorisationUrl).toURL();
        Map<String, String> map = getMapQueryParams(url);
        return map.get("authUrlRequestId");
    }

    public static String getTransferId(final String authorisationUrl) throws URISyntaxException, MalformedURLException {
        final URL url = new URI(authorisationUrl).toURL();
        Map<String, String> map = getMapQueryParams(url);
        return map.get("transferId");
    }

    public static String getAuthUrlCallbackPath(final String redirectUrl) throws URISyntaxException, MalformedURLException {
        final URL url = new URI(redirectUrl).toURL();
        Map<String, String> paramMap = getMapQueryParams(url);
        URIBuilder uriBuilder = new URIBuilder(url.getPath());
        uriBuilder.addParameters(paramMap.entrySet()
                .stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
                .collect(toList()));

        return uriBuilder.toString();
    }

    public static Map<String, String> getMapQueryParams(final URL url) {
        String[] params = url.getQuery().split("&");
        Map<String, String> paramMap = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            paramMap.put(name, value);
        }
        return paramMap;
    }

    public static String createMockBankAccountAis(final String authUrlRequestId) {
        MockBankAccountAisModel mockBankAccountAisModel = MockBankAccountAisModel.defaultMockBankAccountAisModel(authUrlRequestId).build();

        return TestHelper.ensureAsExpected(15,
                        () -> MockBankService.createMockBankAccountAis(mockBankAccountAisModel),
                        SC_CREATED).then()
                .extract()
                .jsonPath()
                .get("redirectUrl");
    }

    public static String createMockBankAccountAis(final String authUrlRequestId,
                                                  final Pair<String, String> bankDetails) {
        MockBankAccountAisModel mockBankAccountAisModel =
                MockBankAccountAisModel
                        .defaultMockBankAccountAisModel(authUrlRequestId)
                        .sortCode(bankDetails.getRight())
                        .accountNumber(bankDetails.getLeft())
                        .build();

        return TestHelper.ensureAsExpected(15,
                        () -> MockBankService.createMockBankAccountAis(mockBankAccountAisModel),
                        SC_CREATED).then()
                .extract()
                .jsonPath()
                .get("redirectUrl");
    }

    public static void getAuthUrlCallback(final String authUrlCallbackPath) {
        TestHelper.ensureAsExpected(15,
                () -> MockBankService.getAuthUrlCallback(authUrlCallbackPath),
                SC_OK);
    }

    public static void mockBankAis(final String authorisationUrl) throws MalformedURLException, URISyntaxException {
        final String authUrlRequestId = MockBankHelper.getAuthUrlRequestId(authorisationUrl);
        final String redirectUrl = MockBankHelper.createMockBankAccountAis(authUrlRequestId);
        final String authUrlCallbackPath = MockBankHelper.getAuthUrlCallbackPath(redirectUrl);
        MockBankHelper.getAuthUrlCallback(authUrlCallbackPath);
    }

    public static void mockBankAis(final String authorisationUrl,
                                   final Pair<String, String> bankDetails) throws MalformedURLException, URISyntaxException {
        final String authUrlRequestId = MockBankHelper.getAuthUrlRequestId(authorisationUrl);
        final String redirectUrl = MockBankHelper.createMockBankAccountAis(authUrlRequestId, bankDetails);
        final String authUrlCallbackPath = MockBankHelper.getAuthUrlCallbackPath(redirectUrl);
        MockBankHelper.getAuthUrlCallback(authUrlCallbackPath);
    }

    public static String createMockBankPaymentConsentPis(final String transferId) {
        MockBankPaymentConsentPisModel mockBankPaymentConsentPisModel = MockBankPaymentConsentPisModel.builder()
                .transferId(transferId)
                .isoStatusCode("ACSC")
                .build();

        return TestHelper.ensureAsExpected(15,
                        () -> MockBankService.createMockBankPaymentConsentPis(mockBankPaymentConsentPisModel),
                        SC_CREATED).then()
                .extract()
                .jsonPath()
                .get("redirectUrl");
    }

    public static void mockBankPis(final String authorisationUrl) throws MalformedURLException, URISyntaxException {
        final String transferId = MockBankHelper.getTransferId(authorisationUrl);
        final String redirectUrl = MockBankHelper.createMockBankPaymentConsentPis(transferId);
        final String authUrlCallbackPath = MockBankHelper.getAuthUrlCallbackPath(redirectUrl);
        MockBankHelper.getAuthUrlCallback(authUrlCallbackPath);
    }
}
