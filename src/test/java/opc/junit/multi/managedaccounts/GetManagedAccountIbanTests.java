package opc.junit.multi.managedaccounts;

import io.restassured.response.ValidatableResponse;
import commons.enums.Currency;
import opc.enums.opc.IdentityType;
import opc.junit.database.ManagedAccountsDatabaseHelper;
import opc.junit.database.PayneticsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.ManagedAccountsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.managedaccounts.CreateManagedAccountModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ManagedAccountsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;

public class GetManagedAccountIbanTests extends BaseManagedAccountsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static CreateCorporateModel corporateDetails;
    private static CreateConsumerModel consumerDetails;
    private static String corporateId;
    private static String consumerId;

    @BeforeAll
    public static void Setup(){
        corporateSetup();
        consumerSetup();
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_Corporate_Success(final Currency currency) throws SQLException {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, currency.name(), corporateAuthenticationToken);

        ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        String paymentReference = null;
        if (currency.equals(Currency.USD)){
            paymentReference =
                    PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
        }

        final ValidatableResponse response =
                ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccount.getLeft(), corporateDetails.getCompany().getName(), paymentReference, true);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_Consumer_Success(final Currency currency) throws SQLException {
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, currency.name(), consumerAuthenticationToken);

        ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        String paymentReference = null;
        if (currency.equals(Currency.USD)){
            paymentReference =
                    PayneticsDatabaseHelper.getPayneticsAccount(managedAccount.getLeft()).get(0).get("processor_external_reference");
        }


        final ValidatableResponse response =
                ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccount.getLeft(),
                String.format("%s %s", consumerDetails.getRootUser().getName(), consumerDetails.getRootUser().getSurname()),
                paymentReference, true);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_CorporateUnderNonFpsTenant_Success(final Currency currency) throws SQLException {

        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(nonFpsEnabledTenantDetails.getCorporatesProfileId()).build();

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getCorporatePayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), corporate.getRight());

        ManagedAccountsService.assignManagedAccountIban(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId, corporate.getRight())
                .then()
                .statusCode(SC_OK);

        final String paymentReference =
                PayneticsDatabaseHelper.getPayneticsAccount(managedAccountId).get(0).get("processor_external_reference");

        final ValidatableResponse response =
                ManagedAccountsService.getManagedAccountIban(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId,
                        corporate.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccountId, createCorporateModel.getCompany().getName(), currency.equals(Currency.EUR) ? null : paymentReference, false);
    }

    @ParameterizedTest
    @EnumSource(value = Currency.class)
    public void GetManagedAccountIban_ConsumerUnderNonFpsTenant_Success(final Currency currency) throws SQLException {

        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(nonFpsEnabledTenantDetails.getConsumersProfileId()).build();

        final Pair<String, String> consumer =
                ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel,
                        nonFpsEnabledTenantDetails.getSecretKey());

        final String managedAccountId =
                ManagedAccountsHelper.createManagedAccount(nonFpsEnabledTenantDetails.getConsumerPayneticsEeaManagedAccountsProfileId(),
                        currency.name(), nonFpsEnabledTenantDetails.getSecretKey(), consumer.getRight());

        ManagedAccountsService.assignManagedAccountIban(nonFpsEnabledTenantDetails.getSecretKey(),
                managedAccountId, consumer.getRight())
                .then()
                .statusCode(SC_OK);

        final String paymentReference =
                PayneticsDatabaseHelper.getPayneticsAccount(managedAccountId).get(0).get("processor_external_reference");

        final ValidatableResponse response =
                ManagedAccountsService.getManagedAccountIban(nonFpsEnabledTenantDetails.getSecretKey(), managedAccountId,
                        consumer.getRight())
                        .then()
                        .statusCode(SC_OK);

        assertSuccessfulResponse(response, currency, managedAccountId,
                String.format("%s %s", createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname()),
                currency.equals(Currency.EUR) ? null : paymentReference, false);
    }

    @Test
    public void GetManagedAccountIban_AccountNotActive_SuccessUnallocated(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createPendingApprovalManagedAccount(corporateManagedAccountProfileId, corporateAuthenticationToken);

        ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("UNALLOCATED"));
    }

    @Test
    public void GetManagedAccountIban_Unallocated_SuccessUnallocated(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateDetails.getBaseCurrency(), corporateAuthenticationToken);

        ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("UNALLOCATED"));
    }

    @Test
    public void GetManagedAccountIban_UnknownManagedAccountId_NotFound(){

        ManagedAccountsService.getManagedAccountIban(secretKey, RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountIban_InvalidApiKey_Unauthorised(){
        ManagedAccountsService.getManagedAccountIban("abc", RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountIban_NoApiKey_BadRequest(){
        ManagedAccountsService.getManagedAccountIban("", RandomStringUtils.randomNumeric(18), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetManagedAccountIban_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ManagedAccountsService.getManagedAccountIban(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountIban_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ManagedAccountsService.getManagedAccountIban(secretKey, RandomStringUtils.randomNumeric(18), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetManagedAccountsIban_CrossIdentityChecks_NotFound(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerDetails.getBaseCurrency(), consumerAuthenticationToken);

        ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("state", equalTo("ALLOCATED"));

        ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetManagedAccountIban_BackofficeCorporateImpersonator_Forbidden(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(corporateManagedAccountProfileId, corporateDetails.getBaseCurrency(), corporateAuthenticationToken);

        ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccount.getLeft(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetManagedAccountIban_BackofficeConsumerImpersonator_Forbidden(){
        final Pair<String, CreateManagedAccountModel> managedAccount =
                createManagedAccount(consumerManagedAccountProfileId, consumerDetails.getBaseCurrency(), consumerAuthenticationToken);

        ManagedAccountsService.assignManagedAccountIban(secretKey, managedAccount.getLeft(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK);

        ManagedAccountsService.getManagedAccountIban(secretKey, managedAccount.getLeft(), getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerDetails = createConsumerModel;

        ConsumersHelper.verifyKyc(secretKey, consumerId);
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateDetails = createCorporateModel;

        CorporatesHelper.verifyKyb(secretKey, corporateId);
    }

    private static void assertSuccessfulResponse(final ValidatableResponse response,
                                                 final Currency currency,
                                                 final String managedAccountId,
                                                 final String beneficiaryName,
                                                 final String paymentReference,
                                                 final boolean isFpsEnabled) throws SQLException {

        response
                .body("state", equalTo("ALLOCATED"));

        switch (currency) {
            case EUR:
                final Map<String, String> eurBankDetails = ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccountId)
                        .get(0);

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

                if (isFpsEnabled) {
                    response.body("bankAccountDetails[0].beneficiaryNameAndSurname", equalTo(beneficiaryName))
                            .body("bankAccountDetails[0].beneficiaryBank", equalTo("FPS - Clearbank A/S"))
                            .body("bankAccountDetails[0].beneficiaryBankAddress",
                                    equalTo("FPS - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                            .body("bankAccountDetails[0].details.accountNumber", equalTo(gbpBankDetails.get("account_number")))
                            .body("bankAccountDetails[0].details.sortCode", equalTo(gbpBankDetails.get("sort_code")))
                            .body("bankAccountDetails[0].paymentReference", nullValue());
                } else {
                    response.body("bankAccountDetails[0].beneficiaryNameAndSurname", equalTo("GBP SAXO - Paynetics AD"))
                            .body("bankAccountDetails[0].beneficiaryBank", equalTo("GBP SAXO - Saxo Payments A/S"))
                            .body("bankAccountDetails[0].beneficiaryBankAddress",
                                    equalTo("GBP SAXO - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                            .body("bankAccountDetails[0].details.code", equalTo(gbpBankDetails.get("bank_identifier_code")))
                            .body("bankAccountDetails[0].details.iban", equalTo(gbpBankDetails.get("iban")))
                            .body("bankAccountDetails[0].details.sortCode", nullValue())
                            .body("bankAccountDetails[0].details.bankIdentifierCode", nullValue())
                            .body("bankAccountDetails[0].details.accountNumber", nullValue())
                            .body("bankAccountDetails[0].paymentReference", equalTo(paymentReference))
                            .body("bankAccountDetails[1].beneficiaryNameAndSurname", equalTo("GBP SAXO - Paynetics AD"))
                            .body("bankAccountDetails[1].beneficiaryBank", equalTo("GBP SAXO - Saxo Payments A/S"))
                            .body("bankAccountDetails[1].beneficiaryBankAddress",
                                    equalTo("GBP SAXO - Hill Tower, 76A, James Bourchier Blvd., 1407 Sofia, Bulgaria"))
                            .body("bankAccountDetails[1].details.accountNumber", equalTo(gbpBankDetails.get("account_number")))
                            .body("bankAccountDetails[1].details.sortCode", equalTo(gbpBankDetails.get("sort_code")))
                            .body("bankAccountDetails[1].details.code", nullValue())
                            .body("bankAccountDetails[1].details.bankIdentifierCode", nullValue())
                            .body("bankAccountDetails[1].details.iban", nullValue())
                            .body("bankAccountDetails[1].paymentReference", equalTo(paymentReference));
                }
                break;

            case USD:

                final Map<String, String> usdBankDetails =
                        ManagedAccountsDatabaseHelper.getManagedAccountBankDetails(managedAccountId).get(0);

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
