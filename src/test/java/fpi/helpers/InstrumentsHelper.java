package fpi.helpers;

import fpi.helpers.uicomponents.AisUxComponentHelper;
import fpi.helpers.simulator.MockBankHelper;
import fpi.paymentrun.services.InstrumentsService;
import io.restassured.response.ValidatableResponse;
import opc.junit.helpers.TestHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.apache.http.HttpStatus.SC_OK;

public class InstrumentsHelper {
    public static void verifyLinkedAccountsCount(final String secretKey,
                                                 final String buyerToken,
                                                 final int count) {
        TestHelper.ensureAsExpected(15,
                () -> InstrumentsService.getLinkedAccounts(secretKey, buyerToken),
                x -> x.statusCode() == SC_OK
                        && x.jsonPath().getString("count").equals(count),
                Optional.of(String.format("Expecting 200 with to get Linked Accounts with count equals %s, check logged payload", count)));
    }

    public static String getLinkedAccountId(final String secretKey,
                                            final String buyerToken) {
        return TestHelper.ensureAsExpected(15,
                        () -> InstrumentsService.getLinkedAccounts(secretKey, buyerToken),
                        SC_OK)
                .then()
                .extract()
                .jsonPath()
                .get("linkedAccounts[0].id");
    }

    public static ValidatableResponse getLinkedAccountById(final String accountId,
                                                           final String secretKey,
                                                           final String buyerToken) {
        return TestHelper.ensureAsExpected(15,
                        () -> InstrumentsService.getLinkedAccount(accountId, secretKey, buyerToken),
                        SC_OK)
                .then();
    }

    public static ValidatableResponse getLinkedAccounts(final String secretKey,
                                                        final String buyerToken) {
        return TestHelper.ensureAsExpected(15,
                () -> InstrumentsService.getLinkedAccounts(secretKey, buyerToken),
                SC_OK).then();
    }

    public static String createLinkedAccountGetId(final String buyerToken,
                                                  final String secretKey,
                                                  final String sharedKey) throws MalformedURLException, URISyntaxException {
        final String authorisationUrl = AisUxComponentHelper.createAuthorisationUrlMockBank(buyerToken, sharedKey);
        MockBankHelper.mockBankAis(authorisationUrl);
        return getLinkedAccountId(secretKey, buyerToken);
    }

    public static String createLinkedAccountGetId(final String buyerToken,
                                                  final Pair<String, String> bankDetails,
                                                  final String secretKey,
                                                  final String sharedKey) throws MalformedURLException, URISyntaxException {
        final String authorisationUrl = AisUxComponentHelper.createAuthorisationUrlMockBank(buyerToken, sharedKey);
        MockBankHelper.mockBankAis(authorisationUrl, bankDetails);
        return getLinkedAccountId(secretKey, buyerToken);
    }

    public static ValidatableResponse createLinkedAccount(final String buyerToken,
                                                          final String secretKey,
                                                          final String sharedKey) throws MalformedURLException, URISyntaxException {
        final String authorisationUrl = AisUxComponentHelper.createAuthorisationUrlMockBank(buyerToken, sharedKey);
        MockBankHelper.mockBankAis(authorisationUrl);
        return getLinkedAccounts(secretKey, buyerToken);
    }

    public static ValidatableResponse createLinkedAccounts(final String buyerToken,
                                                           final String secretKey,
                                                           final String sharedKey,
                                                           final int count) {
        IntStream.range(0, count).forEach(i -> {
            final String authorisationUrl = AisUxComponentHelper.createAuthorisationUrlMockBank(buyerToken, sharedKey);
            try {
                MockBankHelper.mockBankAis(authorisationUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

        });
        return getLinkedAccounts(secretKey, buyerToken);
    }
}
