package spi.openbanking.openbanking;

import org.junit.jupiter.api.Test;
import spi.openbanking.helpers.OpenBankingHelper;
import spi.openbanking.models.OpenBankingAccountResponseModel;
import spi.openbanking.services.OpenBankingService;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetAccountTests {

    private static final String CONSENT_ID = OpenBankingHelper.CONSENT_ID;

    @Test
    public void GetAccount_Success() {

        final OpenBankingAccountResponseModel account =
                OpenBankingHelper.getAccountsByConsentId(CONSENT_ID).stream().findAny().orElseThrow();

        OpenBankingService.getAccount(account.getId(), OpenBankingHelper.API_KEY)
                .then()
                .statusCode(SC_OK)
                .body("meta.tracingId", notNullValue())
                .body("data.id", equalTo(account.getId()))
                .body("data.consentId", equalTo(account.getConsentId()))
                .body("data.institutionId", equalTo(account.getInstitutionId()))
                .body("data.displayName", equalTo(account.getDisplayName()))
                .body("data.currency", equalTo(account.getCurrency()))
                .body("data.accountIdentification.accountNumber", equalTo(account.getAccountIdentification().getAccountNumber()))
                .body("data.accountIdentification.sortCode", equalTo(account.getAccountIdentification().getSortCode()))
                .body("data.accountNames[0]", equalTo(account.getAccountNames().get(0)))
                .body("data.type", equalTo(account.getType()))
                .body("data.usageType", equalTo(account.getUsageType()))
                .body("data.consent.id", equalTo(CONSENT_ID))
                .body("data.consent.status", equalTo("AUTHORIZED"))
                .body("data.consent.expiresAt", notNullValue());
    }
}
