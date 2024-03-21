package spi.openbanking.openbanking;

import org.junit.jupiter.api.Test;
import spi.openbanking.helpers.OpenBankingHelper;
import spi.openbanking.models.OpenBankingAccountResponseModel;
import spi.openbanking.models.OpenBankingAccountsResponseModel;
import spi.openbanking.services.OpenBankingService;

import java.util.List;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GetAccountsTests {

    private static final String CONSENT_ID = OpenBankingHelper.CONSENT_ID;

    @Test
    public void GetAccounts_Success() {

        final List<OpenBankingAccountResponseModel> accounts =
                OpenBankingHelper.getAccountsByConsentId(CONSENT_ID);

        final OpenBankingAccountsResponseModel retrievedAccounts =
                OpenBankingService.getAccounts(CONSENT_ID, OpenBankingHelper.API_KEY)
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .as(OpenBankingAccountsResponseModel.class);

        assertEquals(accounts.size(), retrievedAccounts.getMeta().getCount());
        assertNotNull(retrievedAccounts.getMeta().getTracingId());

        accounts.forEach(account -> {
            final OpenBankingAccountResponseModel retrievedAccount =
                    retrievedAccounts.getData()
                            .stream()
                            .filter(x -> x.getId().equals(account.getId()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(account.getId(), retrievedAccount.getId());
            assertEquals(account.getConsentId(), retrievedAccount.getConsentId());
            assertEquals(account.getInstitutionId(), retrievedAccount.getInstitutionId());
            assertEquals(account.getDisplayName(), retrievedAccount.getDisplayName());
            assertEquals(account.getCurrency(), retrievedAccount.getCurrency());
            assertEquals(account.getAccountIdentification().getAccountNumber(), retrievedAccount.getAccountIdentification().getAccountNumber());
            assertEquals(account.getAccountIdentification().getSortCode(), retrievedAccount.getAccountIdentification().getSortCode());
            assertEquals(account.getAccountNames(), retrievedAccount.getAccountNames());
            assertEquals(account.getType(), retrievedAccount.getType());
            assertEquals(account.getUsageType(), retrievedAccount.getUsageType());
        });
    }
}
