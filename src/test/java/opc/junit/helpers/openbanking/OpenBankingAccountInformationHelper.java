package opc.junit.helpers.openbanking;

import opc.junit.helpers.TestHelper;
import opc.services.openbanking.AccountInformationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;

public class OpenBankingAccountInformationHelper {

    public static String createConsent(final String sharedKey,
                                       final Map<String, String> headers) {
        return TestHelper.ensureAsExpected(10,
                () -> AccountInformationService.createConsent(sharedKey, headers),
                SC_OK).jsonPath().getString("id");
    }

    public static List<String> createConsents(final String sharedKey,
                                              final String clientKeyId,
                                              final Map<String, String> headers,
                                              final Integer numberOfConsents) {
        List<String> consents = new ArrayList<>();

        for (int i = 0; i < numberOfConsents; i++) {
            final String consent = createConsent(sharedKey, headers);
            consents.add(consent);
        }

        TestHelper.ensureAsExpected(30,
                () -> AccountInformationService.getConsents(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId),
                        Optional.empty()),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("count").equals(numberOfConsents),
                Optional.of(String.format("Expecting 200 with a consent count of %s, check logged payload", numberOfConsents)));

        return consents;
    }
}
