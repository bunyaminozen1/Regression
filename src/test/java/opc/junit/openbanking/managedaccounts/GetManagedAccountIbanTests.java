package opc.junit.openbanking.managedaccounts;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.database.PayneticsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.junit.helpers.openbanking.OpenBankingAccountInformationHelper;
import opc.junit.helpers.openbanking.OpenBankingHelper;
import opc.junit.helpers.openbanking.OpenBankingSecureServiceHelper;
import opc.junit.openbanking.BaseSetup;
import opc.services.innovator.InnovatorService;
import opc.services.openbanking.AccountInformationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static opc.enums.openbanking.SignatureHeader.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class GetManagedAccountIbanTests extends BaseSetup {

    private static String corporateConsent;
    private static String consumerConsent;
    private static Map<String, String> corporateHeaders;
    private static Map<String, String> consumerHeaders;

    @BeforeAll
    public static void OneTimeSetup() throws Exception {

        corporateSetup();
        consumerSetup();

        corporateConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));
        consumerConsent = OpenBankingAccountInformationHelper.createConsent(sharedKey, OpenBankingHelper.generateHeaders(clientKeyId));

        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, corporateAuthenticationToken, tppId, corporateConsent);
        OpenBankingSecureServiceHelper.authoriseConsent(sharedKey, consumerAuthenticationToken, tppId, consumerConsent);
    }

    @BeforeEach
    public void Setup() throws Exception {
        corporateHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(corporateConsent)));
        consumerHeaders = OpenBankingHelper.generateHeaders(clientKeyId, ImmutableMap.of(DATE, Optional.empty(), DIGEST, Optional.empty(), TPP_CONSENT_ID, Optional.of(consumerConsent)));
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_Corporate_Success(final Currency currency) throws SQLException {

        final String managedAccountId =
                ManagedAccountsHelper.createUpgradedManagedAccount(corporateManagedAccountProfileId, currency.name(), secretKey, corporateAuthenticationToken);

        final ValidatableResponse response =
                AccountInformationService.getManagedAccountIban(sharedKey, corporateHeaders, managedAccountId)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccountId, createCorporateModel.getCompany().getName());
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_Consumer_Success(final Currency currency) throws SQLException {

        final String managedAccountId =
                ManagedAccountsHelper.createUpgradedManagedAccount(consumerManagedAccountProfileId, currency.name(), secretKey, consumerAuthenticationToken);

        final ValidatableResponse response =
                AccountInformationService.getManagedAccountIban(sharedKey, consumerHeaders, managedAccountId)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccountId,
                String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()));
    }

    @Test
    public void GetManagedAccountIban_AccountNotActive_SuccessUnallocated(){
        final String managedAccountId =
                ManagedAccountsHelper.createPendingApprovalManagedAccount(corporateManagedAccountProfileId, secretKey, corporateAuthenticationToken);

        AccountInformationService.getManagedAccountIban(sharedKey, corporateHeaders, managedAccountId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("UNALLOCATED"));
    }

    @Test
    public void GetManagedAccountIban_Unallocated_SuccessUnallocated(){
        final String managedAccountId = ManagedAccountsHelper
                .createManagedAccount(corporateManagedAccountProfileId, Currency.getRandomCurrency().name(), secretKey, corporateAuthenticationToken);

        AccountInformationService.getManagedAccountIban(sharedKey, corporateHeaders, managedAccountId)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("UNALLOCATED"));
    }

    @Test
    public void GetManagedAccountIban_UnknownManagedAccountId_NotFound(){

        AccountInformationService.getManagedAccountIban(sharedKey, consumerHeaders, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountIban_InvalidSharedKey_Unauthorised(){
        AccountInformationService.getManagedAccountIban("abc", consumerHeaders, RandomStringUtils.randomNumeric(18))
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountIban_NoSharedKey_BadRequest(){
        AccountInformationService.getManagedAccountIban("", consumerHeaders, RandomStringUtils.randomNumeric(18))
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountIban_DifferentInnovatorSharedKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String sharedKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("sharedKey");

        AccountInformationService.getManagedAccountIban(sharedKey, corporateHeaders, RandomStringUtils.randomNumeric(18))
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountIban_OtherProgrammeSharedKey_Forbidden(){

        AccountInformationService.getManagedAccountIban(applicationTwo.getSharedKey(), corporateHeaders, RandomStringUtils.randomNumeric(18))
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountIban_RootUserLoggedOut_Unauthorised() throws Exception {

        final Pair<String, Map<String, String>> newCorporate = createCorporateWithConsentHeaders();

        final String managedAccountId =
                ManagedAccountsHelper.createUpgradedManagedAccount(corporateManagedAccountProfileId, Currency.getRandomCurrency().name(), secretKey, newCorporate.getLeft());

        OpenBankingSecureServiceHelper.logout(sharedKey, newCorporate.getLeft(), tppId);

        AccountInformationService.getManagedAccountIban(sharedKey, newCorporate.getRight(), managedAccountId)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountsIban_CrossIdentityChecks_NotFound(){
        final String managedAccountId =
                ManagedAccountsHelper.createUpgradedManagedAccount(consumerManagedAccountProfileId, Currency.getRandomCurrency().name(), secretKey, consumerAuthenticationToken);

        AccountInformationService.getManagedAccountIban(sharedKey, corporateHeaders, managedAccountId)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void assertSuccessfulResponse(final ValidatableResponse response,
                                                 final Currency currency,
                                                 final String managedAccountId,
                                                 final String beneficiaryName) throws SQLException {

        response
                .body("state", equalTo("ALLOCATED"));

        switch (currency) {
            case EUR:
                final Map<String, String> eurBankDetails =
                        ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccountId).get(0);

                response.body("bankAccountDetails[0].beneficiaryNameAndSurname", equalTo(beneficiaryName))
                        .body("bankAccountDetails[0].beneficiaryBank", equalTo("SEPA - Saxo Payments A/S"))
                        .body("bankAccountDetails[0].beneficiaryBankAddress",
                                equalTo("SEPA - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                        .body("bankAccountDetails[0].details.iban", equalTo(eurBankDetails.get("iban")))
                        .body("bankAccountDetails[0].details.code", equalTo(eurBankDetails.get("bank_identifier_code")))
                        .body("bankAccountDetails[0].details.accountNumber", nullValue())
                        .body("bankAccountDetails[0].details.sortCode", nullValue())
                        .body("bankAccountDetails[0].details.bankIdentifierCode", nullValue())
                        .body("bankAccountDetails[0].paymentReference", nullValue())
                        .body("bankAccountDetails[1].beneficiaryNameAndSurname", equalTo(beneficiaryName))
                        .body("bankAccountDetails[1].beneficiaryBank", equalTo("SEPA - Saxo Payments A/S"))
                        .body("bankAccountDetails[1].beneficiaryBankAddress",
                                equalTo("SEPA - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                        .body("bankAccountDetails[1].details.iban", equalTo(eurBankDetails.get("iban")))
                        .body("bankAccountDetails[1].details.bankIdentifierCode", equalTo(eurBankDetails.get("bank_identifier_code")))
                        .body("bankAccountDetails[1].details.accountNumber", nullValue())
                        .body("bankAccountDetails[1].details.sortCode", nullValue())
                        .body("bankAccountDetails[1].details.code", nullValue())
                        .body("bankAccountDetails[1].paymentReference", nullValue());
                break;

            case GBP:
                final Map<String, String> gbpBankDetails =
                        ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccountId).get(0);

                    response.body("bankAccountDetails[0].beneficiaryNameAndSurname", equalTo(beneficiaryName))
                            .body("bankAccountDetails[0].beneficiaryBank", equalTo("FPS - Clearbank A/S"))
                            .body("bankAccountDetails[0].beneficiaryBankAddress",
                                    equalTo("FPS - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                            .body("bankAccountDetails[0].details.accountNumber", equalTo(gbpBankDetails.get("account_number")))
                            .body("bankAccountDetails[0].details.sortCode", equalTo(gbpBankDetails.get("sort_code")))
                            .body("bankAccountDetails[0].paymentReference", nullValue());
                break;

            case USD:
                final Map<String, String> usdBankDetails =
                        ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccountId).get(0);

                final String paymentReference =
                        PayneticsDatabaseHelper.getPayneticsAccount(managedAccountId).get(0).get("processor_external_reference");

                response.body("bankAccountDetails[0].beneficiaryNameAndSurname", equalTo("USD SAXO - Paynetics AD"))
                        .body("bankAccountDetails[0].beneficiaryBank", equalTo("USD SAXO - Saxo Payments A/S"))
                        .body("bankAccountDetails[0].beneficiaryBankAddress",
                                equalTo("USD SAXO - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                        .body("bankAccountDetails[0].details.iban", equalTo(usdBankDetails.get("iban")))
                        .body("bankAccountDetails[0].details.code", equalTo(usdBankDetails.get("bank_identifier_code")))
                        .body("bankAccountDetails[0].details.sortCode", nullValue())
                        .body("bankAccountDetails[0].details.accountNumber", nullValue())
                        .body("bankAccountDetails[0].paymentReference", equalTo(paymentReference));
                break;

            default: throw new IllegalArgumentException("Currency not supported.");
        }
    }
}
