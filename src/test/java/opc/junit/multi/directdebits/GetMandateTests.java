package opc.junit.multi.directdebits;

import commons.enums.Currency;
import opc.enums.opc.*;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.DirectDebitsHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import commons.models.CompanyModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.simulator.SimulateCreateMandateModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.DirectDebitsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class GetMandateTests extends BaseDirectDebitsSetup {

    private static String corporateAuthenticationToken;
    private static String consumerAuthenticationToken;
    private static String corporateId;
    private static String consumerId;
    private static String corporateCurrency;
    private static String consumerCurrency;
    private static Pair<String, FasterPaymentsBankDetailsModel> corporateManagedAccount;
    private static Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount;
    private static Map.Entry<String, SimulateCreateMandateModel> corporateMandate;
    private static Map.Entry<String, SimulateCreateMandateModel> consumerMandate;

    @BeforeAll
    public static void Setup() {

        corporateSetup();
        consumerSetup();

        corporateManagedAccount = createManagedAccount(corporateModulrManagedAccountProfileId, corporateCurrency, corporateAuthenticationToken);
        consumerManagedAccount = createManagedAccount(consumerModulrManagedAccountProfileId, consumerCurrency, consumerAuthenticationToken);

        corporateMandate = createMandate(corporateManagedAccount, corporateAuthenticationToken);
        consumerMandate = createMandate(consumerManagedAccount, consumerAuthenticationToken);
    }

    @Test
    public void GetMandate_Corporate_Success(){

        DirectDebitsService.getDirectDebitMandate(secretKey, corporateMandate.getKey(), corporateAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(corporateMandate.getKey()))
                .body("profileId", equalTo(corporateOddProfileId))
                .body("instrumentId.id", equalTo(corporateManagedAccount.getLeft()))
                .body("instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("ownerId.id", equalTo(corporateId))
                .body("ownerId.type", equalTo(OwnerType.CORPORATE.name()))
                .body("merchantName", equalTo(corporateMandate.getValue().getMerchantName()))
                .body("merchantNumber", equalTo(corporateMandate.getValue().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(corporateMandate.getValue().getMerchantReference()))
                .body("type", equalTo("ELECTRONIC"))
                .body("state", equalTo(MandateState.ACTIVE.name()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetMandate_Consumer_Success(){

        DirectDebitsService.getDirectDebitMandate(secretKey, consumerMandate.getKey(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(consumerMandate.getKey()))
                .body("profileId", equalTo(consumerOddProfileId))
                .body("instrumentId.id", equalTo(consumerManagedAccount.getLeft()))
                .body("instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("ownerId.id", equalTo(consumerId))
                .body("ownerId.type", equalTo(OwnerType.CONSUMER.name()))
                .body("merchantName", equalTo(consumerMandate.getValue().getMerchantName()))
                .body("merchantNumber", equalTo(consumerMandate.getValue().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(consumerMandate.getValue().getMerchantReference()))
                .body("type", equalTo("ELECTRONIC"))
                .body("state", equalTo(MandateState.ACTIVE.name()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetMandate_CancelledExternally_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<String, SimulateCreateMandateModel> mandate = createMandate(consumerManagedAccount, consumer.getRight());

        DirectDebitsHelper.cancelMandateExternally(mandate.getKey(), mandate.getValue().getDdiId(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandate(secretKey, mandate.getKey(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getKey()))
                .body("profileId", equalTo(consumerOddProfileId))
                .body("instrumentId.id", equalTo(consumerManagedAccount.getLeft()))
                .body("instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("ownerId.id", equalTo(consumer.getLeft()))
                .body("ownerId.type", equalTo(OwnerType.CONSUMER.name()))
                .body("merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("type", equalTo("ELECTRONIC"))
                .body("state", equalTo(MandateState.CANCELLED.name()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetMandate_CancelledInternally_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<String, SimulateCreateMandateModel> mandate = createMandate(consumerManagedAccount, consumer.getRight());

        DirectDebitsHelper.cancelMandateInternally(mandate.getKey(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandate(secretKey, mandate.getKey(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getKey()))
                .body("profileId", equalTo(consumerOddProfileId))
                .body("instrumentId.id", equalTo(consumerManagedAccount.getLeft()))
                .body("instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("ownerId.id", equalTo(consumer.getLeft()))
                .body("ownerId.type", equalTo(OwnerType.CONSUMER.name()))
                .body("merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("type", equalTo("ELECTRONIC"))
                .body("state", equalTo(MandateState.CANCELLED.name()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetMandate_Expired_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setBaseCurrency(Currency.GBP.name())
                .build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, KycLevel.KYC_LEVEL_2, secretKey);

        final Pair<String, FasterPaymentsBankDetailsModel> consumerManagedAccount =
                createManagedAccount(consumerModulrManagedAccountProfileId, createConsumerModel.getBaseCurrency(), consumer.getRight());

        final Map.Entry<String, SimulateCreateMandateModel> mandate = createMandate(consumerManagedAccount, consumer.getRight());

        DirectDebitsHelper.expireMandate(mandate.getKey(), mandate.getValue().getDdiId(), secretKey, consumer.getRight());

        DirectDebitsService.getDirectDebitMandate(secretKey, mandate.getKey(), consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(mandate.getKey()))
                .body("profileId", equalTo(consumerOddProfileId))
                .body("instrumentId.id", equalTo(consumerManagedAccount.getLeft()))
                .body("instrumentId.type", equalTo(ManagedInstrumentType.MANAGED_ACCOUNTS.getValue()))
                .body("ownerId.id", equalTo(consumer.getLeft()))
                .body("ownerId.type", equalTo(OwnerType.CONSUMER.name()))
                .body("merchantName", equalTo(mandate.getValue().getMerchantName()))
                .body("merchantNumber", equalTo(mandate.getValue().getMerchantAccountNumber()))
                .body("merchantReference", equalTo(mandate.getValue().getMerchantReference()))
                .body("type", equalTo("ELECTRONIC"))
                .body("state", equalTo(MandateState.EXPIRED.name()))
                .body("creationTimestamp", notNullValue());
    }

    @Test
    public void GetMandate_UnknownMandate_NotFound(){

        DirectDebitsService.getDirectDebitMandate(secretKey, RandomStringUtils.randomNumeric(18), corporateAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetMandate_InvalidMandate_BadRequest(){

        DirectDebitsService.getDirectDebitMandate(secretKey, RandomStringUtils.randomAlphanumeric(6), consumerAuthenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetMandate_InvalidApiKey_Unauthorised(){
        DirectDebitsService.getDirectDebitMandate("abc", consumerMandate.getKey(), consumerAuthenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetMandate_NoApiKey_BadRequest(){
        DirectDebitsService.getDirectDebitMandate("", consumerMandate.getKey(), consumerAuthenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void GetMandate_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        DirectDebitsService.getDirectDebitMandate(secretKey, corporateMandate.getKey(), corporateAuthenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetMandate_RootUserLoggedOut_Unauthorised(){
        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        DirectDebitsService.getDirectDebitMandate(secretKey, corporateMandate.getKey(), token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetMandate_CrossIdentityChecks_NotFound(){

        DirectDebitsService.getDirectDebitMandate(secretKey, corporateMandate.getKey(), consumerAuthenticationToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    private static void consumerSetup() {
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                        .setBaseCurrency(Currency.GBP.name()).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();
        consumerCurrency = createConsumerModel.getBaseCurrency();
    }

    private static void corporateSetup() {
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId)
                        .setBaseCurrency(Currency.GBP.name())
                        .setCompany(CompanyModel.defaultCompanyModel()
                                .setType(CompanyType.getRandomWithExcludedCompanyType(CompanyType.NON_PROFIT_ORGANISATION).name())
                                .build())
                        .build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();
        corporateCurrency = createCorporateModel.getBaseCurrency();
    }
}
